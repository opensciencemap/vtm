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
package org.mapsforge.android.maps.swrenderer;


import android.graphics.Paint;

class WayTextContainer {
	final WayDataContainer wayDataContainer;
	final int first;
	final int last;
	final Paint paint;
	final String text;
	short x1, y1, x2, y2;
	boolean match;

	WayTextContainer(int first, int last, WayDataContainer wayDataContainer, String text, Paint paint) {
		this.wayDataContainer = wayDataContainer;
		this.first = first;
		this.last = last;
		this.text = text;
		this.paint = paint;
		this.match = false;
	}
}
