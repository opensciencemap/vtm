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

import java.util.ArrayList;
import java.util.LinkedList;

import org.mapsforge.core.Tile;

class LineLayer extends Layer {
	ArrayList<LineLayer> outlines;
	boolean isOutline;
	boolean isFixed;
	float width;

	LineLayer(int layer, int color, boolean outline, boolean fixed) {
		super(layer, color);
		isOutline = outline;
		isFixed = fixed;
		if (outline) {
			outlines = new ArrayList<LineLayer>();
		} else {
			curItem = LayerPool.get();

			pool = new LinkedList<PoolItem>();
			pool.add(curItem);
		}
	}

	void addOutline(LineLayer link) {
		if (!outlines.contains(link))
			outlines.add(link);
	}

	// private void addVertex(float x, float y, byte tex, byte[] c){
	// //
	// }

	void addLine(float[] pointArray, int pos, int length, float w, boolean capRound) {
		float x, y, nextX, nextY, prevX, prevY, ux, uy, vx, vy, wx, wy;
		double a;
		int pointPos = pos;
		boolean rounded = capRound;
		width = w;
		if (w < 0.5)
			rounded = false;

		// amount of vertices used
		verticesCnt += length + (rounded ? 6 : 2);

		float[] curVertices = curItem.vertices;
		int vertexPos = curItem.used;

		if (vertexPos == PoolItem.SIZE) {
			curVertices = getNextItem();
			vertexPos = 0;
		}

		x = pointArray[pointPos++];
		y = pointArray[pointPos++];

		nextX = pointArray[pointPos++];
		nextY = pointArray[pointPos++];

		// Calculate triangle corners for the given width
		vx = nextX - x;
		vy = nextY - y;

		a = Math.sqrt(vx * vx + vy * vy);

		vx = (float) (vx / a);
		vy = (float) (vy / a);

		ux = -vy;
		uy = vx;

		float uxw = ux * w;
		float uyw = uy * w;

		float vxw = vx * w;
		float vyw = vy * w;

		boolean outside = (x <= 0 || x >= Tile.TILE_SIZE || y <= 0 || y >= Tile.TILE_SIZE)
				&& (x - vxw <= 0 || x - vxw >= Tile.TILE_SIZE || y - vyw <= 0 || y - vyw >= Tile.TILE_SIZE);

		if (rounded && !outside) {

			// Add the first point twice to be able to draw with GL_TRIANGLE_STRIP

			curVertices[vertexPos++] = x + uxw - vxw;
			curVertices[vertexPos++] = y + uyw - vyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 1.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x + uxw - vxw;
			curVertices[vertexPos++] = y + uyw - vyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 1.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 1.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Start of line
			curVertices[vertexPos++] = x + uxw;
			curVertices[vertexPos++] = y + uyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw;
			curVertices[vertexPos++] = y - uyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;

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
			curVertices[vertexPos++] = x + uxw - vxw;
			curVertices[vertexPos++] = y + uyw - vyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x + uxw - vxw;
			curVertices[vertexPos++] = y + uyw - vyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;
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
			a = Math.sqrt(vx * vx + vy * vy);
			vx = (float) (vx / a);
			vy = (float) (vy / a);

			// Unit vector pointing forward to next node
			wx = nextX - x;
			wy = nextY - y;
			a = Math.sqrt(wx * wx + wy * wy);
			wx = (float) (wx / a);
			wy = (float) (wy / a);

			// Sum of these two vectors points
			ux = vx + wx;
			uy = vy + wy;
			a = -wy * ux + wx * uy;

			if ((a < 0.1 && a > -0.1)) {
				// Almost straight, use normal vector
				ux = -wy;
				uy = wx;
			} else {
				ux = (float) (ux / a);
				uy = (float) (uy / a);

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

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x + uxw;
			curVertices[vertexPos++] = y + uyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw;
			curVertices[vertexPos++] = y - uyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;

			prevX = x;
			prevY = y;
			x = nextX;
			y = nextY;
		}

		vx = prevX - x;
		vy = prevY - y;

		a = Math.sqrt(vx * vx + vy * vy);

		vx = (float) (vx / a);
		vy = (float) (vy / a);

		ux = vy;
		uy = -vx;

		uxw = ux * w;
		uyw = uy * w;

		vxw = vx * w;
		vyw = vy * w;

		outside = (x <= 0 || x >= Tile.TILE_SIZE || y <= 0 || y >= Tile.TILE_SIZE)
				&& (x - vxw <= 0 || x - vxw >= Tile.TILE_SIZE || y - vyw <= 0 || y - vyw >= Tile.TILE_SIZE);

		if (vertexPos == PoolItem.SIZE) {
			curItem.used = vertexPos;
			curItem = LayerPool.get();
			pool.add(curItem);
			curVertices = curItem.vertices;
			vertexPos = 0;
		}

		if (rounded && !outside) {
			curVertices[vertexPos++] = x + uxw;
			curVertices[vertexPos++] = y + uyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw;
			curVertices[vertexPos++] = y - uyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// For rounded line edges
			curVertices[vertexPos++] = x + uxw - vxw;
			curVertices[vertexPos++] = y + uyw - vyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = -1.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Add the last vertex twice to be able to draw with GL_TRIANGLE_STRIP
			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = -1.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = -1.0f;

		} else {
			if (!outside) {
				vxw *= 0.5;
				vyw *= 0.5;
			}
			if (rounded) {
				verticesCnt -= 2;
			}

			curVertices[vertexPos++] = x + uxw;
			curVertices[vertexPos++] = y + uyw;
			curVertices[vertexPos++] = -1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			// Add the last vertex twice to be able to draw with GL_TRIANGLE_STRIP
			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;

			if (vertexPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				vertexPos = 0;
			}

			curVertices[vertexPos++] = x - uxw - vxw;
			curVertices[vertexPos++] = y - uyw - vyw;
			curVertices[vertexPos++] = 1.0f;
			curVertices[vertexPos++] = 0.0f;

		}

		curItem.used = vertexPos;
	}
}
