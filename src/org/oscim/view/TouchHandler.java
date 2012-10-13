/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

/**
 * Implementation for multi-touch capable devices.
 */

// TODO:
// - write a AnimationTimer instead of using CountDownTimers
// - fix recognition of tilt/rotate/scale state...

final class TouchHandler
		extends SimpleOnGestureListener
		implements ScaleGestureDetector.OnScaleGestureListener {

	private static final float SCALE_DURATION = 450;
	private static final int INVALID_POINTER_ID = -1;

	private final MapView mMapView;
	private final MapViewPosition mMapPosition;
	private final DecelerateInterpolator mInterpolator = new DecelerateInterpolator();
	private boolean mBeginScale;
	private float mSumScale;

	private final float mMapMoveDelta;
	private boolean mMoveStart;
	private boolean mBeginRotate;
	private boolean mBeginTilt;
	private boolean mLongPress;
	private long mLongPressTime;

	private float mPosX;
	private float mPosY;
	private double mAngle;

	private int mActivePointerId;

	private final ScaleGestureDetector mScaleGestureDetector;
	private final GestureDetector mGestureDetector;

	/**
	 * @param context
	 *            the Context
	 * @param mapView
	 *            the MapView
	 */
	public TouchHandler(Context context, MapView mapView) {
		ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
		mMapView = mapView;
		mMapPosition = mapView.getMapPosition();
		mMapMoveDelta = viewConfiguration.getScaledTouchSlop();
		mActivePointerId = INVALID_POINTER_ID;
		mScaleGestureDetector = new ScaleGestureDetector(context, this);
		mGestureDetector = new GestureDetector(context, this);

		mScroller = new Scroller(mMapView.getContext(),
				new android.view.animation.LinearInterpolator());
	}

	/**
	 * @param event
	 *            ...
	 * @return ...
	 */
	public boolean handleMotionEvent(MotionEvent event) {

		// workaround for a bug in the ScaleGestureDetector, see Android issue
		// #12976
		// if (event.getAction() != MotionEvent.ACTION_MOVE
		// || event.getPointerCount() > 1) {

		mScaleGestureDetector.onTouchEvent(event);
		// }

		if (!mScaling)
			mGestureDetector.onTouchEvent(event);

		int action = getAction(event);
		boolean ret = false;
		if (action == MotionEvent.ACTION_DOWN) {
			ret = onActionDown(event);
		} else if (action == MotionEvent.ACTION_MOVE) {
			ret = onActionMove(event);
		} else if (action == MotionEvent.ACTION_UP) {
			ret = onActionUp(event);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			ret = onActionCancel();
		} else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			return onActionPointerDown(event);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			ret = onActionPointerUp(event);
		}

		return ret;
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
		mPosX = event.getX();
		mPosY = event.getY();

		mMoveStart = false;
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

		// calculate the distance between previous and current position
		float moveX = event.getX(id) - mPosX;
		float moveY = event.getY(id) - mPosY;
		// save the position of the event

		// Log.d("...", "mx " + moveX + " my " + moveY);

		boolean scaling = mScaleGestureDetector.isInProgress();

		if (!mScaling) {
			mScaling = scaling;
		}

		if (!scaling && !mMoveStart) {

			if (Math.abs(moveX) > 3 * mMapMoveDelta
					|| Math.abs(moveY) > 3 * mMapMoveDelta) {
				// the map movement threshold has been reached
				// longPressDetector.pressStop();
				mMoveStart = true;
			}

			return true;
		}

		mPosX = event.getX(id);
		mPosY = event.getY(id);

		if (mLongPress) {
			mMapPosition.scaleMap(1 - moveY / 100, 0, 0);
			mMapView.redrawMap();
			return true;
		}

		if (!scaling) {
			mMapPosition.moveMap(moveX, moveY);
			mMapView.redrawMap();
			return true;
		}

		if (!mMapView.enableRotation || multi < 1)
			return true;

		double x1 = event.getX(0);
		double x2 = event.getX(1);
		double y1 = event.getY(0);
		double y2 = event.getY(1);

		double dx = x1 - x2;
		double dy = y1 - y2;

		double rad = Math.atan2(dy, dx);
		double r = rad - mAngle;

		if (!mBeginRotate && Math.abs(rad) < 0.25 || Math.abs(rad) > Math.PI - 0.25) {
			// if (Math.abs(moveX) > 3 * mMapMoveDelta) {
			mBeginTilt = true;
			if (mMapPosition.tilt(moveY / 4)) {
				mMapView.redrawMap();
			}
			// }
			return true;
		}

		if (!mBeginRotate && !mBeginScale && !mBeginTilt) {
			if (Math.abs(r) > 0.03)
				mBeginRotate = true;
		}

		// quick way to prevent flipping...
		// Log.d("", "rotation " + rad + " " + r);
		if (Math.abs(r) > 0.1) {
			rad = mAngle;
			r = 0;
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
			mMapView.redrawMap();
		}

		return true;
	}

	private int multi = 0;

	private boolean onActionPointerDown(MotionEvent event) {
		// longPressDetector.pressStop();
		// multiTouchDownTime = motionEvent.getEventTime();
		multi++;
		if (multi == 1) {
			double dx = event.getX(0) - event.getX(1);
			double dy = event.getY(0) - event.getY(1);
			mAngle = Math.atan2(dy, dx);
		}
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
			mPosX = motionEvent.getX(pointerIndex);
			mPosY = motionEvent.getY(pointerIndex);
			mActivePointerId = motionEvent.getPointerId(pointerIndex);
		}
		multi--;

		mLongPress = false;

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
		multi = 0;

		// if (mLongPress && SystemClock.uptimeMillis() - mLongPressTime < 150)
		// {
		// mScrollX = (mPosX - (mMapView.getWidth() >> 1)) * 2f;
		// mScrollY = (mPosY - (mMapView.getHeight() >> 1)) * 2f;
		// mPrevScale = 0;
		//
		// mTimer = new CountDownTimer((int) SCALE_DURATION, 30) {
		// @Override
		// public void onTick(long tick) {
		// scale2(tick);
		// }
		//
		// @Override
		// public void onFinish() {
		// scale2(0);
		// }
		// }.start();
		// }

		mLongPress = false;

		return true;
	}

	/******************* SimpleOnGestureListener *******************/

	private Scroller mScroller;
	private float mScrollX, mScrollY;
	private boolean fling = false;

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
			mMapView.redrawMap();
			mScrollX = mScroller.getCurrX();
			mScrollY = mScroller.getCurrY();
		}
		return true;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		int w = Tile.TILE_SIZE * 20;
		int h = Tile.TILE_SIZE * 20;
		mScrollX = 0;
		mScrollY = 0;

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		mScroller.fling(0, 0, Math.round(velocityX) / 2, Math.round(velocityY) / 2,
				-w, w, -h, h);

		// animate for two seconds
		mTimer = new CountDownTimer(1500, 50) {
			@Override
			public void onTick(long tick) {
				scroll();
			}

			@Override
			public void onFinish() {
				// do nothing
			}
		}.start();
		fling = true;
		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		if (MapView.testRegionZoom) {
			Log.d("mapsforge", "long press");
			mMapView.mRegionLookup.updateRegion(-1, null);
		}
		mLongPress = true;
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
			mMapView.redrawMap();
		}

		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		if (MapView.testRegionZoom) {
			mMapView.mRegionLookup.updateRegion(1,
					mMapPosition.getOffsetPoint(mPosX, mPosY));
		} else {
			mLongPress = true;
			mLongPressTime = SystemClock.uptimeMillis();
			// mScrollX = (e.getX(0) - (mMapView.getWidth() >> 1)) * 2f;
			// mScrollY = (e.getY(0) - (mMapView.getHeight() >> 1)) * 2f;
			// mPrevScale = 0;
			//
			// mTimer = new CountDownTimer((int) SCALE_DURATION, 30) {
			// @Override
			// public void onTick(long tick) {
			// scale2(tick);
			// }
			//
			// @Override
			// public void onFinish() {
			// scale(0);
			// }
			// }.start();
		}
		return true;
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
			if (mTimeEnd - mTimeStart > 200 || mSumScale > 1.1 || mSumScale < 0.9) {
				mBeginScale = true;
				scale = mSumScale;
			} else
				return true;
		}

		if (mMapPosition.scaleMap(scale, mFocusX, mFocusY))
			mMapView.redrawMap();

		return true;
	}

	@Override
	public boolean onScaleBegin(ScaleGestureDetector gd) {
		mTimeEnd = mTimeStart = SystemClock.elapsedRealtime();
		mSumScale = 1;
		mBeginScale = false;
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

			mTimer = new CountDownTimer((int) SCALE_DURATION, 15) {
				@Override
				public void onTick(long tick) {
					scale(tick);
				}

				@Override
				public void onFinish() {
					scale(0);

				}
			}.start();
		}
		mBeginScale = false;
	}

	private float mPrevScale;
	private CountDownTimer mTimer;
	boolean mZooutOut;

	boolean scale(long tick) {

		if (mPrevScale >= 1) {
			mTimer = null;
			return false;
		}

		float adv = (SCALE_DURATION - tick) / SCALE_DURATION;
		adv = mInterpolator.getInterpolation(adv);

		float scale = adv - mPrevScale;
		mPrevScale += scale;

		if (mZooutOut) {
			mMapPosition.scaleMap(1 - scale, 0, 0);
		} else {
			mMapPosition.scaleMap(1 + scale, mFocusX, mFocusY);
		}

		mMapView.redrawMap();

		if (tick == 0)
			mTimer = null;

		return true;
	}

	/*
	 * from CountDownTimer.java: Copyright (C) 2008 The Android Open Source
	 * Project Licensed under the Apache License, Version 2.0 (the "License");
	 * you may not use this file except in compliance with the License. You may
	 * obtain a copy of the License at
	 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
	 * law or agreed to in writing, software distributed under the License is
	 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	 * KIND, either express or implied. See the License for the specific
	 * language governing permissions and limitations under the License.
	 */

	final static class Timer {

		/**
		 * Millis since epoch when alarm should stop.
		 */
		private final long mMillisInFuture;

		/**
		 * The interval in millis that the user receives callbacks
		 */
		final long mCountdownInterval;

		long mStopTimeInFuture;

		/**
		 * @param millisInFuture
		 *            The number of millis in the future from the call to
		 *            {@link #start()} until the countdown is done and
		 *            {@link #onFinish()} is called.
		 * @param countDownInterval
		 *            The interval along the way to receive
		 *            {@link #onTick(long)} callbacks.
		 */
		public Timer(long millisInFuture, long countDownInterval) {
			mMillisInFuture = millisInFuture;
			mCountdownInterval = countDownInterval;
		}

		/**
		 * Cancel the countdown.
		 */
		public final void cancel() {
			mHandler.removeMessages(MSG);
		}

		/**
		 * Start the countdown.
		 * 
		 * @return ...
		 */
		public synchronized final Timer start() {
			if (mMillisInFuture <= 0) {
				onFinish();
				return this;
			}
			mStopTimeInFuture = SystemClock.elapsedRealtime() + mMillisInFuture;
			mHandler.sendMessage(mHandler.obtainMessage(MSG));
			return this;
		}

		/**
		 * Callback fired on regular interval.
		 * 
		 * @param millisUntilFinished
		 *            The amount of time until finished.
		 */
		public void onTick(long millisUntilFinished) {
		}

		/**
		 * Callback fired when the time is up.
		 */
		public void onFinish() {
		}

		private static final int MSG = 1;

		// handles counting down
		private Handler mHandler = new Handler() {

			@Override
			public void handleMessage(Message msg) {

				synchronized (Timer.this) {
					final long millisLeft = mStopTimeInFuture
							- SystemClock.elapsedRealtime();

					if (millisLeft <= 0) {
						onFinish();
					} else if (millisLeft < mCountdownInterval) {
						// no tick, just delay until done
						sendMessageDelayed(obtainMessage(MSG), millisLeft);
					} else {
						long lastTickStart = SystemClock.elapsedRealtime();
						onTick(millisLeft);

						// take into account user's onTick taking time to
						// execute
						long delay = lastTickStart + mCountdownInterval
								- SystemClock.elapsedRealtime();

						// special case: user's onTick took more than interval
						// to
						// complete, skip to next interval
						while (delay < 0)
							delay += mCountdownInterval;

						sendMessageDelayed(obtainMessage(MSG), delay);
					}
				}
			}
		};
	}
}
