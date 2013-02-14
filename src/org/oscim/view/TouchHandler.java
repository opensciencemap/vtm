/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012, 2013 OpenScienceMap
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
import android.os.CountDownTimer;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * @author Hannes Janetzek
 * @TODO:
 *        - use one AnimationTimer instead of CountDownTimers
 *        - fix recognition of tilt/rotate/scale state...
 */

final class TouchHandler implements OnGestureListener, OnDoubleTapListener {
	//OnScaleGestureListener,

	private static final String TAG = TouchHandler.class.getName();

	private static final float SCALE_DURATION = 500;
	//private static final float ROTATION_DELAY = 200; // ms

	private static final int INVALID_POINTER_ID = -1;

	private final MapView mMapView;
	private final MapViewPosition mMapPosition;
	private final OverlayManager mOverlayManager;

	private final DecelerateInterpolator mInterpolator;
	//private final DecelerateInterpolator mLinearInterpolator;
	private boolean mBeginScale;
	private float mSumScale;
	private float mSumRotate;

	private boolean mBeginRotate;
	private boolean mBeginTilt;
	private boolean mLongPress;

	private float mPrevX;
	private float mPrevY;

	private float mPrevX2;
	private float mPrevY2;

	private double mAngle;

	private int mPointerId1;
	private int mPointerId2;

	//private final ScaleGestureDetector mScaleGestureDetector;
	private final GestureDetector mGestureDetector;

	//private final float dpi;

	protected static final int JUMP_THRESHOLD = 100;
	protected static final double PINCH_ZOOM_THRESHOLD = 5;
	protected static final double PINCH_ROTATE_THRESHOLD = 0.02;
	protected static final float PINCH_TILT_THRESHOLD = 1f;
	protected int mPrevPointerCount = 0;
	protected double mPrevPinchWidth = -1;

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
		// ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		// mMapMoveDelta = viewConfiguration.getScaledTouchSlop();
		mPointerId1 = INVALID_POINTER_ID;
		mPointerId2 = INVALID_POINTER_ID;
		//mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setOnDoubleTapListener(this);

		mInterpolator = new DecelerateInterpolator(2f);

		mScroller = new Scroller(mMapView.getContext(), mInterpolator);
		//mLinearInterpolator = new DecelerateInterpolator(0.8f);//new android.view.animation.LinearInterpolator();

	}

	/**
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean handleMotionEvent(MotionEvent e) {

		if (mOverlayManager.onTouchEvent(e, mMapView))
			return true;

		mGestureDetector.onTouchEvent(e);
		//boolean scaling = false; //mScaleGestureDetector.onTouchEvent(event);

		int action = getAction(e);

		if (action == MotionEvent.ACTION_DOWN) {
			mMulti = 0;
			mWasMulti = false;
			if (mOverlayManager.onDown(e, mMapView))
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
		mPointerId1 = INVALID_POINTER_ID;
		mLongPress = true;
		return true;
	}


	private boolean onActionMove(MotionEvent e) {
		int id = e.findPointerIndex(mPointerId1);

		float x1 = e.getX(id);
		float y1 = e.getY(id);

		float mx = x1 - mPrevX;
		float my = y1 - mPrevY;

		float width = mMapView.getWidth();
		float height = mMapView.getHeight();

		// double-tap + hold
		if (mLongPress) {
			mMapPosition.scaleMap(1 - my / (height / 5), 0, 0);
			mMapView.redrawMap(true);

			mPrevX = x1;
			mPrevY = y1;
			return true;
		}

		// return if detect a new gesture, as indicated by a large jump
		if (Math.abs(mx) > JUMP_THRESHOLD || Math.abs(my) > JUMP_THRESHOLD)
			return true;

		if (mMulti == 0) {
			// reset pinch variables
			//mPrevPinchWidth = -1;
			return true;
		}

		// TODO improve gesture recognition,
		// one could check change of rotation / scale within a
		// given time to estimate if the mode should be changed:
		// http://en.wikipedia.org/wiki/Viterbi_algorithm

		int id2 = e.findPointerIndex(mPointerId2);

		float x2 = e.getX(id2);
		float y2 = e.getY(id2);

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
				changed = mMapPosition.tilt(my / 5);
			}
		}

		if (!mBeginTilt
				&& (mBeginRotate || (Math.abs(slope) > 1 && Math.abs(r) > PINCH_ROTATE_THRESHOLD))) {
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
					double rsin = Math.sin(r);
					double rcos = Math.cos(r);
					float x = (float) (mFocusX * rcos + mFocusY * -rsin - mFocusX);
					float y = (float) (mFocusX * rsin + mFocusY * rcos - mFocusY);

					mMapPosition.rotateMap((float) Math.toDegrees(da), x, y);
					changed = true;
				}
			}
			mAngle = rad;
		}

		if (changed){
			mMapView.redrawMap(true);
			mPrevPinchWidth = pinchWidth;
		}

		mPrevX = x1;
		mPrevY = y1;
		mPrevX2 = x2;
		mPrevY2 = y2;

		return true;
	}

	private int mMulti = 0;
	private boolean mWasMulti;

	//	private long mMultiTouchDownTime;

	private boolean onActionPointerDown(MotionEvent event) {
		//	mMultiTouchDownTime = event.getEventTime();

		mMulti++;
		mWasMulti = true;
		mSumScale = 1;

		if (mMulti == 1) {

			int masked = (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK);
			int pointerIndex = masked >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
			mPointerId2 = event.getPointerId(pointerIndex);

			mPrevX2 = event.getX(pointerIndex);
			mPrevY2 = event.getY(pointerIndex);
			double dx = mPrevX - mPrevX2;
			double dy = mPrevY - mPrevY2;
			mAngle = Math.atan2(dy, dx);

			mPrevPinchWidth = Math.sqrt(dx * dx + dy * dy);

		}
		// Log.d("...", "mMulti down " + mMulti);
		return true;
	}

	private boolean onActionPointerUp(MotionEvent e) {

		// extract the index of the pointer that left the touch sensor
		int masked = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK);
		int pointerIndex = masked >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		if (e.getPointerId(pointerIndex) == mPointerId1) {
			// the active pointer has gone up, choose a new one
			if (pointerIndex == 0) {
				pointerIndex = 1;
			} else {
				pointerIndex = 0;
			}
			// save the position of the event
			mPrevX = e.getX(pointerIndex);
			mPrevY = e.getY(pointerIndex);

			mPointerId1 = e.getPointerId(pointerIndex);
		}

		mMulti--;

		if (mMulti == 0){
			mPointerId2 = INVALID_POINTER_ID;
		}

		mLongPress = false;

		// Log.d("...", "mMulti up " + mMulti);

		return true;
	}

	private boolean onActionDown(MotionEvent e) {
		mPrevX = e.getX();
		mPrevY = e.getY();

		mBeginRotate = false;
		mBeginTilt = false;
		mBeginScale = false;

		// save the ID of the pointer
		mPointerId1 = e.getPointerId(0);

		return true;
	}
	/**
	 * @param event
	 *            unused
	 * @return ...
	 */
	private boolean onActionUp(MotionEvent event) {
		mPointerId1 = INVALID_POINTER_ID;

		mLongPress = false;
		mMulti = 0;
		mPrevPinchWidth = -1;
		mPrevPointerCount = 0;

		return true;
	}

	/******************* GestureListener *******************/

	private final Scroller mScroller;
	private float mScrollX, mScrollY;
	private boolean fling = false;

	@Override
	public void onShowPress(MotionEvent e) {
		mOverlayManager.onShowPress(e, mMapView);
	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return mOverlayManager.onSingleTapUp(e, mMapView);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		if (fling) {
			mScroller.forceFinished(true);

			if (mTimer != null) {
				mTimer.cancel();
				mTimer = null;
			}
			fling = false;
		}

		return true;
	}

	boolean scroll() {
		if (mScroller.isFinished()) {
			return false;
		}
		mScroller.computeScrollOffset();

		float moveX = mScroller.getCurrX() - mScrollX;
		float moveY = mScroller.getCurrY() - mScrollY;

		if (moveX >= 1 || moveY >= 1 || moveX <= -1 || moveY <= -1) {
			mMapPosition.moveMap(moveX, moveY);
			mMapView.redrawMap(true);
			mScrollX = mScroller.getCurrX();
			mScrollY = mScroller.getCurrY();
		}
		return true;
	}

	@Override
	public boolean onScroll(final MotionEvent e1, final MotionEvent e2, final float distanceX,
			final float distanceY) {

		if (mOverlayManager.onScroll(e1, e2, distanceX, distanceY, mMapView)) {
			return true;
		}

		//		if (mScaling)
		//			return true;

		if (mMulti == 0) {
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

		//		if (mScaling || mWasMulti)
		//			return true;

		int w = Tile.TILE_SIZE * 6;
		int h = Tile.TILE_SIZE * 6;
		mScrollX = 0;
		mScrollY = 0;

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		if (mMapView.enablePagedFling) {

			double a = Math.sqrt(velocityX * velocityX + velocityY * velocityY);

			float vx = (float) (velocityX / a);
			float vy = (float) (velocityY / a);

			Log.d(TAG, "velocity: " + a + " " + velocityX + " " + velocityY + " - " + vx + " " + vy);

			if (a < 400)
				return true;

			float move = Math.min(mMapView.getWidth(), mMapView.getHeight()) * 2 / 3;
			mMapPosition.animateTo(vx * move, vy * move, 250);
		} else {
			float s = (300 / mMapView.dpi) / 2;
			mScroller.fling(0, 0, Math.round(velocityX * s),
					Math.round(velocityY * s),
					-w, w, -h, h);

			mTimer = new CountDownTimer(1000, 16) {
				@Override
				public void onTick(long tick) {
					scroll();
				}

				@Override
				public void onFinish() {
				}
			}.start();
			fling = true;
		}
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (mLongPress)
			return;

		if (mOverlayManager.onLongPress(e, mMapView)) {
			return;
		}

		//	if (MapView.testRegionZoom) {
		//		Log.d("mapsforge", "long press");
		//		mMapView.mRegionLookup.updateRegion(-1, null);
		//	}
	}

	boolean scale2(long tick) {

		fling = true;
		if (mPrevScale >= 1)
			return false;

		float adv = (SCALE_DURATION - tick) / SCALE_DURATION;
		adv = mInterpolator.getInterpolation(adv);
		float scale = adv - mPrevScale;
		mPrevScale += scale;
		scale *= 0.75;
		scale += 1;
		adv += 1;

		if (scale > 1) {
			mMapPosition.scaleMap(scale, mScrollX / adv, mScrollY / adv);
			mMapView.redrawMap(true);
		}

		return true;
	}

	/******************* DoubleTapListener ****************/
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return mOverlayManager.onSingleTapConfirmed(e, mMapView);
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (mOverlayManager.onDoubleTap(e, mMapView))
			return true;

		mLongPress = true;

		return true;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	//	/******************* ScaleListener *******************/
	private float mPrevScale;
	private CountDownTimer mTimer;
	boolean mZooutOut;
	//	private float mCenterX;
	//	private float mCenterY;
	private float mFocusX;
	private float mFocusY;
	//	private long mTimeStart;
	//	private long mTimeEnd;
	//
	//	@Override
	//	public boolean onScale(ScaleGestureDetector gd) {
	//
	//		if (mBeginTilt)
	//			return true;
	//
	//		float scale = gd.getScaleFactor();
	//		mFocusX = gd.getFocusX() - mCenterX;
	//		mFocusY = gd.getFocusY() - mCenterY;
	//
	//		mSumScale *= scale;
	//
	//		mTimeEnd = SystemClock.elapsedRealtime();
	//
	//		if (!mBeginScale) {
	//			if (mSumScale > 1.1 || mSumScale < 0.9) {
	//				// Log.d("...", "begin scale " + mSumScale);
	//				mBeginScale = true;
	//				// scale = mSumScale;
	//			}
	//		}
	//
	//		if (mBeginScale && mMapPosition.scaleMap(scale, mFocusX, mFocusY))
	//			mMapView.redrawMap(true);
	//
	//		return true;
	//	}
	//
	//	@Override
	//	public boolean onScaleBegin(ScaleGestureDetector gd) {
	//		mScaling = true;
	//		mBeginScale = false;
	//
	//		mTimeEnd = mTimeStart = SystemClock.elapsedRealtime();
	//		mSumScale = 1;
	//		mCenterX = mMapView.getWidth() >> 1;
	//		mCenterY = mMapView.getHeight() >> 1;
	//
	//		if (mTimer != null) {
	//			mTimer.cancel();
	//			mTimer = null;
	//		}
	//		return true;
	//	}
	//
	//	@Override
	//	public void onScaleEnd(ScaleGestureDetector gd) {
	//		// Log.d("ScaleListener", "Sum " + mSumScale + " " + (mTimeEnd -
	//		// mTimeStart));
	//
	//		if (mTimer == null && mTimeEnd - mTimeStart < 150
	//				&& (mSumScale < 0.99 || mSumScale > 1.01)) {
	//
	//			mPrevScale = 0;
	//
	//			mZooutOut = mSumScale < 0.99;
	//
	//			mTimer = new CountDownTimer((int) SCALE_DURATION, 32) {
	//				@Override
	//				public void onTick(long tick) {
	//					scaleAnim(tick);
	//				}
	//
	//				@Override
	//				public void onFinish() {
	//					scaleAnim(0);
	//				}
	//			}.start();
	//		} else {
	//			mScaling = false;
	//		}
	//
	//		mBeginScale = false;
	//	}
	//

	//
	//	boolean scaleAnim(long tick) {
	//
	//		if (mPrevScale >= 1) {
	//			mTimer = null;
	//			return false;
	//		}
	//
	//		float adv = (SCALE_DURATION - tick) / SCALE_DURATION;
	//		//		adv = mInterpolator.getInterpolation(adv);
	//		adv = mLinearInterpolator.getInterpolation(adv);
	//
	//		float scale = adv - mPrevScale;
	//		mPrevScale += scale;
	//
	//		if (mZooutOut) {
	//			mMapPosition.scaleMap(1 - scale, 0, 0);
	//		} else {
	//			mMapPosition.scaleMap(1 + scale, mFocusX, mFocusY);
	//		}
	//
	//		mMapView.redrawMap(true);
	//
	//		if (tick == 0)
	//			mTimer = null;
	//
	//		return true;
	//	}
}
