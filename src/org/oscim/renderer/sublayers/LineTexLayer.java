/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer.sublayers;

import java.nio.ShortBuffer;

import org.oscim.renderer.GLRenderer;
import org.oscim.theme.renderinstruction.Line;

/**
 * Layer for textured or stippled lines
 *
 * this would be all so much simpler with geometry shaders...
 */
public final class LineTexLayer extends Layer {
	// Interleave two segment quads in one block to be able to use
	// vertices twice. pos0 and pos1 use the same vertex array where
	// pos1 has an offset of one vertex. The vertex shader will use
	// pos0 when the vertexId is even, pos1 when the Id is odd.
	//
	// As there is no gl_VertexId in gles 2.0 an additional 'flip'
	// array is used. Depending on 'flip' extrusion is inverted.
	//
	// Indices and flip buffers can be static.
	//
	// First pass: using even vertex array positions
	//   (used vertices are in braces)
	// vertex id   0  1  2  3  4  5  6  7
	// pos0     x (0) 1 (2) 3 (4) 5 (6) 7 x
	// pos1        x (0) 1 (2) 3 (4) 5 (6) 7 x
	// flip        0  1  0  1  0  1  0  1
	//
	// Second pass: using odd vertex array positions
	// vertex id   0  1  2  3  4  5  6  7
	// pos0   x 0 (1) 2 (3) 4 (5) 6 (7) x
	// pos1      x 0 (1) 2 (3) 4 (5) 6 (7) x
	// flip        0  1  0  1  0  1  0  1
	//
	// Vertex layout:
	// [2 short] position,
	// [2 short] extrusion,
	// [1 short] line length
	// [1 short] unused
	//
	// indices, for two blocks:
	// 0, 1, 2,
	// 2, 1, 3,
	// 4, 5, 6,
	// 6, 5, 7,
	//
	// BIG NOTE: renderer assumes to be able to offset vertex array position
	// so that in the first pass 'pos1' offset will be < 0 if no data precedes
	// - in our case there is always the polygon fill array at start
	// - see addLine hack otherwise.

	private static final float COORD_SCALE = GLRenderer.COORD_SCALE;
	// scale factor mapping extrusion vector to short values
	public static final float DIR_SCALE = 2048;

	// lines referenced by this outline layer
	public LineLayer outlines;
	public Line line;
	public float width;

	public boolean roundCap;

	public int evenQuads;
	public int oddQuads;

	private boolean evenSegment;

	LineTexLayer(int layer) {
		this.level = layer;
		this.type = Layer.TEXLINE;
		this.evenSegment = true;
	}

	public void addLine(float[] points, short[] index) {

		if (vertexItems == null) {
			curItem = vertexItems = VertexItem.pool.get();

			// HACK add one vertex offset when compiling
			// buffer otherwise one cant use the full
			// VertexItem (see Layers.compile)
			// add the two 'x' at front and end
			//verticesCnt = 2;

			// the additional end vertex to make sure
			// not to read outside allocated memory
			verticesCnt = 1;
		}

		VertexItem si = curItem;

		short v[] = si.vertices;
		int opos = si.used;

		boolean even = evenSegment;

		// reset offset to last written position
		if (!even)
			opos -= 12;

		int n;
		int length = 0;

		if (index == null) {
			n = 1;
			length = points.length;
		} else {
			n = index.length;
		}

		for (int i = 0, pos = 0; i < n; i++) {
			if (index != null)
				length = index[i];

			// check end-marker in indices
			if (length < 0)
				break;

			// need at least two points
			if (length < 4) {
				pos += length;
				continue;
			}

			int ipos = pos;

			float x = points[ipos++] * COORD_SCALE;
			float y = points[ipos++] * COORD_SCALE;

			// randomize a bit
			float lineLength = (x * x + y * y) % 80;

			int end = pos + length;

			for (; ipos < end;) {
				float nx = points[ipos++] * COORD_SCALE;
				float ny = points[ipos++] * COORD_SCALE;

				// Calculate triangle corners for the given width
				float vx = nx - x;
				float vy = ny - y;

				float a = (float) Math.sqrt(vx * vx + vy * vy);

				// normal vector
				vx /= a;
				vy /= a;

				// perpendicular to line segment
				float ux = -vy;
				float uy = vx;

				short dx = (short) (ux * DIR_SCALE);
				short dy = (short) (uy * DIR_SCALE);

				if (opos == VertexItem.SIZE) {
					si = si.next = VertexItem.pool.get();
					v = si.vertices;
					opos = 0;
				}

				v[opos + 0] = (short) x;
				v[opos + 1] = (short) y;
				v[opos + 2] = dx;
				v[opos + 3] = dy;
				v[opos + 4] = (short) lineLength;
				v[opos + 5] = 0;

				lineLength += a;
				v[opos + 12] = (short) nx;
				v[opos + 13] = (short) ny;
				v[opos + 14] = dx;
				v[opos + 15] = dy;
				v[opos + 16] = (short) lineLength;
				v[opos + 17] = 0;

				x = nx;
				y = ny;

				if (even) {
					// go to second segment
					opos += 6;
					even = false;

					// vertex 0 and 2 were added
					verticesCnt += 3;
					evenQuads++;
				} else {
					// go to next block
					even = true;
					opos += 18;

					// vertex 1 and 3 were added
					verticesCnt += 1;
					oddQuads++;
				}
			}

			pos += length;
		}

		evenSegment = even;

		// advance offset to last written position
		if (!even)
			opos += 12;

		si.used = opos;
		curItem = si;
	}

	@Override
	protected void clear() {
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
	}

}
