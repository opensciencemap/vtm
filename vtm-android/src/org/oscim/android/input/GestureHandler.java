package org.oscim.android.input;

import org.oscim.event.Gesture;
import org.oscim.map.Map;

import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;

public class GestureHandler implements OnGestureListener, OnDoubleTapListener {
	private final AndroidMotionEvent mMotionEvent;
	private final Map mMap;

	public GestureHandler(Map map) {
		mMotionEvent = new AndroidMotionEvent();
		mMap = map;
	}

	/* GesturListener */

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		//	return mMap.handleGesture(Gesture.TAP, mMotionEvent.wrap(e));
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {
		mMap.handleGesture(Gesture.LONG_PRESS, mMotionEvent.wrap(e));
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
		return false;
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return mMap.handleGesture(Gesture.PRESS, mMotionEvent.wrap(e));
	}

	/* DoubleTapListener */
	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return mMap.handleGesture(Gesture.TAP, mMotionEvent.wrap(e));
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		return mMap.handleGesture(Gesture.DOUBLE_TAP, mMotionEvent.wrap(e));
	}
}
