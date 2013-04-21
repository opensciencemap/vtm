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

import org.oscim.core.PointF;
import org.oscim.layers.InputLayer;
import org.oscim.view.MapView;

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
public abstract class Overlay extends InputLayer {

	public Overlay(MapView mapView) {
		super(mapView);
	}

	/**
	 * TBD
	 *
	 * Interface definition for overlays that contain items that can be snapped
	 * to (for example, when the user invokes a zoom, this could be called
	 * allowing the user to snap the zoom to an interesting point.)
	 *
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
}
