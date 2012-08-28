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
	int verticesCnt;
	int offset;
	short[] mVertex;

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

	private static ShortItem addTwoVertex(short[] vertex, ShortItem item) {
		ShortItem it = item;

		if (it.used + 6 >= ShortItem.SIZE) {

			if (it.used == ShortItem.SIZE) {
				it.next = ShortPool.get();
				it = it.next;

			} else {
				System.arraycopy(vertex, 0, it.vertices, it.used, 6);
				it.used += 6;

				it.next = ShortPool.get();
				it = it.next;

				System.arraycopy(vertex, 6, it.vertices, it.used, 6);
				it.used += 6;

				return it;
			}
		}

		System.arraycopy(vertex, 0, it.vertices, it.used, 12);
		it.used += 12;

		return it;
	}

	private static ShortItem addVertex(short[] vertex, ShortItem item) {
		ShortItem it = item;

		if (it.used == ShortItem.SIZE) {
			it.next = ShortPool.get();
			it = it.next;
		}

		System.arraycopy(vertex, 0, it.vertices, it.used, 6);
		it.used += 6;

		return it;
	}

	/*
	 * line extrusion is based on code from GLMap (https://github.com/olofsj/GLMap/) by olofsj -- need some way to know
	 * how the road connects to set the ending angles
	 */
	void addLine(float[] pointArray, int pos, int length) {
		float x, y, nextX, nextY, prevX, prevY, ux, uy, vx, vy, wx, wy;
		float a;
		int pointPos = pos;
		boolean rounded = false;
		boolean squared = false;

		if (line.cap == Cap.ROUND)
			rounded = true;
		else if (line.cap == Cap.SQUARE)
			squared = true;

		if (pool == null) {
			curItem = ShortPool.get();
			pool = curItem;

			mVertex = new short[12];
		}

		// amount of vertices used
		verticesCnt += length + (rounded ? 6 : 2);

		ShortItem si = curItem;

		x = pointArray[pointPos++];
		y = pointArray[pointPos++];

		nextX = pointArray[pointPos++];
		nextY = pointArray[pointPos++];

		// Calculate triangle corners for the given width
		vx = nextX - x;
		vy = nextY - y;

		a = FloatMath.sqrt(vx * vx + vy * vy);

		vx = (vx / a);
		vy = (vy / a);

		ux = -vy;
		uy = vx;

		float uxw = ux;
		float uyw = uy;

		float vxw = vx;
		float vyw = vy;
		int tsize = Tile.TILE_SIZE;

		short v[] = mVertex;

		v[0] = (short) (x * S);
		v[1] = (short) (y * S);

		boolean outside = (x <= 0 || x >= tsize || y <= 0 || y >= tsize)
				&& (x - vxw <= 0 || x - vxw >= tsize || y - vyw <= 0 || y - vyw >= tsize);

		if (rounded && !outside) {
			v[2] = (short) ((uxw - vxw) * S1000);
			v[3] = (short) ((uyw - vyw) * S1000);
			v[4] = -1;
			v[5] = 1;
			si = addVertex(v, si);
			si = addVertex(v, si);

			v[2] = (short) (-(uxw + vxw) * S1000);
			v[3] = (short) (-(uyw + vyw) * S1000);
			v[4] = 1;
			v[5] = 1;
			si = addVertex(v, si);

			// Start of line
			v[2] = (short) ((uxw) * S1000);
			v[3] = (short) ((uyw) * S1000);
			v[4] = -1;
			v[5] = 0;
			si = addVertex(v, si);

			v[2] = (short) ((-uxw) * S1000);
			v[3] = (short) ((-uyw) * S1000);
			v[4] = 1;
			v[5] = 0;
			si = addVertex(v, si);

		} else {
			// outside means line is probably clipped
			// TODO should align ending with tile boundary
			// for now, just extend the line a little

			if (squared) {
				vxw = 0;
				vyw = 0;
			} else if (!outside) {
				vxw *= 0.5;
				vyw *= 0.5;
			}

			if (rounded)
				verticesCnt -= 2;

			// Add the first point twice to be able to draw with GL_TRIANGLE_STRIP
			v[2] = (short) ((uxw - vxw) * S1000);
			v[3] = (short) ((uyw - vyw) * S1000);
			v[4] = -1;
			v[5] = 0;
			si = addVertex(v, si);
			si = addVertex(v, si);

			v[2] = (short) (-(uxw + vxw) * S1000);
			v[3] = (short) (-(uyw + vyw) * S1000);
			v[4] = 1;
			v[5] = 0;
			si = addVertex(v, si);
		}

		prevX = x;
		prevY = y;
		x = nextX;
		y = nextY;

		for (; pointPos < pos + length;) {
			nextX = pointArray[pointPos++];
			nextY = pointArray[pointPos++];

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

			uxw = ux * S1000;
			uyw = uy * S1000;

			v[6] = v[0] = (short) (x * S);
			v[7] = v[1] = (short) (y * S);

			v[2] = (short) uxw;
			v[3] = (short) uyw;
			v[4] = -1;
			v[5] = 0;

			v[8] = (short) -uxw;
			v[9] = (short) -uyw;
			v[10] = 1;
			v[11] = 0;
			si = addTwoVertex(v, si);

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

		uxw = ux;
		uyw = uy;

		vxw = vx;
		vyw = vy;

		outside = (x <= 0 || x >= tsize || y <= 0 || y >= tsize)
				&& (x - vxw <= 0 || x - vxw >= tsize || y - vyw <= 0 || y - vyw >= tsize);

		v[0] = (short) (x * S);
		v[1] = (short) (y * S);

		if (rounded && !outside) {
			v[2] = (short) ((uxw) * S1000);
			v[3] = (short) ((uyw) * S1000);
			v[4] = -1;
			v[5] = 0;
			si = addVertex(v, si);

			v[2] = (short) ((-uxw) * S1000);
			v[3] = (short) ((-uyw) * S1000);
			v[4] = 1;
			v[5] = 0;
			si = addVertex(v, si);

			// For rounded line edges
			v[2] = (short) ((uxw - vxw) * S1000);
			v[3] = (short) ((uyw - vyw) * S1000);
			v[4] = -1;
			v[5] = -1;
			si = addVertex(v, si);

			v[2] = (short) (-(uxw + vxw) * S1000);
			v[3] = (short) (-(uyw + vyw) * S1000);
			v[4] = 1;
			v[5] = -1;
			si = addVertex(v, si);
			si = addVertex(v, si);

		} else {
			if (squared) {
				vxw = 0;
				vyw = 0;
			} else if (!outside) {
				vxw *= 0.5;
				vyw *= 0.5;
			}

			if (rounded)
				verticesCnt -= 2;

			v[2] = (short) ((uxw) * S1000);
			v[3] = (short) ((uyw) * S1000);
			v[4] = -1;
			v[5] = 0;
			si = addVertex(v, si);

			v[2] = (short) (-(uxw + vxw) * S1000);
			v[3] = (short) (-(uyw + vyw) * S1000);
			v[4] = 1;
			v[5] = 0;
			si = addVertex(v, si);
			si = addVertex(v, si);
		}

		curItem = si;
	}
}
