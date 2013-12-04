package org.oscim.event;

import org.oscim.map.Map;

public class GestureDetector {

	private final Map mMap;

	public GestureDetector(Map map) {
		mMap = map;
	}

	public boolean onTouchEvent(MotionEvent e) {
		if (e.getAction() == MotionEvent.ACTION_DOWN) {
			return mMap.handleGesture(Gesture.PRESS, e);
		}

		return false;
	}
}
