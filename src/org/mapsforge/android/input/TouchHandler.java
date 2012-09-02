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
import org.mapsforge.android.MapViewPosition;
import org.mapsforge.core.Tile;

import android.content.Context;
import android.os.CountDownTimer;
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

	protected final MapView mMapView;
	protected final MapViewPosition mMapPosition;

	private final float mMapMoveDelta;
	private boolean mMoveThresholdReached;
	private float mPosX;
	private float mPosY;
	private float mAngle;

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
		mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener(
				mMapView));
		mGestureDetector = new GestureDetector(context, new MapGestureDetector(mMapView));

	}

	private static int getAction(MotionEvent motionEvent) {
		return motionEvent.getAction() & MotionEvent.ACTION_MASK;
	}

	private boolean onActionCancel() {
		mActivePointerId = INVALID_POINTER_ID;
		return true;
	}

	private boolean onActionDown(MotionEvent event) {
		mPosX = event.getX();
		mPosY = event.getY();
		mMoveThresholdReached = false;
		// save the ID of the pointer
		mActivePointerId = event.getPointerId(0);
		// Log.d("...", "set active pointer" + mActivePointerId);

		return true;
	}

	private boolean mScaling = false;

	private boolean onActionMove(MotionEvent event) {
		int pointerIndex = event.findPointerIndex(mActivePointerId);

		// calculate the distance between previous and current position
		float moveX = event.getX(pointerIndex) - mPosX;
		float moveY = event.getY(pointerIndex) - mPosY;

		boolean scaling = mScaleGestureDetector.isInProgress();
		if (!mScaling) {
			mScaling = scaling;
		}
		if (!scaling && !mMoveThresholdReached) {

			if (Math.abs(moveX) > 3 * mMapMoveDelta
					|| Math.abs(moveY) > 3 * mMapMoveDelta) {
				// the map movement threshold has been reached
				// longPressDetector.pressStop();
				mMoveThresholdReached = true;

				// save the position of the event
				mPosX = event.getX(pointerIndex);
				mPosY = event.getY(pointerIndex);
			}
			return true;
		}

		// if (multi > 0) {
		// double dx = event.getX(0) - event.getX(1);
		// double dy = event.getY(0) - event.getY(1);
		// double rad = Math.atan2(dy, dx);
		// float angle = (float) Math.toDegrees(rad);
		// mMapPosition.rotateMap(angle - mAngle);
		// mAngle = angle;
		// mMapView.redrawTiles();
		// }

		// save the position of the event
		mPosX = event.getX(pointerIndex);
		mPosY = event.getY(pointerIndex);

		if (scaling) {
			return true;
		}

		mMapPosition.moveMap(moveX, moveY);
		mMapView.redrawTiles();

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
			double rad = Math.atan2(dy, dx);
			mAngle = (float) Math.toDegrees(rad);
		}

		return true;
	}

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
			mPosX = motionEvent.getX(pointerIndex);
			mPosY = motionEvent.getY(pointerIndex);
			mActivePointerId = motionEvent.getPointerId(pointerIndex);
		}
		multi--;

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

		return true;
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

	class MapGestureDetector extends SimpleOnGestureListener {
		private Scroller mScroller;
		private float mPrevX, mPrevY, mPrevScale;
		private CountDownTimer mTimer = null;
		private boolean fling = false;

		public MapGestureDetector(MapView mapView) {
			mScroller = new Scroller(mapView.getContext(),
					new android.view.animation.LinearInterpolator());
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
			// Log.d("mapsforge", "onDown");

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
				mMapPosition.moveMap(moveX, moveY);
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

		private DecelerateInterpolator mBounce = new DecelerateInterpolator();

		@Override
		public void onLongPress(MotionEvent e) {
			// mMapView.zoom((byte) 1);
			// Log.d("mapsforge", "long press");
		}

		private final float mScaleDuration = 300;

		boolean scale(long tick) {

			fling = true;
			if (mPrevScale >= 1)
				return false;
			float adv = (mScaleDuration - tick) / mScaleDuration;
			adv = mBounce.getInterpolation(adv);

			float scale = adv - mPrevScale;
			mPrevScale += scale;
			scale += 1;
			adv += 1;

			if (scale > 1) {
				mMapPosition.scaleMap(scale, mPrevX / adv, mPrevY / adv);
				mMapView.redrawTiles();
			}

			return true;
		}

		@Override
		public boolean onDoubleTap(MotionEvent e) {
			// Log.d("mapsforge", "double tap");

			mPrevX = (e.getX(0) - (mMapView.getWidth() >> 1)) * 2f;
			mPrevY = (e.getY(0) - (mMapView.getHeight() >> 1)) * 2f;
			mPrevScale = 0;

			mTimer = new CountDownTimer((int) mScaleDuration, 30) {
				@Override
				public void onTick(long tick) {
					scale(tick);
				}

				@Override
				public void onFinish() {
					scale(0);
				}
			}.start();

			return true;
		}
	}
}
