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

/**
 * from http://en.wikipedia.org/wiki/Cohen%E2%80%93
 * Sutherland_algorithm
 * 
 * @adapted by Hannes Janetzek
 */

public class LineClipper {

	private static final int INSIDE = 0; // 0000
	private static final int LEFT = 1; // 0001
	private static final int RIGHT = 2; // 0010
	private static final int BOTTOM = 4; // 0100
	private static final int TOP = 8; // 1000

	private final int xmin, xmax, ymin, ymax;
	public final int[] out;

	public LineClipper(int minx, int miny, int maxx, int maxy) {
		this.xmin = minx;
		this.ymin = miny;
		this.xmax = maxx;
		this.ymax = maxy;
		this.out = null;
	}

	public LineClipper(int minx, int miny, int maxx, int maxy, boolean keepResult) {
		this.xmin = minx;
		this.ymin = miny;
		this.xmax = maxx;
		this.ymax = maxy;
		if (keepResult)
			this.out = new int[4];
		else
			this.out = null;
	}

	private int mPrevOutcode;
	private int mPrevX;
	private int mPrevY;

	public int outX;
	public int outY;

	public void clipStart(int x0, int y0) {
		mPrevX = x0;
		mPrevY = y0;

		int outcode = INSIDE;
		if (x0 < xmin)
			outcode |= LEFT;
		else if (x0 > xmax)
			outcode |= RIGHT;
		if (y0 < ymin)
			outcode |= BOTTOM;
		else if (y0 > ymax)
			outcode |= TOP;

		mPrevOutcode = outcode;
	}

	/**
	 * @param x1 ...
	 * @param y1 ...
	 * @return 0 if not intersection, 1 fully within, -1 clipped (and 'out' set
	 *         to new points)
	 */
	public int clipNext(int x1, int y1) {
		int accept;

		int outcode = INSIDE;
		if (x1 < xmin)
			outcode |= LEFT;
		else if (x1 > xmax)
			outcode |= RIGHT;
		if (y1 < ymin)
			outcode |= BOTTOM;
		else if (y1 > ymax)
			outcode |= TOP;

		if ((mPrevOutcode | outcode) == 0) {
			// Bitwise OR is 0. Trivially accept
			accept = 1;
		} else if ((mPrevOutcode & outcode) != 0) {
			// Bitwise AND is not 0. Trivially reject
			accept = 0;
		} else {
			accept = clip(mPrevX, mPrevY, x1, y1, xmin, ymin, xmax, ymax, mPrevOutcode, outcode,
			              this.out) ? -1 : 0;
		}
		mPrevOutcode = outcode;
		mPrevX = x1;
		mPrevY = y1;

		return accept;
	}

	// CohenSutherland clipping algorithm clips a line from
	// P0 = (x0, y0) to P1 = (x1, y1) against a rectangle with
	// diagonal from (xmin, ymin) to (xmax, ymax).
	private static boolean clip(int x0, int y0, int x1, int y1,
	        int xmin, int ymin, int xmax, int ymax, int outcode0, int outcode1, int[] out) {

		boolean accept = false;

		while (true) {
			if ((outcode0 | outcode1) == 0) {
				// Bitwise OR is 0. Trivially accept and get out of loop
				accept = true;
				break;
			} else if ((outcode0 & outcode1) != 0) {
				// Bitwise AND is not 0. Trivially reject and get out of loop
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
				} else if ((outcodeOut & BOTTOM) != 0) {
					// point is below the clip rectangle
					x = x0 + (x1 - x0) * (ymin - y0) / (y1 - y0);
					y = ymin;
				} else if ((outcodeOut & RIGHT) != 0) {
					// point is to the right of clip rectangle
					y = y0 + (y1 - y0) * (xmax - x0) / (x1 - x0);
					x = xmax;
				} else if ((outcodeOut & LEFT) != 0) {
					// point is to the left of clip rectangle
					y = y0 + (y1 - y0) * (xmin - x0) / (x1 - x0);
					x = xmin;
				}

				int outcode = INSIDE;
				if (x < xmin)
					outcode |= LEFT;
				else if (x > xmax)
					outcode |= RIGHT;
				if (y < ymin)
					outcode |= BOTTOM;
				else if (y > ymax)
					outcode |= TOP;

				// Now we move outside point to intersection point to clip
				// and get ready for next pass.
				if (outcodeOut == outcode0) {
					x0 = x;
					y0 = y;
					outcode0 = outcode;
				} else {
					x1 = x;
					y1 = y;
					outcode1 = outcode;
				}
			}
		}
		if (accept && out != null) {
			out[0] = x0;
			out[1] = y0;
			out[2] = x1;
			out[3] = y1;
		}
		return accept;
	}
}
