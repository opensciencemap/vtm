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

import android.util.FloatMath;

class LineLayer {

	private static final float SCALE_FACTOR = 16;

	Line line;

	LineLayer next;
	LineLayer outlines;

	float width;
	boolean isOutline;

	ShortItem pool;
	protected ShortItem curItem;
	int verticesCnt;
	int offset;

	final int layer;

	LineLayer(int layer, Line line, boolean outline) {
		this.layer = layer;

		this.line = line;
		this.isOutline = outline;

		if (!outline) {
			curItem = ShortPool.get();
			pool = curItem;
		}
	}

	void addOutline(LineLayer link) {
		for (LineLayer l = outlines; l != null; l = l.outlines)
			if (link == l)
				return;

		link.outlines = outlines;
		outlines = link;
	}

	short[] getNextItem() {
		curItem.used = ShortItem.SIZE;

		curItem.next = ShortPool.get();
		curItem = curItem.next;

		return curItem.vertices;
	}

	/*
	 * line extrusion is based on code from GLMap (https://github.com/olofsj/GLMap/) by olofsj
	 */
	void addLine(float[] pointArray, int pos, int length, float w, boolean capRound) {
		float x, y, nextX, nextY, prevX, prevY, ux, uy, vx, vy, wx, wy;
		float a;
		int pointPos = pos;
		boolean rounded = capRound;
		width = w;// * SCALE_FACTOR;
		if (w < 0.5)
			rounded = false;

		// amount of vertices used
		verticesCnt += length + (rounded ? 6 : 2);

		int MAX = PoolItem.SIZE;

		short[] curVertices = curItem.vertices;
		int vertexPos = curItem.used;

		if (vertexPos == MAX) {
			curVertices = getNextItem();
			vertexPos = 0;
		}

		x = pointArray[pointPos++];// * SCALE_FACTOR;
		y = pointArray[pointPos++];// * SCALE_FACTOR;

		nextX = pointArray[pointPos++];// * SCALE_FACTOR;
		nextY = pointArray[pointPos++];// * SCALE_FACTOR;

		// Calculate triangle corners for the given width
		vx = nextX - x;
		vy = nextY - y;

		a = FloatMath.sqrt(vx * vx + vy * vy);

		vx = (vx / a);
		vy = (vy / a);

		ux = -vy;
		uy = vx;

		float uxw = ux * w;
		float uyw = uy * w;

		float vxw = vx * w;
		float vyw = vy * w;

		// boolean outside = (x <= 0 || x >= Tile.TILE_SIZE || y <= 0 || y >= Tile.TILE_SIZE)
		// && (x - vxw <= 0 || x - vxw >= Tile.TILE_SIZE || y - vyw <= 0 || y - vyw >= Tile.TILE_SIZE);

		boolean outside = false;
		if (rounded && !outside) {

			// Add the first point twice to be able to draw with GL_TRIANGLE_STRIP

			curVertices[vertexPos++] = (short) ((x + uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 1;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x + uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 1;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 1;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Start of line
			curVertices[vertexPos++] = (short) ((x + uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;

		} else {
			// outside means line is probably clipped
			// TODO should align ending with tile boundary
			// for now, just extend the line a little
			if (!outside) {
				vxw *= 0.5;
				vyw *= 0.5;
			}
			if (rounded) {
				verticesCnt -= 2;
			}
			// Add the first point twice to be able to draw with GL_TRIANGLE_STRIP
			curVertices[vertexPos++] = (short) ((x + uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x + uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;
		}

		prevX = x;
		prevY = y;
		x = nextX;
		y = nextY;
		// boolean flipped = false;

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

			if ((a < 0.1 && a > -0.1)) {
				// Almost straight, use normal vector
				ux = -wy;
				uy = wx;
			} else {
				ux = (ux / a);
				uy = (uy / a);

				if (ux > 2 || uy > 2 || ux < -2 || uy < -2) {
					ux = -wy;
					uy = wx;

					// ux = vx + wx;
					// uy = vy + wy;
					// // Normalize u, and project normal vector onto this
					// double c = Math.sqrt(ux * ux + uy * uy);
					// if (a < 0) {
					// ux = (float) -(ux / c);
					// uy = (float) -(uy / c);
					// }
					// else {
					// ux = (float) (ux / c);
					// uy = (float) (uy / c);
					// }
					// flipped = flipped ? false : true;
				}
			}

			uxw = ux * w;
			uyw = uy * w;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x + uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;

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

		uxw = ux * w;
		uyw = uy * w;

		vxw = vx * w;
		vyw = vy * w;

		// outside = (x <= 0 || x >= Tile.TILE_SIZE || y <= 0 || y >= Tile.TILE_SIZE)
		// && (x - vxw <= 0 || x - vxw >= Tile.TILE_SIZE || y - vyw <= 0 || y - vyw >= Tile.TILE_SIZE);

		if (vertexPos == MAX) {
			curVertices = getNextItem();
			vertexPos = 0;
		}

		if (rounded && !outside) {
			curVertices[vertexPos++] = (short) ((x + uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// For rounded line edges
			curVertices[vertexPos++] = (short) ((x + uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = -1;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Add the last vertex twice to be able to draw with GL_TRIANGLE_STRIP
			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = -1;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = -1;

		} else {
			if (!outside) {
				vxw *= 0.5;
				vyw *= 0.5;
			}
			if (rounded) {
				verticesCnt -= 2;
			}

			curVertices[vertexPos++] = (short) ((x + uxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y + uyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = -1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Add the last vertex twice to be able to draw with GL_TRIANGLE_STRIP
			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;

			if (vertexPos == MAX) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = (short) ((x - uxw - vxw) * SCALE_FACTOR);
			curVertices[vertexPos++] = (short) ((y - uyw - vyw) * SCALE_FACTOR);
			curVertices[vertexPos++] = 1;
			curVertices[vertexPos++] = 0;

		}

		curItem.used = vertexPos;
	}
}
