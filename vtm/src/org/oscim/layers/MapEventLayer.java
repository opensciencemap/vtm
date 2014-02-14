/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

import org.oscim.backend.CanvasAdapter;
import org.oscim.core.Tile;
import org.oscim.event.Gesture;
import org.oscim.event.GestureListener;
import org.oscim.event.MotionEvent;
import org.oscim.map.Map;
import org.oscim.map.Viewport;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Changes Viewport by handling move, fling, scale, rotation and tilt gestures.
 */
public class MapEventLayer extends Layer implements Map.InputListener, GestureListener {

	static final Logger log = LoggerFactory.getLogger(MapEventLayer.class);

	/* TODO replace with bitmasks */
	private boolean mEnableRotate = true;
	private boolean mEnableTilt = true;
	private boolean mEnableMove = true;
	private boolean mEnableScale = true;

	/* possible state transitions */
	private boolean mCanScale;
	private boolean mCanRotate;
	private boolean mCanTilt;

	/* current gesture state */
	private boolean mDoRotate;
	private boolean mDoScale;
	private boolean mDoTilt;

	private boolean mDown;
	private boolean mDoubleTap;

	private float mPrevX1;
	private float mPrevY1;
	private float mPrevX2;
	private float mPrevY2;

	private double mAngle;
	private double mPrevPinchWidth;
	private long mStartMove;

	protected static final double PINCH_ZOOM_THRESHOLD = 2;
	protected static final double PINCH_TILT_THRESHOLD = 2;
	protected static final double PINCH_TILT_SLOPE = 0.75;
	protected static final double PINCH_ROTATE_THRESHOLD = 0.2;
	protected static final double PINCH_ROTATE_THRESHOLD2 = 0.5;

	/** 2mm as minimal distance to start move: dpi / 25.4 */
	protected static final float MIN_SLOP = 25.4f / 2;

	/** 100 ms since start of move to reduce fling scroll */
	protected static final float FLING_THREHSHOLD = 100;

	private final Viewport mViewport;
	private final VelocityTracker mTracker;

	public MapEventLayer(Map map) {
		super(map);
		mViewport = map.viewport();
		mTracker = new VelocityTracker();
	}

	@Override
	public void onMotionEvent(MotionEvent event) {
		onTouchEvent(event);
	}

	public void enableRotation(boolean enable) {
		mEnableRotate = enable;
	}

	public boolean rotationEnabled() {
		return mEnableRotate;
	}

	public void enableTilt(boolean enable) {
		mEnableTilt = enable;
	}

	public void enableMove(boolean enable) {
		mEnableMove = enable;
	}

	public void enableZoom(boolean enable) {
		mEnableScale = enable;
	}

	public boolean onTouchEvent(MotionEvent e) {

		int action = getAction(e);

		if (action == MotionEvent.ACTION_DOWN) {
			mMap.animator().cancel();

			mDoubleTap = false;
			mStartMove = -1;
			mDown = true;

			mPrevX1 = e.getX(0);
			mPrevY1 = e.getY(0);

			return true;
		}
		if (!(mDown || mDoubleTap)) {
			/* no down event received */
			return false;
		}

		if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(e);
		}
		if (action == MotionEvent.ACTION_UP) {
			mDown = false;
			if (mStartMove < 0)
				return true;

			float vx = mTracker.getVelocityX();
			float vy = mTracker.getVelocityY();

			/* reduce velocity for short moves */
			float tx = e.getTime() - mStartMove;
			if (tx < FLING_THREHSHOLD) {
				vx *= tx / FLING_THREHSHOLD;
				vy *= tx / FLING_THREHSHOLD;
			}
			doFling(vx, vy);
			return true;
		}
		if (action == MotionEvent.ACTION_CANCEL) {
			//mStartMove = -1;
			mDown = false;
			mDoubleTap = false;
			return true;
		}
		if (action == MotionEvent.ACTION_POINTER_DOWN) {
			mStartMove = -1;
			updateMulti(e);
			return true;
		}
		if (action == MotionEvent.ACTION_POINTER_UP) {
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

		float mx = x1 - mPrevX1;
		float my = y1 - mPrevY1;

		float width = mMap.getWidth();
		float height = mMap.getHeight();

		if (e.getPointerCount() < 2) {
			mPrevX1 = x1;
			mPrevY1 = y1;

			/* double-tap + hold */
			if (mDoubleTap) {
				if (!mDown) {
					// TODO check if within tap range
					mDown = true;
					return true;
				}
				// FIXME limit scale properly
				mViewport.scaleMap(1 - my / (height / 6), 0, 0);
				mMap.updateMap(true);
				mStartMove = -1;
				return true;
			}

			if (!mEnableMove)
				return true;

			if (mStartMove < 0) {
				float minSlop = (CanvasAdapter.dpi / MIN_SLOP);
				if (FastMath.withinSquaredDist(mx, my, minSlop * minSlop)) {
					mPrevX1 -= mx;
					mPrevY1 -= my;
					return true;
				}

				mStartMove = e.getTime();
				mTracker.start(x1, y1, mStartMove);
				return true;
			}
			mViewport.moveMap(mx, my);
			mTracker.update(x1, y1, e.getTime());
			mMap.updateMap(true);
			return true;
		}
		mStartMove = -1;

		float x2 = e.getX(1);
		float y2 = e.getY(1);
		float dx = (x1 - x2);
		float dy = (y1 - y2);

		double rotateBy = 0;
		float scaleBy = 1;
		float tiltBy = 0;

		mx = ((x1 + x2) - (mPrevX1 + mPrevX2)) / 2;
		my = ((y1 + y2) - (mPrevY1 + mPrevY2)) / 2;

		if (mCanTilt) {
			float slope = (dx == 0) ? 0 : dy / dx;

			if (Math.abs(slope) < PINCH_TILT_SLOPE) {

				if (mDoTilt) {
					tiltBy = my / 5;
				} else if (Math.abs(my) > (CanvasAdapter.dpi /
				        MIN_SLOP * PINCH_TILT_THRESHOLD)) {
					/* enter exclusive tilt mode */
					mCanScale = false;
					mCanRotate = false;
					mDoTilt = true;
				}
			}
		}

		double pinchWidth = Math.sqrt(dx * dx + dy * dy);
		double deltaPinch = pinchWidth - mPrevPinchWidth;

		if (mCanRotate) {
			double rad = Math.atan2(dy, dx);
			double r = rad - mAngle;

			if (mDoRotate) {
				double da = rad - mAngle;

				if (Math.abs(da) > 0.0001) {
					rotateBy = da;
					mAngle = rad;

					deltaPinch = 0;
				}
			} else {
				r = Math.abs(r);
				if (r > PINCH_ROTATE_THRESHOLD) {
					/* start rotate, disable tilt */
					mDoRotate = true;
					mCanTilt = false;

					mAngle = rad;
				} else if (!mDoScale) {
					/* reduce pinch trigger by the amount of rotation */
					deltaPinch *= 1 - (r / PINCH_ROTATE_THRESHOLD);
				} else {
					mPrevPinchWidth = pinchWidth;
				}
			}
		} else if (mDoScale && mEnableRotate) {
			/* re-enable rotation when higher threshold is reached */
			double rad = Math.atan2(dy, dx);
			double r = rad - mAngle;

			if (r > PINCH_ROTATE_THRESHOLD2) {
				/* start rotate again */
				mDoRotate = true;
				mCanRotate = true;
				mAngle = rad;
			}
		}

		if (mCanScale || mDoRotate) {

			if (!(mDoScale || mDoRotate)) {
				/* enter exclusive scale mode */
				if (Math.abs(deltaPinch) > (CanvasAdapter.dpi
				        / MIN_SLOP * PINCH_ZOOM_THRESHOLD)) {

					if (!mDoRotate) {
						mPrevPinchWidth = pinchWidth;
						mCanRotate = false;
					}

					mCanTilt = false;
					mDoScale = true;
				}
			}
			if (mDoScale || mDoRotate) {
				scaleBy = (float) (pinchWidth / mPrevPinchWidth);
				mPrevPinchWidth = pinchWidth;
			}
		}

		if (!(mDoRotate || mDoScale || mDoTilt))
			return true;

		float fx = (x2 + x1) / 2 - width / 2;
		float fy = (y2 + y1) / 2 - height / 2;

		synchronized (mViewport) {

			if (!mDoTilt) {

				if (rotateBy != 0)
					mViewport.rotateMap(rotateBy, fx, fy);
				if (scaleBy != 1)
					mViewport.scaleMap(scaleBy, fx, fy);

				mViewport.moveMap(mx, my);
			} else {
				if (tiltBy != 0) {
					mViewport.moveMap(0, my / 2);
					mViewport.tiltMap(tiltBy);
				}
			}
		}

		mPrevX1 = x1;
		mPrevY1 = y1;
		mPrevX2 = x2;
		mPrevY2 = y2;

		mMap.updateMap(true);

		return true;
	}

	private void updateMulti(MotionEvent e) {
		int cnt = e.getPointerCount();

		mPrevX1 = e.getX(0);
		mPrevY1 = e.getY(0);

		if (cnt == 2) {
			mDoScale = false;
			mDoRotate = false;
			mDoTilt = false;
			mCanScale = mEnableScale;
			mCanRotate = mEnableRotate;
			mCanTilt = mEnableTilt;

			mPrevX2 = e.getX(1);
			mPrevY2 = e.getY(1);
			double dx = mPrevX1 - mPrevX2;
			double dy = mPrevY1 - mPrevY2;

			mAngle = Math.atan2(dy, dx);
			mPrevPinchWidth = Math.sqrt(dx * dx + dy * dy);
		}
	}

	private boolean doFling(float velocityX, float velocityY) {

		int w = Tile.SIZE * 3;
		int h = Tile.SIZE * 3;

		mMap.animator().animateFling(Math.round(velocityX),
		                             Math.round(velocityY),
		                             -w, w, -h, h);
		return true;
	}

	@Override
	public boolean onGesture(Gesture g, MotionEvent e) {
		if (g == Gesture.DOUBLE_TAP) {
			mDoubleTap = true;
			return true;
		}
		return false;
	}

	/*******************************************************************************
	 * from libgdx:
	 * Copyright 2011 Mario Zechner <badlogicgames@gmail.com>
	 * Copyright 2011 Nathan Sweet <nathan.sweet@gmail.com>
	 * 
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
		int sampleSize = 16;
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
