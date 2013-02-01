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
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * @author Hannes Janetzek
 * @TODO:
 *        - use one AnimationTimer instead of CountDownTimers
 *        - fix recognition of tilt/rotate/scale state...
 */

final class TouchHandler implements OnGestureListener, OnScaleGestureListener, OnDoubleTapListener {

	private static final String TAG = TouchHandler.class.getName();

	private static final float SCALE_DURATION = 500;
	private static final float ROTATION_DELAY = 200; // ms

	private static final int INVALID_POINTER_ID = -1;

	private final MapView mMapView;
	private final MapViewPosition mMapPosition;
	private final OverlayManager mOverlayManager;

	private final DecelerateInterpolator mInterpolator;
	private final DecelerateInterpolator mLinearInterpolator;
	private boolean mBeginScale;
	private float mSumScale;

	private boolean mBeginRotate;
	private boolean mBeginTilt;
	private boolean mLongPress;

	//	private float mPosX;
	private float mPosY;
	private double mAngle;

	private int mActivePointerId;

	private final ScaleGestureDetector mScaleGestureDetector;
	private final GestureDetector mGestureDetector;

	private final float dpi;

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
		mActivePointerId = INVALID_POINTER_ID;
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector.setOnDoubleTapListener(this);

		mInterpolator = new DecelerateInterpolator(2f);

		mScroller = new Scroller(mMapView.getContext(), mInterpolator);
		mLinearInterpolator = new DecelerateInterpolator(0.8f);//new android.view.animation.LinearInterpolator();

		DisplayMetrics metrics = mapView.getResources().getDisplayMetrics();
		dpi = metrics.xdpi;
		Log.d(TAG, "dpi is: " + dpi);

	}

	/**
	 * @param event
	 *            ...
	 * @return ...
	 */
	public boolean handleMotionEvent(MotionEvent event) {

		if (mOverlayManager.onTouchEvent(event, mMapView))
			return true;

		mGestureDetector.onTouchEvent(event);
		mScaleGestureDetector.onTouchEvent(event);

		int action = getAction(event);

		if (action == MotionEvent.ACTION_DOWN) {
			mMulti = 0;
			mWasMulti = false;
			if (mOverlayManager.onDown(event, mMapView))
				return true;

			return onActionDown(event);
		} else if (action == MotionEvent.ACTION_MOVE) {
			return onActionMove(event);
		} else if (action == MotionEvent.ACTION_UP) {
			return onActionUp(event);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			return onActionCancel();
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			return onActionPointerDown(event);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			return onActionPointerUp(event);
		}

		return false;
	}

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionCancel() {
		mActivePointerId = INVALID_POINTER_ID;
		mLongPress = true;
		return true;
	}

	private boolean onActionDown(MotionEvent event) {
		//	mPosX = event.getX();
		mPosY = event.getY();

		// mMoveStart = false;
		mBeginRotate = false;
		mBeginTilt = false;
		// save the ID of the pointer
		mActivePointerId = event.getPointerId(0);
		// Log.d("...", "set active pointer" + mActivePointerId);

		return true;
	}

	private boolean mScaling = false;

	private boolean onActionMove(MotionEvent event) {
		int id = event.findPointerIndex(mActivePointerId);

		float py = event.getY(id);
		float moveY = py - mPosY;
		mPosY = py;

		// double-tap + hold
		if (mLongPress) {
			mMapPosition.scaleMap(1 - moveY / 100, 0, 0);
			mMapView.redrawMap(true);
			return true;
		}

		if (mMulti == 0)
			return true;

		if (event.getEventTime() - mMultiTouchDownTime < ROTATION_DELAY)
			return true;

		double x1 = event.getX(0);
		double x2 = event.getX(1);
		double y1 = event.getY(0);
		double y2 = event.getY(1);

		double dx = x1 - x2;
		double dy = y1 - y2;

		double rad = Math.atan2(dy, dx);
		double r = rad - mAngle;

		if (!mBeginRotate && !mBeginScale) {
			/* our naive gesture detector for rotation and tilt.. */

			if (Math.abs(rad) < 0.30 || Math.abs(rad) > Math.PI - 0.30) {
				mBeginTilt = true;
				if (mMapPosition.tilt(moveY / 4)) {
					mMapView.redrawMap(true);
				}

				return true;
			}

			if (!mBeginTilt) {
				if (Math.abs(r) > 0.05) {
					// Log.d(TAG, "begin rotate");
					mAngle = rad;
					mBeginRotate = true;
				}
			}
		}

		if (mBeginRotate) {
			double rsin = Math.sin(r);
			double rcos = Math.cos(r);

			// focus point relative to center
			double cx = (mMapView.getWidth() >> 1) - (x1 + x2) / 2;
			double cy = (mMapView.getHeight() >> 1) - (y1 + y2) / 2;

			float x = (float) (cx * rcos + cy * -rsin - cx);
			float y = (float) (cx * rsin + cy * rcos - cy);

			mMapPosition.rotateMap((float) Math.toDegrees(rad - mAngle), x, y);
			mAngle = rad;
			mMapView.redrawMap(true);
		}

		return true;
	}

	private int mMulti = 0;
	private boolean mWasMulti;
	private long mMultiTouchDownTime;

	private boolean onActionPointerDown(MotionEvent event) {

		mMultiTouchDownTime = event.getEventTime();

		mMulti++;
		mWasMulti = true;

		if (mMulti == 1) {
			double dx = event.getX(0) - event.getX(1);
			double dy = event.getY(0) - event.getY(1);
			mAngle = Math.atan2(dy, dx);
		}
		// Log.d("...", "mMulti down " + mMulti);
		return true;
	}

	private boolean onActionPointerUp(MotionEvent motionEvent) {

		// extract the index of the pointer that left the touch sensor
		int masked = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK);
		int pointerIndex = masked >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;

		if (motionEvent.getPointerId(pointerIndex) == mActivePointerId) {
			// the active pointer has gone up, choose a new one
			if (pointerIndex == 0) {
				pointerIndex = 1;
			} else {
				pointerIndex = 0;
			}
			// save the position of the event
			//	mPosX = motionEvent.getX(pointerIndex);
			mPosY = motionEvent.getY(pointerIndex);
			mActivePointerId = motionEvent.getPointerId(pointerIndex);
		}
		mMulti--;

		mLongPress = false;
		// Log.d("...", "mMulti up " + mMulti);

		return true;
	}

	/**
	 * @param motionEvent
	 *            ...
	 * @return ...
	 */
	private boolean onActionUp(MotionEvent motionEvent) {
		mActivePointerId = INVALID_POINTER_ID;
		mScaling = false;
		mLongPress = false;

		mMulti = 0;

		return true;
	}

	/******************* GestureListener *******************/

	private Scroller mScroller;
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

		if (mScaling)
			return true;

		if (mMulti == 0) {
			mMapPosition.moveMap(-distanceX, -distanceY);
			mMapView.redrawMap(true);
		}

		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {

		if (mScaling || mWasMulti)
			return true;

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
			float s = (300 / dpi) / 2;
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

	/******************* ScaleListener *******************/
	private float mCenterX;
	private float mCenterY;
	private float mFocusX;
	private float mFocusY;
	private long mTimeStart;
	private long mTimeEnd;

	@Override
	public boolean onScale(ScaleGestureDetector gd) {

		if (mBeginTilt)
			return true;

		float scale = gd.getScaleFactor();
		mFocusX = gd.getFocusX() - mCenterX;
		mFocusY = gd.getFocusY() - mCenterY;

		mSumScale *= scale;

		mTimeEnd = SystemClock.elapsedRealtime();

		if (!mBeginScale) {
			if (mSumScale > 1.1 || mSumScale < 0.9) {
				// Log.d("...", "begin scale " + mSumScale);
				mBeginScale = true;
				// scale = mSumScale;
			}
		}

		if (mBeginScale && mMapPosition.scaleMap(scale, mFocusX, mFocusY))
			mMapView.redrawMap(true);

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector gd) {
		mScaling = true;
		mBeginScale = false;

		mTimeEnd = mTimeStart = SystemClock.elapsedRealtime();
		mSumScale = 1;
		mCenterX = mMapView.getWidth() >> 1;
		mCenterY = mMapView.getHeight() >> 1;

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}
		return true;
	}

	@Override
	public void onScaleEnd(ScaleGestureDetector gd) {
		// Log.d("ScaleListener", "Sum " + mSumScale + " " + (mTimeEnd -
		// mTimeStart));

		if (mTimer == null && mTimeEnd - mTimeStart < 150
				&& (mSumScale < 0.99 || mSumScale > 1.01)) {

			mPrevScale = 0;

			mZooutOut = mSumScale < 0.99;

			mTimer = new CountDownTimer((int) SCALE_DURATION, 32) {
				@Override
				public void onTick(long tick) {
					scaleAnim(tick);
				}

				@Override
				public void onFinish() {
					scaleAnim(0);
				}
			}.start();
		} else {
			mScaling = false;
		}

		mBeginScale = false;
	}

	private float mPrevScale;
	private CountDownTimer mTimer;
	boolean mZooutOut;

	boolean scaleAnim(long tick) {

		if (mPrevScale >= 1) {
			mTimer = null;
			return false;
		}

		float adv = (SCALE_DURATION - tick) / SCALE_DURATION;
		//		adv = mInterpolator.getInterpolation(adv);
		adv = mLinearInterpolator.getInterpolation(adv);

		float scale = adv - mPrevScale;
		mPrevScale += scale;

		if (mZooutOut) {
			mMapPosition.scaleMap(1 - scale, 0, 0);
		} else {
			mMapPosition.scaleMap(1 + scale, mFocusX, mFocusY);
		}

		mMapView.redrawMap(true);

		if (tick == 0)
			mTimer = null;

		return true;
	}
}
