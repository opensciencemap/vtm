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

	private static final float COORD_SCALE = MapRenderer.COORD_MULTIPLIER;
	// scale factor mapping extrusion vector to short values
	private static final float DIR_SCALE = 2048;
	// mask for packing last two bits of extrusion vector with texture coordinates
	private static final int DIR_MASK = 0xFFFFFFFC;

	// next layer
	LineLayer next;

	// lines referenced by this outline layer
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
	 * line extrusion is based on code from GLMap (https://github.com/olofsj/GLMap/) by olofsj
	 */

	void addLine(float[] points, short[] index) {
		float x, y, nextX, nextY, prevX, prevY;
		float a, ux, uy, vx, vy, wx, wy;

		int tmax = Tile.TILE_SIZE + 10;
		int tmin = -10;

		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		if (pool == null) {
			pool = curItem = ShortPool.get();
		}

		ShortItem si = curItem;
		short v[] = si.vertices;
		int opos = si.used;

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];
			if (length < 0)
				break;

			// save some vertices
			if (rounded && i > 200)
				rounded = false;

			int ipos = pos;

			// need at least two points
			if (length < 4) {
				pos += length;
				continue;
			}
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

			if (opos == ShortItem.SIZE) {
				si = si.next = ShortPool.get();
				v = si.vertices;
				opos = 0;
			}

			short ox, oy, dx, dy;
			int ddx, ddy;

			ox = (short) (x * COORD_SCALE);
			oy = (short) (y * COORD_SCALE);

			boolean outside = (x < tmin || x > tmax || y < tmin || y > tmax);

			if (opos == ShortItem.SIZE) {
				si = si.next = ShortPool.get();
				v = si.vertices;
				opos = 0;
			}

			if (rounded && !outside) {
				// add first vertex twice
				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);
				dx = (short) (0 | ddx & DIR_MASK);
				dy = (short) (2 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | ddx & DIR_MASK);
				v[opos++] = (short) (2 | ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				// Start of line
				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

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

				// add first vertex twice
				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);
				dx = (short) (0 | ddx & DIR_MASK);
				dy = (short) (1 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

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
				if (a < 0.01f && a > -0.01f) {
					// Almost straight
					ux = -wy;
					uy = wx;
				} else {
					ux = (ux / a);
					uy = (uy / a);

					// hack to avoid miter going to infinity
					if (ux > 2.0f || ux < -2.0f || uy > 2.0f || uy < -2.0f) {
						ux = -wy;
						uy = wx;
					}
				}

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				ox = (short) (x * COORD_SCALE);
				oy = (short) (y * COORD_SCALE);

				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

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

			outside = (x < tmin || x > tmax || y < tmin || y > tmax);

			if (opos == ShortItem.SIZE) {
				si.next = ShortPool.get();
				si = si.next;
				opos = 0;
				v = si.vertices;
			}

			ox = (short) (x * COORD_SCALE);
			oy = (short) (y * COORD_SCALE);

			if (rounded && !outside) {
				ddx = (int) (ux * DIR_SCALE);
				ddy = (int) (uy * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (2 | -ddx & DIR_MASK);
				v[opos++] = (short) (1 | -ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				// For rounded line edges
				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);
				dx = (short) (0 | ddx & DIR_MASK);
				dy = (short) (0 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				// add last vertex twice
				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);
				dx = (short) (2 | ddx & DIR_MASK);
				dy = (short) (0 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

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

				ddx = (int) ((ux - vx) * DIR_SCALE);
				ddy = (int) ((uy - vy) * DIR_SCALE);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = (short) (0 | ddx & DIR_MASK);
				v[opos++] = (short) (1 | ddy & DIR_MASK);

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				// add last vertex twice
				ddx = (int) (-(ux + vx) * DIR_SCALE);
				ddy = (int) (-(uy + vy) * DIR_SCALE);
				dx = (short) (2 | ddx & DIR_MASK);
				dy = (short) (1 | ddy & DIR_MASK);

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;

				if (opos == ShortItem.SIZE) {
					si = si.next = ShortPool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos++] = ox;
				v[opos++] = oy;
				v[opos++] = dx;
				v[opos++] = dy;
			}
			pos += length;
		}

		si.used = opos;
		curItem = si;
	}
}
