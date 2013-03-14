/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012, 2013 Hannes Janetzek
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
import org.oscim.overlay.OverlayManager;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

/**
 * @author Hannes Janetzek
 * @TODO:
 *        - use one AnimationTimer instead of CountDownTimers
 *        - fix recognition of tilt/rotate/scale state...
 */

final class TouchHandler implements OnGestureListener, OnDoubleTapListener {

	private static final String TAG = TouchHandler.class.getName();

	private static final boolean debug = false;

	private final MapView mMapView;
	private final MapViewPosition mMapPosition;
	private final OverlayManager mOverlayManager;

	private float mSumScale;
	private float mSumRotate;

	private boolean mBeginScale;
	private boolean mBeginRotate;
	private boolean mBeginTilt;
	private boolean mDoubleTap;

	private float mPrevX;
	private float mPrevY;

	private float mPrevX2;
	private float mPrevY2;

	private double mAngle;
	private double mPrevPinchWidth;

	private float mFocusX;
	private float mFocusY;

	private final GestureDetector mGestureDetector;

	protected static final int JUMP_THRESHOLD = 100;
	protected static final double PINCH_ZOOM_THRESHOLD = 5;
	protected static final double PINCH_ROTATE_THRESHOLD = 0.02;
	protected static final float PINCH_TILT_THRESHOLD = 1f;

	/**
	 * @param context
	 *            the Context
	 * @param mapView
	 *            the MapView
	 */
	public TouchHandler(Context context, MapView mapView) {
		mMapView = mapView;
		mMapPosition = mapView.getMapPosition();
		mOverlayManager = mapView.getOverlayManager();
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setOnDoubleTapListener(this);
	}

	/**
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean handleMotionEvent(MotionEvent e) {

		if (mOverlayManager.onTouchEvent(e))
			return true;

		mGestureDetector.onTouchEvent(e);

		int action = getAction(e);

		if (action == MotionEvent.ACTION_DOWN) {
			mMulti = 0;
			mWasMulti = false;
			if (mOverlayManager.onDown(e))
				return true;

			return onActionDown(e);
		} else if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(e);
		} else if (action == MotionEvent.ACTION_UP) {
			return onActionUp(e);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			return onActionCancel();
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			return onActionPointerDown(e);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			return onActionPointerUp(e);
		}

		return false;
	}

	private static int getAction(MotionEvent e) {
		return e.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionCancel() {
		mDoubleTap = false;
		return true;
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

		if (mMulti == 0)
			return true;

		// TODO improve gesture recognition,
		// one could check change of rotation / scale within a
		// given time to estimate if the mode should be changed:
		// http://en.wikipedia.org/wiki/Viterbi_algorithm

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

	private int mMulti;
	private boolean mWasMulti;

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

	private boolean onActionPointerDown(MotionEvent e) {

		mMulti++;
		mWasMulti = true;

		updateMulti(e);

		return true;
	}

	private boolean onActionPointerUp(MotionEvent e) {

		updateMulti(e);
		mMulti--;

		return true;
	}

	private void printState(String action) {
		Log.d(TAG, action
				+ " " + mDoubleTap
				+ " " + mBeginScale
				+ " " + mBeginRotate
				+ " " + mBeginTilt);
	}

	private boolean onActionDown(MotionEvent e) {
		mPrevX = e.getX(0);
		mPrevY = e.getY(0);

		if (debug)
			printState("onActionDown");

		return true;
	}

	/**
	 * @param event
	 *            unused
	 * @return ...
	 */
	private boolean onActionUp(MotionEvent event) {

		if (debug)
			printState("onActionUp");

		mBeginRotate = false;
		mBeginTilt = false;
		mBeginScale = false;
		mDoubleTap = false;

		return true;
	}

	/******************* GestureListener *******************/

	//private final Scroller mScroller;
	//private float mScrollX, mScrollY;
	//	private boolean fling = false;

	@Override
	public void onShowPress(MotionEvent e) {
		mOverlayManager.onShowPress(e);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return mOverlayManager.onSingleTapUp(e);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		if (debug)
			printState("onDown");

		return true;
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
			final float distanceY) {

		if (mOverlayManager.onScroll(e1, e2, distanceX, distanceY)) {
			return true;
		}

		if (mMulti == 0) {
			if (debug)
				printState("onScroll " + distanceX + " " + distanceY);
			mMapPosition.moveMap(-distanceX, -distanceY);
			mMapView.redrawMap(true);
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (mWasMulti)
			return true;

		int w = Tile.TILE_SIZE * 6;
		int h = Tile.TILE_SIZE * 6;

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
		float s = (200 / mMapView.dpi);

		mMapPosition.animateFling(Math.round(velocityX * s), Math.round(velocityY * s), -w, w, -h,
				h);
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (mDoubleTap)
			return;

		if (mOverlayManager.onLongPress(e)) {
			return;
		}

		//	if (MapView.testRegionZoom) {
		//		Log.d("mapsforge", "long press");
		//		mMapView.mRegionLookup.updateRegion(-1, null);
		//	}
	}

	/******************* DoubleTapListener ****************/
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return mOverlayManager.onSingleTapConfirmed(e);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (mOverlayManager.onDoubleTap(e))
			return true;

		mDoubleTap = true;
		//mMapPosition.animateZoom(2);

		if (debug)
			printState("onDoubleTap");

		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}
}
