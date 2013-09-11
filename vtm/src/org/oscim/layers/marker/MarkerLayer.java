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

package org.oscim.layers.marker;

import org.oscim.core.Point;
import org.oscim.layers.InputLayer;
import org.oscim.map.Map;

/**
 * Base class representing an overlay which may be displayed on top of a
 * {@link Map}. To add an overlay, subclass this class, create an instance,
 * and add via addOverlay() of {@link Map}.
 * This class implements a form of Gesture Handling similar to
 * {@link android.view.GestureDetector.SimpleOnGestureListener} and
 * GestureDetector.OnGestureListener.
 *
 * @author Nicolas Gramlich
 */
public abstract class MarkerLayer extends InputLayer {

	public MarkerLayer(Map map) {
		super(map);
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
		boolean onSnapToItem(int x, int y, Point snapPoint);
	}
}
