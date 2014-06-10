/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import static org.oscim.renderer.MapRenderer.COORD_SCALE;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.TextureItem.TexturePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TextureLayer extends RenderElement {

	static final Logger log = LoggerFactory.getLogger(TextureLayer.class);

	public final static int INDICES_PER_SPRITE = 6;
	final static int VERTICES_PER_SPRITE = 4;
	final static int SHORTS_PER_VERTICE = 6;

	final static int TEXTURE_HEIGHT = 128;
	final static int TEXTURE_WIDTH = 512;
	final static int POOL_FILL = 10;

	/** pool shared by TextLayers */
	final static TexturePool pool = new TexturePool(POOL_FILL,
	                                                TEXTURE_WIDTH,
	                                                TEXTURE_HEIGHT);

	protected TextureLayer(int type) {
		super(type);
	}

	/** holds textures and offset in vbo */
	public TextureItem textures;

	/** scale mode */
	public boolean fixed;

	@Override
	protected void compile(ShortBuffer sbuf) {

		for (TextureItem t = textures; t != null; t = t.next)
			t.upload();

		/* add vertices to vbo */
		ElementLayers.addPoolItems(this, sbuf);
	}

	abstract public boolean prepare();

	protected void clear() {
		while (textures != null)
			textures = textures.dispose();

		vertexItems.dispose();

		numVertices = 0;
	}

	static class Shader extends GLShader {
		int uMV, uProj, uScale, uTexSize, aPos, aTexCoord;

		Shader() {
			if (!create("texture_layer"))
				return;
			uMV = getUniform("u_mv");
			uProj = getUniform("u_proj");
			uScale = getUniform("u_scale");
			uTexSize = getUniform("u_div");
			aPos = getAttrib("vertex");
			aTexCoord = getAttrib("tex_coord");
		}

		@Override
		public boolean useProgram() {
			if (super.useProgram()) {
				GLState.enableVertexArrays(aPos, aTexCoord);
				return true;
			}
			return false;
		}
	}

	public static final class Renderer {

		public final static boolean debug = false;

		private static Shader shader;

		static void init() {
			shader = new Shader();

			/* FIXME pool should be disposed on exit... */
			pool.init(0);
		}

		public static RenderElement draw(ElementLayers layers, RenderElement l,
		        GLViewport v, float scale) {

			GLState.test(false, false);
			GLState.blend(true);

			shader.useProgram();

			TextureLayer tl = (TextureLayer) l;
			GL.glUniform1f(shader.uScale, tl.fixed ? 1 / scale : 1);

			v.proj.setAsUniform(shader.uProj);
			v.mvp.setAsUniform(shader.uMV);

			MapRenderer.bindQuadIndicesVBO(true);

			for (TextureItem t = tl.textures; t != null; t = t.next) {
				GL.glUniform2f(shader.uTexSize,
				               1f / (t.width * COORD_SCALE),
				               1f / (t.height * COORD_SCALE));
				t.bind();

				int maxIndices = MapRenderer.maxQuads * INDICES_PER_SPRITE;

				/* draw up to maxVertices in each iteration */
				for (int i = 0; i < t.indices; i += maxIndices) {
					/* to.offset * (24(shorts) * 2(short-bytes)
					 * / 6(indices) == 8) */
					int off = (t.offset + i) * 8 + tl.offset;

					if (layers.useVBO) {
						GL.glVertexAttribPointer(shader.aPos, 4,
						                         GL20.GL_SHORT,
						                         false, 12, off);

						GL.glVertexAttribPointer(shader.aTexCoord, 2,
						                         GL20.GL_SHORT,
						                         false, 12, off + 8);
					} else {
						layers.vertexArrayBuffer.position(off);
						GL.glVertexAttribPointer(shader.aPos, 4,
						                         GL20.GL_SHORT, false, 12,
						                         layers.vertexArrayBuffer);
						layers.vertexArrayBuffer.position(off + 8);
						GL.glVertexAttribPointer(shader.aTexCoord,
						                         2, GL20.GL_SHORT, false, 12,
						                         layers.vertexArrayBuffer);
					}
					int numIndices = t.indices - i;
					if (numIndices > maxIndices)
						numIndices = maxIndices;

					GL.glDrawElements(GL20.GL_TRIANGLES, numIndices,
					                  GL20.GL_UNSIGNED_SHORT, 0);

				}
			}

			MapRenderer.bindQuadIndicesVBO(false);

			return l.next;
		}
	}
}
