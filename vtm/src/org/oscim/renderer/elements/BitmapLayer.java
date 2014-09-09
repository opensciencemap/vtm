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
import org.oscim.renderer.GLShader;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.TextureItem.TexturePool;

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
		setBitmap(bitmap, w, h, null);
	}

	public void setBitmap(Bitmap bitmap, int w, int h, TexturePool pool) {

		mWidth = w;
		mHeight = h;

		mBitmap = bitmap;
		if (textures == null) {
			if (pool == null)
				textures = new TextureItem(mBitmap);
			else {
				textures = pool.get(mBitmap);
			}
		}

		TextureItem t = textures;
		t.indices = TextureLayer.INDICES_PER_SPRITE;
	}

	private void setVertices(ShortBuffer vboData) {
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

		this.vertexOffset = vboData.position() * 2;
		vboData.put(buf);
	}

	@Override
	protected void compile(ShortBuffer vboData, ShortBuffer iboData) {

		if (mBitmap == null)
			return;

		setVertices(vboData);

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

	static class Shader extends GLShader {
		int uMVP, uAlpha, aPos, aTexCoord;

		Shader(String shaderFile) {
			if (!create(shaderFile))
				return;
			uMVP = getUniform("u_mvp");
			uAlpha = getUniform("u_alpha");
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

		public final static int INDICES_PER_SPRITE = 6;
		final static int VERTICES_PER_SPRITE = 4;
		final static int SHORTS_PER_VERTICE = 6;
		static Shader shader;

		static void init() {
			shader = new Shader("texture_alpha");
		}

		public static RenderElement draw(RenderElement renderElement, GLViewport v,
		        float scale, float alpha) {

			GLState.blend(true);
			Shader s = shader;
			s.useProgram();

			TextureLayer tl = (TextureLayer) renderElement;

			GL.glUniform1f(s.uAlpha, alpha);
			v.mvp.setAsUniform(s.uMVP);

			MapRenderer.bindQuadIndicesVBO(true);

			for (TextureItem t = tl.textures; t != null; t = t.next) {

				t.bind();

				int maxIndices = MapRenderer.maxQuads * INDICES_PER_SPRITE;

				// draw up to maxVertices in each iteration
				for (int i = 0; i < t.indices; i += maxIndices) {
					// to.offset * (24(shorts) * 2(short-bytes) / 6(indices) == 8)
					int off = (t.offset + i) * 8 + tl.vertexOffset;

					GL.glVertexAttribPointer(s.aPos, 2,
					                         GL20.GL_SHORT, false, 12, off);

					GL.glVertexAttribPointer(s.aTexCoord, 2,
					                         GL20.GL_SHORT, false, 12, off + 8);

					int numIndices = t.indices - i;
					if (numIndices > maxIndices)
						numIndices = maxIndices;

					GL.glDrawElements(GL20.GL_TRIANGLES, numIndices,
					                  GL20.GL_UNSIGNED_SHORT, 0);
				}
			}

			MapRenderer.bindQuadIndicesVBO(false);

			return renderElement.next;
		}
	}
}
