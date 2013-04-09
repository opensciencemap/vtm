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
package org.oscim.renderer.layer;

import java.nio.ShortBuffer;

import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.theme.renderinstruction.Area;

public final class PolygonLayer extends Layer {
	private static final float S = GLRenderer.COORD_SCALE;

	public Area area;

	PolygonLayer(int layer) {
		this.level = layer;
		this.type = Layer.POLYGON;
		curItem = VertexItem.pool.get();
		vertexItems = curItem;
	}

	public void addPolygon(float[] points, short[] index) {
		short center = (short) ((Tile.SIZE >> 1) * S);

		VertexItem si = curItem;
		short[] v = si.vertices;
		int outPos = si.used;

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];
			if (length < 0)
				break;

			// need at least three points
			if (length < 6) {
				pos += length;
				continue;
			}

			verticesCnt += length / 2 + 2;

			int inPos = pos;

			if (outPos == VertexItem.SIZE) {
				si = si.next = VertexItem.pool.get();
				v = si.vertices;
				outPos = 0;
			}

			v[outPos++] = center;
			v[outPos++] = center;

			for (int j = 0; j < length; j += 2) {
				if (outPos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					outPos = 0;
				}
				v[outPos++] = (short) (points[inPos++] * S);
				v[outPos++] = (short) (points[inPos++] * S);
			}

			if (outPos == VertexItem.SIZE) {
				si = si.next = VertexItem.pool.get();
				v = si.vertices;
				outPos = 0;
			}

			v[outPos++] = (short) (points[pos + 0] * S);
			v[outPos++] = (short) (points[pos + 1] * S);

			pos += length;
		}

		si.used = outPos;
		curItem = si;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
	}

	@Override
	protected void clear() {
	}
}
