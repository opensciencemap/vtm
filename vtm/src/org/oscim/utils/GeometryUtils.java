/*
 * Copyright 2012, 2013 Hannes Janetzek
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
package org.oscim.utils;

public final class GeometryUtils {

	private GeometryUtils() {
	}

	// from www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
	/**
	 * test if point x/y is in polygon defined by vertices[offset ...
	 * offset+length]
	 */
	public static boolean pointInPoly(float x, float y, float[] vertices,
	        int length, int offset) {

		int end = (offset + length);

		boolean inside = false;
		for (int i = offset, j = (end - 2); i < end; j = i, i += 2) {
			if (((vertices[i + 1] > y) != (vertices[j + 1] > y)) &&
			        (x < (vertices[j] - vertices[i]) * (y - vertices[i + 1])
			                / (vertices[j + 1] - vertices[i + 1]) + vertices[i]))
				inside = !inside;
		}
		return inside;
	}

	public static float area(float ax, float ay, float bx, float by, float cx, float cy) {
		float area = ((ax - cx) * (by - cy) - (bx - cx) * (ay - cy)) * 0.5f;
		return area < 0 ? -area : area;
	}
}
