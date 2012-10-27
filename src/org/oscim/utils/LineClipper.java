/*
 * Copyright 2012 Hannes Janetzek
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

import android.graphics.Point;

// from http://en.wikipedia.org/wiki/Cohen%E2%80%93Sutherland_algorithm

public class LineClipper {

	private static final int INSIDE = 0; // 0000
	private static int LEFT = 1; // 0001
	private static int RIGHT = 2; // 0010
	private static int BOTTOM = 4; // 0100
	private static int TOP = 8; // 1000

	// Compute the bit code for a point (x, y) using the clip rectangle
	// bounded diagonally by (xmin, ymin), and (xmax, ymax)

	private int xmin, xmax, ymin, ymax;

	public boolean clip(Point p1, Point p2, int minx, int miny, int maxx, int maxy) {
		this.xmin = minx;
		this.ymin = miny;
		this.xmax = maxx;
		this.ymax = maxy;

		return cohenSutherlandLineClip(p1.x, p1.y, p2.x, p2.y);
	}

	private int outCode(int x, int y) {
		int code;

		code = INSIDE; // initialised as being inside of clip window

		if (x < xmin) // to the left of clip window
			code |= LEFT;
		else if (x > xmax) // to the right of clip window
			code |= RIGHT;
		if (y < ymin) // below the clip window
			code |= BOTTOM;
		else if (y > ymax) // above the clip window
			code |= TOP;

		return code;
	}

	// CohenSutherland clipping algorithm clips a line from
	// P0 = (x0, y0) to P1 = (x1, y1) against a rectangle with 
	// diagonal from (xmin, ymin) to (xmax, ymax).
	private boolean cohenSutherlandLineClip(int x0, int y0, int x1, int y1) {
		// compute outcodes for P0, P1, and whatever point lies outside the clip rectangle
		int outcode0 = outCode(x0, y0);
		int outcode1 = outCode(x1, y1);
		boolean accept = false;

		while (true) {
			if ((outcode0 | outcode1) == 0) { // Bitwise OR is 0. Trivially accept and get out of loop
				accept = true;
				break;
			} else if ((outcode0 & outcode1) != 0) { // Bitwise AND is not 0. Trivially reject and get out of loop
				break;
			} else {
				// failed both tests, so calculate the line segment to clip
				// from an outside point to an intersection with clip edge
				int x = 0;
				int y = 0;

				// At least one endpoint is outside the clip rectangle; pick it.
				int outcodeOut = (outcode0 == 0) ? outcode1 : outcode0;

				// Now find the intersection point;
				// use formulas y = y0 + slope * (x - x0), x = x0 + (1 / slope) * (y - y0)
				if ((outcodeOut & TOP) != 0) { // point is above the clip rectangle
					x = x0 + (x1 - x0) * (ymax - y0) / (y1 - y0);
					y = ymax;
				} else if ((outcodeOut & BOTTOM) != 0) { // point is below the clip rectangle
					x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
					y = ymin;
				} else if ((outcodeOut & RIGHT) != 0) { // point is to the right of clip rectangle
					y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
					x = xmax;
				} else if ((outcodeOut & LEFT) != 0) { // point is to the left of clip rectangle
					y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
					x = xmin;
				}

				// Now we move outside point to intersection point to clip
				// and get ready for next pass.
				if (outcodeOut == outcode0) {
					x0 = x;
					y0 = y;
					outcode0 = outCode(x0, y0);
				} else {
					x1 = x;
					y1 = y;
					outcode1 = outCode(x1, y1);
				}
			}
		}

		// TODO do sth with the result x0...
		return accept;
	}

}
