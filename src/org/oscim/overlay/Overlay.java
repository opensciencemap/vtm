/*
 * Copyright 2012 osmdroid
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

package org.oscim.overlay;

import org.oscim.core.MapPosition;
import org.oscim.core.PointF;
import org.oscim.renderer.overlays.RenderOverlay;
import org.oscim.view.MapView;

import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * Base class representing an overlay which may be displayed on top of a
 * {@link MapView}. To add an overlay, subclass this class, create an instance,
 * and add via addOverlay() of {@link MapView}.
 * This class implements a form of Gesture Handling similar to
 * {@link android.view.GestureDetector.SimpleOnGestureListener} and
 * GestureDetector.OnGestureListener.
 *
 * @author Nicolas Gramlich
 */
public abstract class Overlay {

	// ===========================================================
	// Constants
	// ===========================================================

	//private static AtomicInteger sOrdinal = new AtomicInteger();

	// From Google Maps API
	//protected static final float SHADOW_X_SKEW = -0.8999999761581421f;
	//protected static final float SHADOW_Y_SCALE = 0.5f;

	// ===========================================================
	// Fields
	// ===========================================================

	private boolean mEnabled = true;
	private boolean mReceiveEvents = true;

	protected final MapView mMapView;

	/** RenderOverlay used to draw this layer. To be implemented by sub-classes */
	protected RenderOverlay mLayer;

	public RenderOverlay getLayer() {
		return mLayer;
	}

	// ===========================================================
	// Constructors
	// ===========================================================

	public Overlay(MapView mapView) {
		mMapView = mapView;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	/**
	 * Sets whether the Overlay is marked to be enabled. This setting does
	 * nothing by default, but should be checked before calling draw().
	 *
	 * @param pEnabled
	 *            ...
	 */
	public void setEnabled(boolean pEnabled) {
		mEnabled = pEnabled;
	}

	/**
	 * Specifies if the Overlay is marked to be enabled. This should be checked
	 * before calling draw().
	 *
	 * @return true if the Overlay is marked enabled, false otherwise
	 */
	public boolean isEnabled() {
		return mEnabled;
	}

	/**
	 * Sets whether the Overlay is marked to be receive touch exents.
	 *
	 * @param pEnabled
	 *            ...
	 */
	public void setEnableEvents(boolean pEnabled) {
		mReceiveEvents = pEnabled;
	}

	/**
	 * Specifies if the Overlay is marked to be enabled. This should be checked
	 * before calling draw().
	 *
	 * @return true if the Overlay is marked enabled, false otherwise
	 */
	public boolean eventsEnabled() {
		return mReceiveEvents;
	}

	///**
	// * Since the menu-chain will pass through several independent Overlays, menu
	// * IDs cannot be fixed at compile time. Overlays should use this method to
	// * obtain and store a menu id for each menu item at construction time. This
	// * will ensure that two overlays don't use the same id.
	// *
	// * @return an integer suitable to be used as a menu identifier
	// */
	//protected final static int getSafeMenuId() {
	//	return sOrdinal.getAndIncrement();
	//}
	//
	///**
	// * Similar to <see cref="getSafeMenuId" />, except this reserves a sequence
	// * of IDs of length <param name="count" />. The returned number is the
	// * starting index of that sequential list.
	// *
	// * @param count
	// *            ....
	// * @return an integer suitable to be used as a menu identifier
	// */
	//protected final static int getSafeMenuIdSequence(int count) {
	//	return sOrdinal.getAndAdd(count);
	//}

	// ===========================================================
	// Methods
	// ===========================================================

	/**
	 * Called before each frame render request.
	 *
	 * @param mapPosition
	 *            current MapPosition
	 * @param changed
	 *            true when MapPosition has changed since last call
	 */
	public void onUpdate(MapPosition mapPosition, boolean changed) {

	}

	/**
	 * Override to perform clean up of resources before shutdown. By default
	 * does nothing.
	 */
	public void onDetach() {
		// FIXME call to this function is not implemented
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onTouchEvent(MotionEvent e) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onDown(MotionEvent e) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link MapView} has the chance to handle this event.
	 *
	 * @param pEvent1
	 *            ...
	 * @param pEvent2
	 *            ...
	 * @param pVelocityX
	 *            ...
	 * @param pVelocityY
	 *            ...
	 * @return ...
	 */
	public boolean onFling(MotionEvent pEvent1, MotionEvent pEvent2,
			float pVelocityX, float pVelocityY) {
		return false;
	}

	/**
	 * By default does nothing (<code>return false</code>). If you handled the
	 * Event, return <code>true</code>, otherwise return <code>false</code>. If
	 * you returned <code>true</code> none of the following Overlays or the
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
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
	 * underlying {@link MapView} has the chance to handle this event.
	 *
	 * @param e
	 *            ...
	 * @return ...
	 */
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================

	/**
	 * Interface definition for overlays that contain items that can be snapped
	 * to (for example, when the user invokes a zoom, this could be called
	 * allowing the user to snap the zoom to an interesting point.)
	 */
	public interface Snappable {

		/**
		 * Checks to see if the given x and y are close enough to an item
		 * resulting in snapping the current action (e.g. zoom) to the item.
		 *
		 * @param x
		 *            The x in screen coordinates.
		 * @param y
		 *            The y in screen coordinates.
		 * @param snapPoint
		 *            To be filled with the the interesting point (in screen
		 *            coordinates) that is closest to the given x and y. Can be
		 *            untouched if not snapping.
		 * @return Whether or not to snap to the interesting point.
		 */
		boolean onSnapToItem(int x, int y, PointF snapPoint);
	}

}
