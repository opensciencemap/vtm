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
package org.oscim.view;

import org.oscim.core.Tile;
import org.oscim.overlay.Overlay;

import android.util.Log;
import android.view.MotionEvent;

/**
 * Changes MapViewPosition for scroll, fling, scale, rotation and tilt gestures
 *
 * @TODO:
 *        - better recognition of tilt/rotate/scale state
 *        one could check change of rotation / scale within a
 *        given time to estimate if the mode should be changed:
 *        http://en.wikipedia.org/wiki/Viterbi_algorithm
 */

public class MapEventLayer extends Overlay {
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

	private final MapViewPosition mMapPosition;

	public MapEventLayer(MapView mapView) {
		super(mapView);
		mMapPosition = mapView.getMapViewPosition();
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {

		int action = getAction(e);

		if (action == MotionEvent.ACTION_DOWN) {
			mBeginRotate = false;
			mBeginTilt = false;
			mBeginScale = false;
			mDoubleTap = false;
			mWasMulti = false;

			mPrevX = e.getX(0);
			mPrevY = e.getY(0);
			return true; //onActionDown(e);
		} else if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(e);
		} else if (action == MotionEvent.ACTION_UP) {
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

		float width = mMapView.getWidth();
		float height = mMapView.getHeight();

		// return if detect a new gesture, as indicated by a large jump
		if (Math.abs(mx) > JUMP_THRESHOLD || Math.abs(my) > JUMP_THRESHOLD)
			return true;

		// double-tap + hold
		if (mDoubleTap) {
			if (debug)
				Log.d(TAG, "tap scale: " + mx + " " + my);
			mMapPosition.scaleMap(1 - my / (height / 8), 0, 0);
			mMapView.redrawMap(true);

			mPrevX = x1;
			mPrevY = y1;
			return true;
		}

		if (e.getPointerCount() < 2)
			return true;

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

		if (!mBeginTilt && (mBeginScale || startScale)) {
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

		if (!mBeginRotate && Math.abs(slope) < 1) {
			float my2 = y2 - mPrevY2;
			float threshold = PINCH_TILT_THRESHOLD;
			//Log.d(TAG, r + " " + slope + " m1:" + my + " m2:" + my2);

			if ((my > threshold && my2 > threshold)
					|| (my < -threshold && my2 < -threshold))
			{
				mBeginTilt = true;
				changed = mMapPosition.tiltMap(my / 5);
			}
		} else if (!mBeginTilt && (mBeginRotate || Math.abs(r) > PINCH_ROTATE_THRESHOLD)) {
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
			mMapView.redrawMap(true);
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

	@Override
	public boolean onDoubleTap(MotionEvent e) {

		mDoubleTap = true;
		//mMapPosition.animateZoom(2);

		if (debug)
			printState("onDoubleTap");

		// avoid onLongPress
		mMapView.getOverlayManager().cancelGesture();

		return true;
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
			final float distanceY) {

		if (e2.getPointerCount() == 1) {
			mMapPosition.moveMap(-distanceX, -distanceY);
			mMapView.redrawMap(true);
			return true;
		}

		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (mWasMulti)
			return true;

		int w = Tile.SIZE * 3;
		int h = Tile.SIZE * 3;

		//if (mMapView.enablePagedFling) {
		//	double a = Math.sqrt(velocityX * velocityX + velocityY * velocityY);
		//
		//	float vx = (float) (velocityX / a);
		//	float vy = (float) (velocityY / a);
		//
		//	if (a < 400)
		//		return true;
		//
		//	float move = Math.min(mMapView.getWidth(), mMapView.getHeight()) * 2 / 3;
		//	mMapPosition.animateTo(vx * move, vy * move, 250);
		//} else {
		float s = (200 / MapView.dpi);

		mMapPosition.animateFling(
				Math.round(velocityX * s),
				Math.round(velocityY * s),
				-w, w, -h, h);
		return true;
	}

	private void printState(String action) {
		Log.d(TAG, action
				+ " " + mDoubleTap
				+ " " + mBeginScale
				+ " " + mBeginRotate
				+ " " + mBeginTilt);
	}

}
