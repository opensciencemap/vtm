/*
 * Copyright 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.layers;

import org.oscim.backend.Log;
import org.oscim.backend.input.MotionEvent;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.map.Viewport;

/**
 * Changes Viewport for scroll, fling, scale, rotation and tilt gestures
 *
 * @TODO:
 *        - better recognition of tilt/rotate/scale state
 *        one could check change of rotation / scale within a
 *        given time to estimate if the mode should be changed:
 *        http://en.wikipedia.org/wiki/Viterbi_algorithm
 */

public class MapEventLayer extends InputLayer {
	private static final boolean debug = false;
	private static final String TAG = MapEventLayer.class.getName();

	private float mSumScale;
	private float mSumRotate;

	private boolean mBeginScale;
	private boolean mBeginRotate;
	private boolean mBeginTilt;
	private boolean mDoubleTap;
	private boolean mWasMulti;

	private float mPrevX;
	private float mPrevY;

	private float mPrevX2;
	private float mPrevY2;

	private double mAngle;
	private double mPrevPinchWidth;

	private float mFocusX;
	private float mFocusY;

	protected static final int JUMP_THRESHOLD = 100;
	protected static final double PINCH_ZOOM_THRESHOLD = 5;
	protected static final double PINCH_ROTATE_THRESHOLD = 0.02;
	protected static final float PINCH_TILT_THRESHOLD = 1f;

	private final Viewport mMapPosition;
	private final VelocityTracker mTracker;

	public MapEventLayer(Map map) {
		super(map);
		mMapPosition = map.getViewport();
		mTracker = new VelocityTracker();
	}

	//private long mPrevTime;

	private boolean mEnableRotation = true;
	private boolean mEnableTilt = true;
	private boolean mEnableMove = true;
	private boolean mEnableZoom = true;

	public void enableRotation(boolean enable) {
		mEnableRotation = enable;
	}

	public boolean rotationEnabled() {
		return mEnableRotation;
	}

	public void enableTilt(boolean enable) {
		mEnableTilt = enable;
	}

	public void enableMove(boolean enable) {
		mEnableMove = enable;
	}

	public void enableZoom(boolean enable) {
		mEnableZoom = enable;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {

		//mPrevTime = e.getTime();

		int action = getAction(e);

		if (action == MotionEvent.ACTION_DOWN) {
			mBeginRotate = false;
			mBeginTilt = false;
			mBeginScale = false;
			mDoubleTap = false;
			mWasMulti = false;

			mPrevX = e.getX(0);
			mPrevY = e.getY(0);

			mTracker.start(mPrevX, mPrevY, e.getTime());
			return true;
		} else if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(e);
		} else if (action == MotionEvent.ACTION_UP) {
			onFling(mTracker.getVelocityX(), mTracker.getVelocityY());
			return true;
		} else if (action == MotionEvent.ACTION_CANCEL) {
			mDoubleTap = false;
			return true;
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			mWasMulti = true;
			updateMulti(e);
			return true;
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			updateMulti(e);
			return true;
		}

		return false;
	}

	private static int getAction(MotionEvent e) {
		return e.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionMove(MotionEvent e) {
		float x1 = e.getX(0);
		float y1 = e.getY(0);

		float mx = x1 - mPrevX;
		float my = y1 - mPrevY;

		float width = mMap.getWidth();
		float height = mMap.getHeight();

		mTracker.update(x1, y1, e.getTime());

		// return if detect a new gesture, as indicated by a large jump
		if (Math.abs(mx) > JUMP_THRESHOLD || Math.abs(my) > JUMP_THRESHOLD)
			return true;

		// double-tap + hold
		if (mDoubleTap) {
			if (debug)
				Log.d(TAG, "tap scale: " + mx + " " + my);
			mMapPosition.scaleMap(1 - my / (height / 8), 0, 0);
			mMap.updateMap(true);

			mPrevX = x1;
			mPrevY = y1;
			return true;
		}

		if (e.getPointerCount() < 2) {
			if (!mEnableMove)
				return true;

			if (mx > 1 || mx < -1 || my > 1 || my < -1) {
				mMapPosition.moveMap(mx, my);
				mMap.updateMap(true);

				mPrevX = x1;
				mPrevY = y1;
			}
			return true;
		}

		float x2 = e.getX(1);
		float y2 = e.getY(1);

		float dx = (x1 - x2);
		float dy = (y1 - y2);
		float slope = 0;

		if (dx != 0)
			slope = dy / dx;

		double pinchWidth = Math.sqrt(dx * dx + dy * dy);

		final double deltaPinchWidth = pinchWidth - mPrevPinchWidth;

		double rad = Math.atan2(dy, dx);
		double r = rad - mAngle;

		boolean startScale = (Math.abs(deltaPinchWidth) > PINCH_ZOOM_THRESHOLD);

		boolean changed = false;

		if (mEnableZoom && !mBeginTilt && (mBeginScale || startScale)) {
			mBeginScale = true;

			float scale = (float) (pinchWidth / mPrevPinchWidth);

			// decrease change of scale by the change of rotation
			// * 20 is just arbitrary
			if (mBeginRotate)
				scale = 1 + ((scale - 1) * Math.max((1 - (float) Math.abs(r) * 20), 0));

			mSumScale *= scale;

			if ((mSumScale < 0.99 || mSumScale > 1.01) && mSumRotate < Math.abs(0.02))
				mBeginRotate = false;

			float fx = (x2 + x1) / 2 - width / 2;
			float fy = (y2 + y1) / 2 - height / 2;

			//Log.d(TAG, "zoom " + deltaPinchWidth + " " + scale + " " + mSumScale);
			changed = mMapPosition.scaleMap(scale, fx, fy);
		}

		if (mEnableTilt && !mBeginRotate && Math.abs(slope) < 1) {
			float my2 = y2 - mPrevY2;
			float threshold = PINCH_TILT_THRESHOLD;
			//Log.d(TAG, r + " " + slope + " m1:" + my + " m2:" + my2);

			if ((my > threshold && my2 > threshold)
					|| (my < -threshold && my2 < -threshold))
			{
				mBeginTilt = true;
				changed = mMapPosition.tiltMap(my / 5);
			}
		} else if (mEnableRotation && !mBeginTilt &&
				(mBeginRotate || Math.abs(r) > PINCH_ROTATE_THRESHOLD)) {
			//Log.d(TAG, "rotate: " + mBeginRotate + " " + Math.toDegrees(rad));
			if (!mBeginRotate) {
				mAngle = rad;

				mSumScale = 1;
				mSumRotate = 0;

				mBeginRotate = true;

				mFocusX = (width / 2) - (x1 + x2) / 2;
				mFocusY = (height / 2) - (y1 + y2) / 2;
			} else {
				double da = rad - mAngle;
				mSumRotate += da;

				if (Math.abs(da) > 0.001) {
					mMapPosition.rotateMap(da, mFocusX, mFocusY);
					changed = true;
				}
			}
			mAngle = rad;
		}

		if (changed) {
			mMap.updateMap(true);
			mPrevPinchWidth = pinchWidth;

			mPrevX2 = x2;
			mPrevY2 = y2;
		}

		mPrevX = x1;
		mPrevY = y1;

		return true;
	}

	private void updateMulti(MotionEvent e) {
		int cnt = e.getPointerCount();

		if (cnt == 2) {
			mPrevX = e.getX(0);
			mPrevY = e.getY(0);

			mPrevX2 = e.getX(1);
			mPrevY2 = e.getY(1);
			double dx = mPrevX - mPrevX2;
			double dy = mPrevY - mPrevY2;

			mAngle = Math.atan2(dy, dx);
			mPrevPinchWidth = Math.sqrt(dx * dx + dy * dy);
			mSumScale = 1;
		}
	}

	private boolean onFling(float velocityX, float velocityY) {
		if (mWasMulti)
			return true;

		int w = Tile.SIZE * 3;
		int h = Tile.SIZE * 3;

		mMap.getAnimator().animateFling(
				Math.round(velocityX),
				Math.round(velocityY),
				-w, w, -h, h);
		return true;
	}

	//@Override
	//public boolean onDoubleTap(MotionEvent e) {
	//
	//	mDoubleTap = true;
	//	//mMapPosition.animateZoom(2);
	//
	//	if (debug)
	//		printState("onDoubleTap");
	//
	//	// avoid onLongPress
	//	mMap.getLayers().cancelGesture();
	//
	//	return true;
	//}
	//
	//@Override
	//public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
	//		final float distanceY) {
	//
	//	if (e2.getPointerCount() == 1) {
	//		mMapPosition.moveMap(-distanceX, -distanceY);
	//		mMap.updateMap(true);
	//		return true;
	//	}
	//
	//	return false;
	//}
	//

	//
	//private void printState(String action) {
	//		Log.d(TAG, action
	//				+ " " + mDoubleTap
	//				+ " " + mBeginScale
	//				+ " " + mBeginRotate
	//				+ " " + mBeginTilt);
	//}

	/*******************************************************************************
	 * Copyright 2011 See libgdx AUTHORS file.
	 * Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License.
	 * You may obtain a copy of the License at
	 * http://www.apache.org/licenses/LICENSE-2.0
	 * Unless required by applicable law or agreed to in writing, software
	 * distributed under the License is distributed on an "AS IS" BASIS,
	 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	 * See the License for the specific language governing permissions and
	 * limitations under the License.
	 ******************************************************************************/
	class VelocityTracker {
		int sampleSize = 10;
		float lastX, lastY;
		float deltaX, deltaY;
		long lastTime;
		int numSamples;
		float[] meanX = new float[sampleSize];
		float[] meanY = new float[sampleSize];
		long[] meanTime = new long[sampleSize];

		public void start(float x, float y, long timeStamp) {
			lastX = x;
			lastY = y;
			deltaX = 0;
			deltaY = 0;
			numSamples = 0;
			for (int i = 0; i < sampleSize; i++) {
				meanX[i] = 0;
				meanY[i] = 0;
				meanTime[i] = 0;
			}
			lastTime = timeStamp;
		}

		public void update(float x, float y, long timeStamp) {
			long currTime = timeStamp;
			deltaX = x - lastX;
			deltaY = y - lastY;
			lastX = x;
			lastY = y;
			long deltaTime = currTime - lastTime;
			lastTime = currTime;
			int index = numSamples % sampleSize;
			meanX[index] = deltaX;
			meanY[index] = deltaY;
			meanTime[index] = deltaTime;
			numSamples++;
		}

		public float getVelocityX() {
			float meanX = getAverage(this.meanX, numSamples);
			float meanTime = getAverage(this.meanTime, numSamples) / 1000.0f;
			if (meanTime == 0)
				return 0;
			return meanX / meanTime;
		}

		public float getVelocityY() {
			float meanY = getAverage(this.meanY, numSamples);
			float meanTime = getAverage(this.meanTime, numSamples) / 1000.0f;
			if (meanTime == 0)
				return 0;
			return meanY / meanTime;
		}

		private float getAverage(float[] values, int numSamples) {
			numSamples = Math.min(sampleSize, numSamples);
			float sum = 0;
			for (int i = 0; i < numSamples; i++) {
				sum += values[i];
			}
			return sum / numSamples;
		}

		private long getAverage(long[] values, int numSamples) {
			numSamples = Math.min(sampleSize, numSamples);
			long sum = 0;
			for (int i = 0; i < numSamples; i++) {
				sum += values[i];
			}
			if (numSamples == 0)
				return 0;
			return sum / numSamples;
		}

		//private float getSum (float[] values, int numSamples) {
		//	numSamples = Math.min(sampleSize, numSamples);
		//	float sum = 0;
		//	for (int i = 0; i < numSamples; i++) {
		//		sum += values[i];
		//	}
		//	if (numSamples == 0) return 0;
		//	return sum;
		//}
	}

}
