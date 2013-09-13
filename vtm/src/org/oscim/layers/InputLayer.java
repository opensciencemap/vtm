/*
 * Copyright 2012 osmdroid authors
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
package org.oscim.layers;

import org.oscim.event.KeyEvent;
import org.oscim.event.MotionEvent;
import org.oscim.map.Map;

public abstract class InputLayer extends Layer {

	public InputLayer(Map map) {
		super(map);

	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param keyCode
	 *            ...
	 * @param event
	 *            ...
	 * @return ...
	 */
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param keyCode
	 *            ...
	 * @param event
	 *            ...
	 * @return ...
	 */
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		return false;
	}

	/**
	 * <b>You can prevent all(!) other Touch-related events from happening!</b><br />
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param event
	 *            ...
	 * @return ...
	 */
	public boolean onTouchEvent(MotionEvent event) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onTrackballEvent(MotionEvent e) {
		return false;
	}

	/** GestureDetector.OnDoubleTapListener **/

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onDoubleTap(MotionEvent e) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onDoubleTapEvent(MotionEvent e) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onSingleTapConfirmed(MotionEvent e) {
		return false;
	}

	/** OnGestureListener **/

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onDown(MotionEvent e) {
		return false;
	}

	///**
	// * By default does nothing (<code>return false</code>). If you handled the
	// * Event, return <code>true</code>, otherwise return <code>false</code>. If
	// * you returned <code>true</code> none of the following Overlays or the
	// * underlying {@link MapView} has the chance to handle this event.
	// *
	// * @param pEvent1
	// *            ...
	// * @param pEvent2
	// *            ...
	// * @param pVelocityX
	// *            ...
	// * @param pVelocityY
	// *            ...
	// * @return ...
	// */
	//public boolean onFling(MotionEvent pEvent1, MotionEvent pEvent2,
	//		float pVelocityX, float pVelocityY) {
	//	return false;
	//}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onLongPress(MotionEvent e) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param pEvent1
	 *            ...
	 * @param pEvent2
	 *            ...
	 * @param pDistanceX
	 *            ...
	 * @param pDistanceY
	 *            ...
	 * @return ...
	 */
	public boolean onScroll(MotionEvent pEvent1, MotionEvent pEvent2,
			float pDistanceX, float pDistanceY) {
		return false;
	}

	/**
	 * @param pEvent
	 *            ...
	 */
	public void onShowPress(MotionEvent pEvent) {
		return;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link Map} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

}
