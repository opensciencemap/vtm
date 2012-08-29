/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.glrenderer;

import org.mapsforge.android.rendertheme.renderinstruction.Line;
import org.mapsforge.core.Tile;

import android.graphics.Paint.Cap;
import android.util.FloatMath;

class LineLayer {

	private static final float S = MapRenderer.COORD_MULTIPLIER;
	private static final float S1000 = 1000;

	LineLayer next;
	LineLayer outlines;

	Line line;
	float width;
	boolean isOutline;
	int layer;

	ShortItem pool;
	protected ShortItem curItem;

	// number of vertices this layer holds
	int verticesCnt;
	// vertices offset of this layer in VBO
	int offset;

	LineLayer(int layer, Line line, float width, boolean outline) {
		this.layer = layer;
		this.width = width;
		this.line = line;
		this.isOutline = outline;
	}

	void addOutline(LineLayer link) {
		for (LineLayer l = outlines; l != null; l = l.outlines)
			if (link == l)
				return;

		link.outlines = outlines;
		outlines = link;
	}

	/*
	 * line extrusion is based on code from GLMap (https://github.com/olofsj/GLMap/) by olofsj -- need some way to know
	 * how the road connects to set the ending angles
	 */
	void addLine(float[] points, int pos, int length) {
		float x, y, nextX, nextY, prevX, prevY, ux, uy, vx, vy, wx, wy;
		float a;
		int ipos = pos;
		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		// amount of vertices used
		verticesCnt += length + (rounded ? 6 : 2);

		x = points[ipos++];
		y = points[ipos++];

		nextX = points[ipos++];
		nextY = points[ipos++];

		// Calculate triangle corners for the given width
		vx = nextX - x;
		vy = nextY - y;

		a = FloatMath.sqrt(vx * vx + vy * vy);

		vx = (vx / a);
		vy = (vy / a);

		ux = -vy;
		uy = vx;

		int tsize = Tile.TILE_SIZE;

		if (pool == null) {
			pool = curItem = ShortPool.get();
		}

		ShortItem si = curItem;
		short v[] = si.vertices;
		int opos = si.used;

		if (opos == ShortItem.SIZE) {
			si.used = ShortItem.SIZE;
			si.next = ShortPool.get();
			si = si.next;
			opos = 0;
			v = si.vertices;
		}

		boolean outside = (x <= 0 || x >= tsize || y <= 0 || y >= tsize)
				&& (x - vx <= 0 || x - vx >= tsize || y - vy <= 0 || y - vy >= tsize);

		short ox, oy, dx, dy;

		ox = (short) (x * S);
		oy = (short) (y * S);

		if (rounded && !outside) {

			// For rounded line edges
			dx = (short) ((ux - vx) * S1000);
			dy = (short) ((uy - vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 1;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 1;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			dx = (short) (-(ux + vx) * S1000);
			dy = (short) (-(uy + vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = 1;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			// Start of line
			dx = (short) (ux * S1000);
			dy = (short) (uy * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = (short) (-dx);
			v[opos + 3] = (short) (-dy);
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;

		} else {
			// outside means line is probably clipped
			// TODO should align ending with tile boundary
			// for now, just extend the line a little

			if (squared) {
				vx = 0;
				vy = 0;
			} else if (!outside) {
				vx *= 0.5;
				vy *= 0.5;
			}

			if (rounded)
				verticesCnt -= 2;

			dx = (short) ((ux - vx) * S1000);
			dy = (short) ((uy - vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			dx = (short) (-(ux + vx) * S1000);
			dy = (short) (-(uy + vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;
		}

		prevX = x;
		prevY = y;
		x = nextX;
		y = nextY;

		for (; ipos < pos + length;) {
			nextX = points[ipos++];
			nextY = points[ipos++];

			// Unit vector pointing back to previous node
			vx = prevX - x;
			vy = prevY - y;
			a = FloatMath.sqrt(vx * vx + vy * vy);
			vx = (vx / a);
			vy = (vy / a);

			// Unit vector pointing forward to next node
			wx = nextX - x;
			wy = nextY - y;
			a = FloatMath.sqrt(wx * wx + wy * wy);
			wx = (wx / a);
			wy = (wy / a);

			// Sum of these two vectors points
			ux = vx + wx;
			uy = vy + wy;

			a = -wy * ux + wx * uy;

			// boolean split = false;
			if (a < 0.1f && a > -0.1f) {
				// Almost straight or miter goes to infinity, use normal vector
				ux = -wy;
				uy = wx;
			} else {
				ux = (ux / a);
				uy = (uy / a);

				if (ux > 2.0f || ux < -2.0f || uy > 2.0f || uy < -2.0f) {
					ux = -wy;
					uy = wx;
				}
			}

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			ox = (short) (x * S);
			oy = (short) (y * S);

			dx = (short) (ux * S1000);
			dy = (short) (uy * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = (short) -dx;
			v[opos + 3] = (short) -dy;
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;

			prevX = x;
			prevY = y;
			x = nextX;
			y = nextY;
		}

		vx = prevX - x;
		vy = prevY - y;

		a = FloatMath.sqrt(vx * vx + vy * vy);

		vx = (vx / a);
		vy = (vy / a);

		ux = vy;
		uy = -vx;

		outside = (x <= 0 || x >= tsize || y <= 0 || y >= tsize)
				&& (x - vx <= 0 || x - vx >= tsize || y - vy <= 0 || y - vy >= tsize);

		if (opos == ShortItem.SIZE) {
			si.used = ShortItem.SIZE;
			si.next = ShortPool.get();
			si = si.next;
			opos = 0;
			v = si.vertices;
		}

		ox = (short) (x * S);
		oy = (short) (y * S);

		if (rounded && !outside) {

			dx = (short) (ux * S1000);
			dy = (short) (uy * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = (short) -dx;
			v[opos + 3] = (short) -dy;
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			// For rounded line edges
			dx = (short) ((ux - vx) * S1000);
			dy = (short) ((uy - vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = -1;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			dx = (short) (-(ux + vx) * S1000);
			dy = (short) (-(uy + vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = -1;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = -1;
			opos += 6;

		} else {
			if (squared) {
				vx = 0;
				vy = 0;
			} else if (!outside) {
				vx *= 0.5;
				vy *= 0.5;
			}

			if (rounded)
				verticesCnt -= 2;

			dx = (short) ((ux - vx) * S1000);
			dy = (short) ((uy - vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = -1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			dx = (short) (-(ux + vx) * S1000);
			dy = (short) (-(uy + vy) * S1000);

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;

			if (opos == ShortItem.SIZE) {
				si.used = ShortItem.SIZE;
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			v[opos + 0] = ox;
			v[opos + 1] = oy;
			v[opos + 2] = dx;
			v[opos + 3] = dy;
			v[opos + 4] = 1;
			v[opos + 5] = 0;
			opos += 6;
		}

		si.used = opos;
		curItem = si;
	}
}
