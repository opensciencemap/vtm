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
package org.mapsforge.android.input;

import org.mapsforge.android.MapView;
import org.mapsforge.core.Tile;

import android.content.Context;
import android.os.CountDownTimer;
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
public class TouchHandler {
	private static final int INVALID_POINTER_ID = -1;

	/**
	 * is pritected correct? share MapView with inner class
	 */
	protected final MapView mMapView;

	private final float mMapMoveDelta;
	private boolean mMoveThresholdReached;
	private float mPreviousPositionX;
	private float mPreviousPositionY;
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
		mMapMoveDelta = viewConfiguration.getScaledTouchSlop();
		mActivePointerId = INVALID_POINTER_ID;
		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(
				mMapView));
		mGestureDetector = new GestureDetector(new MapGestureDetector(mMapView));
	}

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionCancel() {
		mActivePointerId = INVALID_POINTER_ID;
		return true;
	}

	private boolean onActionDown(MotionEvent motionEvent) {
		mPreviousPositionX = motionEvent.getX();
		mPreviousPositionY = motionEvent.getY();
		mMoveThresholdReached = false;
		// save the ID of the pointer
		mActivePointerId = motionEvent.getPointerId(0);
		return true;
	}

	private boolean onActionMove(MotionEvent motionEvent) {
		int pointerIndex = motionEvent.findPointerIndex(mActivePointerId);

		// calculate the distance between previous and current position
		float moveX = motionEvent.getX(pointerIndex) - mPreviousPositionX;
		float moveY = motionEvent.getY(pointerIndex) - mPreviousPositionY;
		boolean scaling = mScaleGestureDetector.isInProgress();
		if (!scaling && !mMoveThresholdReached) {

			if (Math.abs(moveX) > 3 * mMapMoveDelta
					|| Math.abs(moveY) > 3 * mMapMoveDelta) {
				// the map movement threshold has been reached
				// longPressDetector.pressStop();
				mMoveThresholdReached = true;

				// save the position of the event
				mPreviousPositionX = motionEvent.getX(pointerIndex);
				mPreviousPositionY = motionEvent.getY(pointerIndex);
			}
			return true;
		}

		// save the position of the event
		mPreviousPositionX = motionEvent.getX(pointerIndex);
		mPreviousPositionY = motionEvent.getY(pointerIndex);

		if (scaling) {
			return true;
		}

		mMapView.getMapPosition().moveMap(moveX, moveY);
		mMapView.redrawTiles();

		return true;
	}

	// private boolean onActionPointerDown(MotionEvent motionEvent) {
	// longPressDetector.pressStop();
	// multiTouchDownTime = motionEvent.getEventTime();
	// return true;
	// }

	private boolean onActionPointerUp(MotionEvent motionEvent) {

		// extract the index of the pointer that left the touch sensor
		int pointerIndex = (motionEvent.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
		if (motionEvent.getPointerId(pointerIndex) == mActivePointerId) {
			// the active pointer has gone up, choose a new one
			if (pointerIndex == 0) {
				pointerIndex = 1;
			} else {
				pointerIndex = 0;
			}
			// save the position of the event
			mPreviousPositionX = motionEvent.getX(pointerIndex);
			mPreviousPositionY = motionEvent.getY(pointerIndex);
			mActivePointerId = motionEvent.getPointerId(pointerIndex);
		}

		// calculate the time difference since the pointer has gone down
		// long multiTouchTime = motionEvent.getEventTime() -
		// multiTouchDownTime;
		// if (multiTouchTime < doubleTapTimeout) {
		// // multi-touch tap event, zoom out
		// previousEventTap = false;
		// mapView.zoom((byte) -1);
		// }
		return true;
	}

	/**
	 * @param motionEvent
	 *            ...
	 * @return ...
	 */
	private boolean onActionUp(MotionEvent motionEvent) {
		// longPressDetector.pressStop();
		// int pointerIndex = motionEvent.findPointerIndex(mActivePointerId);

		mActivePointerId = INVALID_POINTER_ID;
		// if (mMoveThresholdReached // || longPressDetector.isEventHandled()
		// ) {
		// mPreviousEventTap = false;
		// } else {
		// if (mPreviousEventTap) {
		//
		// } else {
		// mPreviousEventTap = true;
		// }
		//
		// // store the position and the time of this tap event
		// mPreviousTapX = motionEvent.getX(pointerIndex);
		// mPreviousTapY = motionEvent.getY(pointerIndex);
		// mPreviousTapTime = motionEvent.getEventTime();
		//
		// }
		return true;
	}

	private long lastRun = 0;

	/**
	 * @param motionEvent
	 *            ...
	 * @return ...
	 */
	public boolean handleMotionEvent(MotionEvent motionEvent) {

		// workaround for a bug in the ScaleGestureDetector, see Android issue
		// #12976
		if (motionEvent.getAction() != MotionEvent.ACTION_MOVE
				|| motionEvent.getPointerCount() > 1) {
			mScaleGestureDetector.onTouchEvent(motionEvent);
		}

		mGestureDetector.onTouchEvent(motionEvent);
		// if () {
		// // mActivePointerId = INVALID_POINTER_ID;
		// // return true;
		// }
		int action = getAction(motionEvent);
		boolean ret = false;
		if (action == MotionEvent.ACTION_DOWN) {
			ret = onActionDown(motionEvent);
		} else if (action == MotionEvent.ACTION_MOVE) {
			ret = onActionMove(motionEvent);
		} else if (action == MotionEvent.ACTION_UP) {
			ret = onActionUp(motionEvent);
		} else if (action == MotionEvent.ACTION_CANCEL) {
			ret = onActionCancel();
			// } else if (action == MotionEvent.ACTION_POINTER_DOWN) {
			// return onActionPointerDown(motionEvent);
		} else if (action == MotionEvent.ACTION_POINTER_UP) {
			ret = onActionPointerUp(motionEvent);
		}

		// if (ret) {
		// Log.d("", "" + );
		//
		// // try {
		// //
		// // Thread.sleep(10);
		// // } catch (InterruptedException e) {
		// // // TODO Auto-generated catch block
		// // // e.printStackTrace();
		// // }
		//
		// }
		if (ret) {
			// throttle input
			long diff = SystemClock.uptimeMillis() - lastRun;
			if (diff < 16) {
				// Log.d("", "" + diff);
				SystemClock.sleep(16 - diff);
			}
			lastRun = SystemClock.uptimeMillis();
		}
		// the event was not handled
		return ret;
	}

	class MapGestureDetector extends SimpleOnGestureListener {
		private Scroller mScroller;
		private float mPrevX, mPrevY;
		private CountDownTimer mTimer = null;

		public MapGestureDetector(MapView mapView) {
			mScroller = new Scroller(mapView.getContext(), new DecelerateInterpolator());
		}

		@Override
		public boolean onDown(MotionEvent e) {
			mScroller.forceFinished(true);

			if (mTimer != null) {
				mTimer.cancel();
				mTimer = null;
			}
			return true;
		}

		boolean scroll() {
			if (mScroller.isFinished()) {
				return false;
			}
			mScroller.computeScrollOffset();
			float moveX = mScroller.getCurrX() - mPrevX;
			float moveY = mScroller.getCurrY() - mPrevY;

			if (moveX >= 1 || moveY >= 1 || moveX <= -1 || moveY <= -1) {
				mMapView.getMapPosition().moveMap(moveX, moveY);
				mMapView.redrawTiles();
				mPrevX = mScroller.getCurrX();
				mPrevY = mScroller.getCurrY();
			}
			return true;
		}

		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			int w = Tile.TILE_SIZE * 20;
			int h = Tile.TILE_SIZE * 20;
			mPrevX = 0;
			mPrevY = 0;

			if (mTimer != null) {
				mTimer.cancel();
				mTimer = null;
			}

			mScroller.fling(0, 0, Math.round(velocityX) / 2, Math.round(velocityY) / 2,
					-w, w, -h, h);
			// animate for two seconds
			mTimer = new CountDownTimer(2000, 20) {
				@Override
				public void onTick(long tick) {
					if (!scroll())
						cancel();
				}

				@Override
				public void onFinish() {
					// do nothing
				}
			}.start();

			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			// mMapView.zoom((byte) 1);
			Log.d("mapsforge", "long press");
			// return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			mMapView.zoom((byte) 1);
			Log.d("mapsforge", "double tap");
			return true;
		}
	}
}
