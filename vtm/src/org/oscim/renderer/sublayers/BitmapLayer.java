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

import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.GLRenderer;

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
	 * it is compiled to texture.
	 * */
	public BitmapLayer(boolean reuseBitmap) {
		type = Layer.BITMAP;
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
		ti.vertices = TextureRenderer.INDICES_PER_SPRITE;
	}

	private int mWidth, mHeight;

	/**
	 * Set target dimension to renderthe bitmap */
	public void setSize(int w, int h){
		mWidth = w;
		mHeight = h;
	}

	private void setVertices(ShortBuffer sbuf){
		short[] buf = mVertices;
		short w = (short) (mWidth * GLRenderer.COORD_SCALE);
		short h = (short) (mHeight* GLRenderer.COORD_SCALE);

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
}
