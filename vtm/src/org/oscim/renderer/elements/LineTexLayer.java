/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.renderer.elements;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.core.GeometryBuffer;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RenderElement for textured or stippled lines
 * 
 * Interleave two segment quads in one block to be able to use
 * vertices twice. pos0 and pos1 use the same vertex array where
 * pos1 has an offset of one vertex. The vertex shader will use
 * pos0 when the vertexId is even, pos1 when the Id is odd.
 * 
 * As there is no gl_VertexId in gles 2.0 an additional 'flip'
 * array is used. Depending on 'flip' extrusion is inverted.
 * 
 * Indices and flip buffers can be static.
 * 
 * <pre>
 * First pass: using even vertex array positions
 *   (used vertices are in braces)
 * vertex id   0  1  2  3  4  5  6  7
 * pos0     x (0) 1 (2) 3 (4) 5 (6) 7 x
 * pos1        x (0) 1 (2) 3 (4) 5 (6) 7 x
 * flip        0  1  0  1  0  1  0  1
 * 
 * Second pass: using odd vertex array positions
 * vertex id   0  1  2  3  4  5  6  7
 * pos0   x 0 (1) 2 (3) 4 (5) 6 (7) x
 * pos1      x 0 (1) 2 (3) 4 (5) 6 (7) x
 * flip        0  1  0  1  0  1  0  1
 * </pre>
 * 
 * Vertex layout:
 * [2 short] position,
 * [2 short] extrusion,
 * [1 short] line length
 * [1 short] unused
 * 
 * indices, for two blocks:
 * 0, 1, 2,
 * 2, 1, 3,
 * 4, 5, 6,
 * 6, 5, 7,
 * 
 * BIG NOTE: renderer assumes to be able to offset vertex array position
 * so that in the first pass 'pos1' offset will be < 0 if no data precedes
 * - in our case there is always the polygon fill array at start
 * - see addLine hack otherwise.
 */
public final class LineTexLayer extends RenderElement {

	static final Logger log = LoggerFactory.getLogger(LineTexLayer.class);

	private static final float COORD_SCALE = MapRenderer.COORD_SCALE;
	/* scale factor mapping extrusion vector to short values */
	public static final float DIR_SCALE = 2048;

	public LineStyle line;
	public float width;

	public int evenQuads;
	public int oddQuads;

	private boolean evenSegment;

	protected boolean mRandomizeOffset = true;

	LineTexLayer(int layer) {
		super(TEXLINE);

		this.level = layer;
		this.evenSegment = true;
	}

	public void addLine(GeometryBuffer geom) {
		addLine(geom.points, geom.index);
	}

	public void addLine(float[] points, short[] index) {

		if (vertexItems.empty()) {
			/* HACK add one vertex offset when compiling
			 * buffer otherwise one cant use the full
			 * VertexItem (see Layers.compile)
			 * add the two 'x' at front and end */
			//numVertices = 2;

			/* the additional end vertex to make sure
			 * not to read outside allocated memory */
			numVertices = 1;
		}
		VertexData vi = vertexItems;

		boolean even = evenSegment;

		/* reset offset to last written position */
		if (!even)
			vi.seek(-12);

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

			/* check end-marker in indices */
			if (length < 0)
				break;

			/* need at least two points */
			if (length < 4) {
				pos += length;
				continue;
			}

			int end = pos + length;
			float x = points[pos++] * COORD_SCALE;
			float y = points[pos++] * COORD_SCALE;

			/* randomize a bit */
			float lineLength = mRandomizeOffset ? (x * x + y * y) % 80 : 0;

			while (pos < end) {
				float nx = points[pos++] * COORD_SCALE;
				float ny = points[pos++] * COORD_SCALE;

				/* Calculate triangle corners for the given width */
				float vx = nx - x;
				float vy = ny - y;

				float a = (float) Math.sqrt(vx * vx + vy * vy);

				/* normal vector */
				vx /= a;
				vy /= a;

				/* perpendicular to line segment */
				float ux = -vy;
				float uy = vx;

				short dx = (short) (ux * DIR_SCALE);
				short dy = (short) (uy * DIR_SCALE);

				vi.add((short) x,
				       (short) y,
				       dx, dy,
				       (short) lineLength,
				       (short) 0);

				lineLength += a;

				vi.seek(6);
				vi.add((short) nx,
				       (short) ny,
				       dx, dy,
				       (short) lineLength,
				       (short) 0);

				x = nx;
				y = ny;

				if (even) {
					/* go to second segment */
					vi.seek(-12);
					even = false;

					/* vertex 0 and 2 were added */
					numVertices += 3;
					evenQuads++;
				} else {
					/* go to next block */
					even = true;

					/* vertex 1 and 3 were added */
					numVertices += 1;
					oddQuads++;
				}
			}
		}

		evenSegment = even;

		/* advance offset to last written position */
		if (!even)
			vi.seek(12);
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
		ElementLayers.addPoolItems(this, sbuf);
		/* add additional vertex for interleaving, see TexLineLayer. */
		sbuf.position(sbuf.position() + 6);
	}

	static class Shader extends GLShader {
		int uMVP, uColor, uWidth, uBgColor, uScale;
		int uPatternWidth, uPatternScale;
		int aPos0, aPos1, aLen0, aLen1, aFlip;

		Shader(String shaderFile) {
			if (!create(shaderFile))
				return;

			uMVP = getUniform("u_mvp");

			uScale = getUniform("u_scale");
			uColor = getUniform("u_color");
			uWidth = getUniform("u_width");
			uBgColor = getUniform("u_bgcolor");

			uPatternWidth = getUniform("u_pwidth");
			uPatternScale = getUniform("u_pscale");

			aPos0 = getAttrib("a_pos0");
			aPos1 = getAttrib("a_pos1");
			aLen0 = getAttrib("a_len0");
			aLen1 = getAttrib("a_len1");
			aFlip = getAttrib("a_flip");
		}
	}

	public final static class Renderer {
		private static Shader shader;

		/* factor to normalize extrusion vector and scale to coord scale */
		private final static float COORD_SCALE_BY_DIR_SCALE =
		        MapRenderer.COORD_SCALE / LineLayer.DIR_SCALE;

		private static int mVertexFlipID;

		public static void init() {

			shader = new Shader("linetex_layer");

			int[] vboIds = GLUtils.glGenBuffers(1);
			mVertexFlipID = vboIds[0];

			/* bytes: 0, 1, 0, 1, 0, ... */
			byte[] flip = new byte[MapRenderer.maxQuads * 4];
			for (int i = 0; i < flip.length; i++)
				flip[i] = (byte) (i % 2);

			ByteBuffer buf = ByteBuffer.allocateDirect(flip.length)
			    .order(ByteOrder.nativeOrder());
			buf.put(flip);
			buf.flip();

			ShortBuffer sbuf = buf.asShortBuffer();

			GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, mVertexFlipID);
			GL.glBufferData(GL20.GL_ARRAY_BUFFER, flip.length, sbuf,
			                GL20.GL_STATIC_DRAW);
			GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

			//		mTexID = new int[10];
			//		byte[] stipple = new byte[2];
			//		stipple[0] = 32;
			//		stipple[1] = 32;
			//		mTexID[0] = GlUtils.loadStippleTexture(stipple);
		}

		private final static int STRIDE = 12;
		private final static int LEN_OFFSET = 8;

		public static RenderElement draw(RenderElement curLayer, GLViewport v,
		        float div, ElementLayers layers) {

			//if (shader == 0)
			//	return curLayer.next;

			GLState.blend(true);
			//GLState.useProgram(shader);
			shader.useProgram();

			GLState.enableVertexArrays(-1, -1);

			int aLen0 = shader.aLen0;
			int aLen1 = shader.aLen1;
			int aPos0 = shader.aPos0;
			int aPos1 = shader.aPos1;
			int aFlip = shader.aFlip;

			GL.glEnableVertexAttribArray(aPos0);
			GL.glEnableVertexAttribArray(aPos1);
			GL.glEnableVertexAttribArray(aLen0);
			GL.glEnableVertexAttribArray(aLen1);
			GL.glEnableVertexAttribArray(aFlip);

			v.mvp.setAsUniform(shader.uMVP);

			int maxIndices = MapRenderer.maxQuads * 6;

			MapRenderer.bindQuadIndicesVBO(true);

			GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, mVertexFlipID);
			GL.glVertexAttribPointer(shader.aFlip, 1,
			                         GL20.GL_BYTE, false, 0, 0);

			layers.vbo.bind();

			float scale = (float) v.pos.getZoomScale();

			float s = scale / div;

			//GL.glBindTexture(GL20.GL_TEXTURE_2D, mTexID[0]);

			RenderElement l = curLayer;
			for (; l != null && l.type == TEXLINE; l = l.next) {
				LineTexLayer ll = (LineTexLayer) l;
				LineStyle line = ll.line.current();

				GLUtils.setColor(shader.uColor, line.stippleColor, 1);
				GLUtils.setColor(shader.uBgColor, line.color, 1);

				float pScale = (int) (s + 0.5f);
				if (pScale < 1)
					pScale = 1;

				GL.glUniform1f(shader.uPatternScale,
				               (MapRenderer.COORD_SCALE * line.stipple) / pScale);

				GL.glUniform1f(shader.uPatternWidth, line.stippleWidth);
				//GL.glUniform1f(hScale, scale);

				/* keep line width fixed */
				GL.glUniform1f(shader.uWidth, ll.width / s * COORD_SCALE_BY_DIR_SCALE);

				/* add offset vertex */
				int vOffset = -STRIDE;

				// TODO interleave 1. and 2. pass to improve vertex cache usage?
				/* first pass */
				int allIndices = (ll.evenQuads * 6);
				for (int i = 0; i < allIndices; i += maxIndices) {
					int numIndices = allIndices - i;
					if (numIndices > maxIndices)
						numIndices = maxIndices;

					/* i / 6 * (24 shorts per block * 2 short bytes) */
					int add = (l.offset + i * 8) + vOffset;

					GL.glVertexAttribPointer(aPos0, 4, GL20.GL_SHORT, false, STRIDE,
					                         add + STRIDE);

					GL.glVertexAttribPointer(aLen0, 2, GL20.GL_SHORT, false, STRIDE,
					                         add + STRIDE + LEN_OFFSET);

					GL.glVertexAttribPointer(aPos1, 4, GL20.GL_SHORT, false, STRIDE,
					                         add);

					GL.glVertexAttribPointer(aLen1, 2, GL20.GL_SHORT, false, STRIDE,
					                         add + LEN_OFFSET);

					GL.glDrawElements(GL20.GL_TRIANGLES, numIndices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}

				/* second pass */
				allIndices = (ll.oddQuads * 6);
				for (int i = 0; i < allIndices; i += maxIndices) {
					int numIndices = allIndices - i;
					if (numIndices > maxIndices)
						numIndices = maxIndices;
					/* i / 6 * (24 shorts per block * 2 short bytes) */
					int add = (l.offset + i * 8) + vOffset;

					GL.glVertexAttribPointer(aPos0, 4, GL20.GL_SHORT, false, STRIDE,
					                         add + 2 * STRIDE);

					GL.glVertexAttribPointer(aLen0, 2, GL20.GL_SHORT, false, STRIDE,
					                         add + 2 * STRIDE + LEN_OFFSET);

					GL.glVertexAttribPointer(aPos1, 4, GL20.GL_SHORT, false, STRIDE,
					                         add + STRIDE);

					GL.glVertexAttribPointer(aLen1, 2, GL20.GL_SHORT, false, STRIDE,
					                         add + STRIDE + LEN_OFFSET);

					GL.glDrawElements(GL20.GL_TRIANGLES, numIndices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}
				//GlUtils.checkGlError(TAG);
			}

			MapRenderer.bindQuadIndicesVBO(false);

			GL.glDisableVertexAttribArray(aPos0);
			GL.glDisableVertexAttribArray(aPos1);
			GL.glDisableVertexAttribArray(aLen0);
			GL.glDisableVertexAttribArray(aLen1);
			GL.glDisableVertexAttribArray(aFlip);

			//GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);

			return l;
		}
	}
}
