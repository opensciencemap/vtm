/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.MapRenderer.Matrices;

public abstract class TextureLayer extends RenderElement {

	protected TextureLayer(byte type) {
		super(type);
	}

	// holds textures and offset in vbo
	public TextureItem textures;

	// scale mode
	public boolean fixed;

	/**
	 * @param sbuf
	 *            buffer to add vertices
	 */
	@Override
	protected void compile(ShortBuffer sbuf) {

		for (TextureItem to = textures; to != null; to = to.next)
			to.upload();

		// add vertices to vbo
		ElementLayers.addPoolItems(this, sbuf);
	}

	abstract public boolean prepare();

	static void putSprite(short buf[], int pos,
	        short tx, short ty,
	        short x1, short y1,
	        short x2, short y2,
	        short u1, short v1,
	        short u2, short v2) {

		// top-left
		buf[pos + 0] = tx;
		buf[pos + 1] = ty;
		buf[pos + 2] = x1;
		buf[pos + 3] = y1;
		buf[pos + 4] = u1;
		buf[pos + 5] = v2;
		// bot-left
		buf[pos + 6] = tx;
		buf[pos + 7] = ty;
		buf[pos + 8] = x1;
		buf[pos + 9] = y2;
		buf[pos + 10] = u1;
		buf[pos + 11] = v1;
		// top-right
		buf[pos + 12] = tx;
		buf[pos + 13] = ty;
		buf[pos + 14] = x2;
		buf[pos + 15] = y1;
		buf[pos + 16] = u2;
		buf[pos + 17] = v2;
		// bot-right
		buf[pos + 18] = tx;
		buf[pos + 19] = ty;
		buf[pos + 20] = x2;
		buf[pos + 21] = y2;
		buf[pos + 22] = u2;
		buf[pos + 23] = v1;
	}

	public static final class Renderer {
		//static final Logger log = LoggerFactory.getLogger(TextureRenderer.class);

		public final static boolean debug = false;

		private static int mTextureProgram;
		private static int hTextureMVMatrix;
		private static int hTextureProjMatrix;
		private static int hTextureVertex;
		private static int hTextureScale;
		private static int hTextureScreenScale;
		private static int hTextureTexCoord;
		private static int hTextureSize;

		public final static int INDICES_PER_SPRITE = 6;
		final static int VERTICES_PER_SPRITE = 4;
		final static int SHORTS_PER_VERTICE = 6;

		static void init() {

			mTextureProgram = GLUtils.createProgram(textVertexShader,
			                                        textFragmentShader);

			hTextureMVMatrix = GL.glGetUniformLocation(mTextureProgram, "u_mv");
			hTextureProjMatrix = GL.glGetUniformLocation(mTextureProgram, "u_proj");
			hTextureScale = GL.glGetUniformLocation(mTextureProgram, "u_scale");
			hTextureSize = GL.glGetUniformLocation(mTextureProgram, "u_div");
			hTextureScreenScale = GL.glGetUniformLocation(mTextureProgram, "u_swidth");
			hTextureVertex = GL.glGetAttribLocation(mTextureProgram, "vertex");
			hTextureTexCoord = GL.glGetAttribLocation(mTextureProgram, "tex_coord");
		}

		public static RenderElement draw(RenderElement renderElement, float scale, Matrices m) {

			GLState.test(false, false);
			GLState.blend(true);

			GLState.useProgram(mTextureProgram);

			GLState.enableVertexArrays(hTextureTexCoord, hTextureVertex);

			TextureLayer tl = (TextureLayer) renderElement;

			if (tl.fixed)
				GL.glUniform1f(hTextureScale, (float) Math.sqrt(scale));
			else
				GL.glUniform1f(hTextureScale, 1);

			GL.glUniform1f(hTextureScreenScale, 1f / MapRenderer.screenWidth);

			m.proj.setAsUniform(hTextureProjMatrix);
			m.mvp.setAsUniform(hTextureMVMatrix);

			GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, MapRenderer.mQuadIndicesID);

			for (TextureItem ti = tl.textures; ti != null; ti = ti.next) {

				ti.bind();

				int maxVertices = MapRenderer.maxQuads * INDICES_PER_SPRITE;

				GL.glUniform2f(hTextureSize,
				               1f / (ti.width * COORD_SCALE),
				               1f / (ti.height * COORD_SCALE));

				// draw up to maxVertices in each iteration
				for (int i = 0; i < ti.vertices; i += maxVertices) {
					// to.offset * (24(shorts) * 2(short-bytes) / 6(indices) == 8)
					int off = (ti.offset + i) * 8 + tl.offset;

					GL.glVertexAttribPointer(hTextureVertex, 4,
					                         GL20.GL_SHORT, false, 12, off);

					GL.glVertexAttribPointer(hTextureTexCoord, 2,
					                         GL20.GL_SHORT, false, 12, off + 8);

					int numVertices = ti.vertices - i;
					if (numVertices > maxVertices)
						numVertices = maxVertices;

					GL.glDrawElements(GL20.GL_TRIANGLES, numVertices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}
			}

			GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

			return renderElement.next;
		}

		private final static double COORD_DIV = 1.0 / MapRenderer.COORD_SCALE;

		private final static String textVertexShader = ""
		        + "precision highp float;"
		        + "attribute vec4 vertex;"
		        + "attribute vec2 tex_coord;"
		        + "uniform mat4 u_mv;"
		        + "uniform mat4 u_proj;"
		        + "uniform float u_scale;"
		        + "uniform float u_swidth;"
		        + "uniform vec2 u_div;"
		        + "varying vec2 tex_c;"
		        + "const float coord_scale = " + COORD_DIV + ";"
		        + "void main() {"
		        + "  vec4 pos;"
		        + "  vec2 dir = vertex.zw;"
		        + " if (mod(vertex.x, 2.0) == 0.0){"
		        + "       pos = u_proj * (u_mv * vec4(vertex.xy + dir * u_scale, 0.0, 1.0));"
		        + "  } else {" // place as billboard
		        + "    vec4 center = u_mv * vec4(vertex.xy, 0.0, 1.0);"
		        + "    pos = u_proj * (center + vec4(dir * (coord_scale * u_swidth), 0.0, 0.0));"
		        + "  }"
		        + "  gl_Position = pos;"
		        + "  tex_c = tex_coord * u_div;"
		        + "}";

		private final static String textFragmentShader = ""
		        + "precision highp float;"
		        + "uniform sampler2D tex;"
		        + "varying vec2 tex_c;"
		        + "void main() {"
		        + "   gl_FragColor = texture2D(tex, tex_c.xy);"
		        + "}";
	}
}
