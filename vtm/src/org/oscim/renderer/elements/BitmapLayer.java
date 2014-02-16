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

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;

/**
 * Renderer for a single bitmap, width and height must be power of 2.
 */
public class BitmapLayer extends TextureLayer {
	// TODO share layers.vbo() between BitmapTileLayers

	//	static final Logger log = LoggerFactory.getLogger(BitmapLayer.class);
	private Bitmap mBitmap;
	private final boolean mReuseBitmap;
	private final short[] mVertices;
	private int mWidth, mHeight;

	/**
	 * @param reuseBitmap false if the Bitmap should be disposed
	 *            after loading to texture.
	 */
	public BitmapLayer(boolean reuseBitmap) {
		super(RenderElement.BITMAP);

		mReuseBitmap = reuseBitmap;
		mVertices = new short[24];

		// used for size calculation of Layers buffer.
		numVertices = 4;
	}

	/**
	 * w/h sets also target dimension to render the bitmap.
	 */
	public void setBitmap(Bitmap bitmap, int w, int h) {
		mWidth = w;
		mHeight = h;

		mBitmap = bitmap;
		if (textures == null)
			textures = new TextureItem(mBitmap);

		TextureItem t = textures;
		t.vertices = TextureLayer.INDICES_PER_SPRITE;
	}

	private void setVertices(ShortBuffer sbuf) {
		short[] buf = mVertices;
		short w = (short) (mWidth * MapRenderer.COORD_SCALE);
		short h = (short) (mHeight * MapRenderer.COORD_SCALE);

		short texMin = 0;
		short texMax = 1;

		//	putSprite(buf, pos, tx, ty, x1, y1, x2, y2, u1, v1, u2, v2);
		int pos = 0;

		// top-left
		buf[pos++] = 0;
		buf[pos++] = 0;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = texMin;
		buf[pos++] = texMin;
		// bot-left
		buf[pos++] = 0;
		buf[pos++] = h;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = texMin;
		buf[pos++] = texMax;
		// top-right
		buf[pos++] = w;
		buf[pos++] = 0;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = texMax;
		buf[pos++] = texMin;
		// bot-right
		buf[pos++] = w;
		buf[pos++] = h;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = texMax;
		buf[pos++] = texMax;

		this.offset = sbuf.position() * 2;
		sbuf.put(buf);
	}

	@Override
	public boolean prepare() {
		return false;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {

		if (mBitmap == null)
			return;

		setVertices(sbuf);

		textures.upload();

		if (!mReuseBitmap) {
			mBitmap.recycle();
			mBitmap = null;
			textures.bitmap = null;
		}
	}

	@Override
	protected void clear() {

		// release textures and vertexItems
		super.clear();

		if (mBitmap == null)
			return;

		if (!mReuseBitmap)
			mBitmap.recycle();

		mBitmap = null;

		//textures.bitmap = null;
		//textures.dispose();
		//TextureItem.pool.releaseTexture(textures);
		//textures = null;
	}

	public static final class Renderer {

		private static int mTextureProgram;
		private static int hTextureMVMatrix;
		private static int hTextureProjMatrix;
		private static int hTextureVertex;
		private static int hTextureScale;
		private static int hTextureScreenScale;
		private static int hTextureTexCoord;

		private static int hAlpha;

		public final static int INDICES_PER_SPRITE = 6;
		final static int VERTICES_PER_SPRITE = 4;
		final static int SHORTS_PER_VERTICE = 6;

		static void init() {
			mTextureProgram = GLUtils.createProgram(textVertexShader,
			                                        textFragmentShader);

			hTextureMVMatrix = GL.glGetUniformLocation(mTextureProgram, "u_mv");
			hTextureProjMatrix = GL.glGetUniformLocation(mTextureProgram, "u_proj");
			hTextureScale = GL.glGetUniformLocation(mTextureProgram, "u_scale");
			hTextureScreenScale = GL.glGetUniformLocation(mTextureProgram, "u_swidth");
			hTextureVertex = GL.glGetAttribLocation(mTextureProgram, "vertex");
			hTextureTexCoord = GL.glGetAttribLocation(mTextureProgram, "tex_coord");
			hAlpha = GL.glGetUniformLocation(mTextureProgram, "u_alpha");
		}

		public static RenderElement draw(RenderElement renderElement, GLViewport v, float scale,
		        float alpha) {
			//GLState.test(false, false);
			GLState.blend(true);

			GLState.useProgram(mTextureProgram);

			GLState.enableVertexArrays(hTextureTexCoord, hTextureVertex);

			TextureLayer tl = (TextureLayer) renderElement;

			if (tl.fixed)
				GL.glUniform1f(hTextureScale, (float) Math.sqrt(scale));
			else
				GL.glUniform1f(hTextureScale, 1);

			GL.glUniform1f(hTextureScreenScale, 1f / v.getWidth());
			GL.glUniform1f(hAlpha, alpha);

			v.proj.setAsUniform(hTextureProjMatrix);

			v.mvp.setAsUniform(hTextureMVMatrix);

			MapRenderer.bindQuadIndicesVBO(true);

			for (TextureItem t = tl.textures; t != null; t = t.next) {

				t.bind();

				int maxVertices = MapRenderer.maxQuads * INDICES_PER_SPRITE;

				// draw up to maxVertices in each iteration
				for (int i = 0; i < t.vertices; i += maxVertices) {
					// to.offset * (24(shorts) * 2(short-bytes) / 6(indices) == 8)
					int off = (t.offset + i) * 8 + tl.offset;

					GL.glVertexAttribPointer(hTextureVertex, 4,
					                         GL20.GL_SHORT, false, 12, off);

					GL.glVertexAttribPointer(hTextureTexCoord, 2,
					                         GL20.GL_SHORT, false, 12, off + 8);

					int numVertices = t.vertices - i;
					if (numVertices > maxVertices)
						numVertices = maxVertices;

					GL.glDrawElements(GL20.GL_TRIANGLES, numVertices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}
			}

			MapRenderer.bindQuadIndicesVBO(false);

			return renderElement.next;
		}

		private final static String textVertexShader = ""
		        + "precision mediump float; "
		        + "attribute vec4 vertex;"
		        + "attribute vec2 tex_coord;"
		        + "uniform mat4 u_mv;"
		        + "varying vec2 tex_c;"
		        + "void main() {"
		        + "  gl_Position = u_mv * vec4(vertex.xy, 0.0, 1.0);"
		        + "  tex_c = tex_coord;"
		        + "}";

		private final static String textFragmentShader = ""
		        + "precision mediump float;"
		        + "uniform sampler2D tex;"
		        + "uniform float u_alpha;"
		        + "varying vec2 tex_c;"
		        + "void main() {"
		        + "   gl_FragColor = texture2D(tex, tex_c.xy) * u_alpha;"
		        + "}";
	}

}
