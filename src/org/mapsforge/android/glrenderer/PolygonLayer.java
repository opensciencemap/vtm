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

import java.util.LinkedList;

class PolygonLayer extends Layer {
	int fadeLevel;
	private boolean first = true;
	private float originX;
	private float originY;

	PolygonLayer(int layer, int color, int fade) {
		super(layer, color);
		fadeLevel = fade;
		curItem = LayerPool.get();
		pool = new LinkedList<PoolItem>();
		pool.add(curItem);
	}

	void addPolygon(float[] points, int pos, int length) {

		verticesCnt += length / 2 + 2;

		if (first) {
			first = false;
			originX = points[pos];
			originY = points[pos + 1];
		}

		float[] curVertices = curItem.vertices;
		int outPos = curItem.used;

		if (outPos == PoolItem.SIZE) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = originX;
		curVertices[outPos++] = originY;

		int remaining = length;
		int inPos = pos;
		while (remaining > 0) {

			if (outPos == PoolItem.SIZE) {
				curVertices = getNextItem();
				outPos = 0;
			}

			int len = remaining;
			if (len > (PoolItem.SIZE) - outPos)
				len = (PoolItem.SIZE) - outPos;

			System.arraycopy(points, inPos, curVertices, outPos, len);
			outPos += len;
			inPos += len;
			remaining -= len;
		}

		if (outPos == PoolItem.SIZE) {
			curVertices = getNextItem();
			outPos = 0;
		}

		curVertices[outPos++] = points[pos + 0];
		curVertices[outPos++] = points[pos + 1];

		curItem.used = outPos;
	}
}
