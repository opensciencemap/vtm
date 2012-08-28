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

import org.mapsforge.android.rendertheme.renderinstruction.Area;
import org.mapsforge.core.Tile;

class PolygonLayer {
	private static final float S = MapRenderer.COORD_MULTIPLIER;

	PolygonLayer next;
	Area area;
	// private static final float MapRenderer.COORD_MULTIPLIER = 8.0f;
	private boolean first = true;
	private float originX;
	private float originY;

	ShortItem pool;
	protected ShortItem curItem;
	int verticesCnt;
	int offset;

	final int layer;

	PolygonLayer(int layer, Area area) {
		this.layer = layer;
		this.area = area;
		curItem = ShortPool.get();
		pool = curItem;
	}

	short[] getNextItem() {
		curItem.used = ShortItem.SIZE;

		curItem.next = ShortPool.get();
		curItem = curItem.next;

		return curItem.vertices;
	}

	void addPolygon(float[] points, int pos, int length) {

		verticesCnt += length / 2 + 2;

		if (first) {
			first = false;
			originX = Tile.TILE_SIZE >> 1; // points[pos];
			originY = Tile.TILE_SIZE >> 1; // points[pos + 1];
		}

		short[] curVertices = curItem.vertices;
		int outPos = curItem.used;

		if (outPos == ShortItem.SIZE) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = (short) (originX * S);
		curVertices[outPos++] = (short) (originY * S);

		int MAX = ShortItem.SIZE;
		int remaining = length;
		int inPos = pos;
		while (remaining > 0) {

			if (outPos == MAX) {
				curVertices = getNextItem();
				outPos = 0;
			}

			int len = remaining;
			if (len > MAX - outPos)
				len = MAX - outPos;

			for (int i = 0; i < len; i++)
				curVertices[outPos++] = (short) (points[inPos++] * S);

			// System.arraycopy(points, inPos, curVertices, outPos, len);

			// outPos += len;
			// inPos += len;

			remaining -= len;
		}

		if (outPos == MAX) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = (short) (points[pos + 0] * S);
		curVertices[outPos++] = (short) (points[pos + 1] * S);

		curItem.used = outPos;
	}
}
