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
package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.MapRenderer.Matrices;

/**
 * Renderer for a single bitmap, width and height must be power of 2.
 */
public class BitmapLayer extends TextureLayer {

	//	private final static String TAG = BitmapLayer.class.getName();
	private Bitmap mBitmap;
	private final boolean mReuseBitmap;
	private final short[] mVertices;

	/**
	 * @param reuseBitmap false if the Bitmap should be recycled after
	 *            it is compiled to texture.
	 */
	public BitmapLayer(boolean reuseBitmap) {
		super(RenderElement.BITMAP);

		mReuseBitmap = reuseBitmap;
		mVertices = new short[24];

		// used for size calculation of Layers buffer.
		verticesCnt = 4;
	}

	public void setBitmap(Bitmap bitmap, int w, int h) {
		mWidth = w;
		mHeight = h;

		mBitmap = bitmap;
		if (this.textures == null)
			this.textures = new TextureItem(mBitmap);

		TextureItem ti = this.textures;
		ti.vertices = TextureLayer.Renderer.INDICES_PER_SPRITE;
	}

	private int mWidth, mHeight;

	/**
	 * Set target dimension to renderthe bitmap
	 */
	public void setSize(int w, int h) {
		mWidth = w;
		mHeight = h;
	}

	private void setVertices(ShortBuffer sbuf) {
		short[] buf = mVertices;
		short w = (short) (mWidth * MapRenderer.COORD_SCALE);
		short h = (short) (mHeight * MapRenderer.COORD_SCALE);

		short t = 1;

		int pos = 0;
		// top-left
		buf[pos++] = 0;
		buf[pos++] = 0;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = 0;
		buf[pos++] = 0;
		// bot-left
		buf[pos++] = 0;
		buf[pos++] = h;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = 0;
		buf[pos++] = t;
		// top-right
		buf[pos++] = w;
		buf[pos++] = 0;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = t;
		buf[pos++] = 0;
		// bot-right
		buf[pos++] = w;
		buf[pos++] = h;
		buf[pos++] = -1;
		buf[pos++] = -1;
		buf[pos++] = t;
		buf[pos++] = t;

		this.offset = sbuf.position() * 2; // bytes
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

		if (mBitmap != null) {
			if (!mReuseBitmap)
				mBitmap.recycle();

			mBitmap = null;
			textures.bitmap = null;
		}

		TextureItem.releaseTexture(textures);
		textures = null;

		VertexItem.pool.releaseAll(vertexItems);
		vertexItems = null;
	}

	public static final class Renderer {

		//private final static String TAG = BitmapRenderer.class.getName();

		public final static boolean debug = true;

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

		public static RenderElement draw(RenderElement renderElement, Matrices m, float scale,
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

			GL.glUniform1f(hTextureScreenScale, 1f / MapRenderer.screenWidth);
			GL.glUniform1f(hAlpha, alpha);

			m.proj.setAsUniform(hTextureProjMatrix);

			m.mvp.setAsUniform(hTextureMVMatrix);

			GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, MapRenderer.mQuadIndicesID);

			for (TextureItem ti = tl.textures; ti != null; ti = ti.next) {

				ti.bind();

				int maxVertices = MapRenderer.maxQuads * INDICES_PER_SPRITE;

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

		private final static String textVertexShader = ""
		        + "precision mediump float; "
		        + "attribute vec4 vertex;"
		        + "attribute vec2 tex_coord;"
		        + "uniform mat4 u_mv;"
		        + "uniform mat4 u_proj;"
		        + "uniform float u_scale;"
		        + "uniform float u_swidth;"
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
