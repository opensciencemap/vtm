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
package org.oscim.renderer.layer;

import java.nio.ShortBuffer;

import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;

import android.graphics.Bitmap;

public class BitmapLayer extends TextureLayer {
	//	private final static String TAG = BitmapLayer.class.getName();
	private Bitmap mBitmap;

	public BitmapLayer() {
		type = Layer.BITMAP;
	}

	public void setBitmap(Bitmap bitmap) {
		mBitmap = bitmap;

		vertexItems = VertexItem.pool.get();
		short[] buf = vertexItems.vertices;
		short size = (short) (Tile.SIZE * GLRenderer.COORD_SCALE);
		short center = (short) (size >> 1);
		short m = (short) (-(size >> 1));
		short p = center;
		short t = (8 * 256);

		int pos = 0;
		// top-left
		buf[pos++] = 0;
		buf[pos++] = 0;
		buf[pos++] = m;
		buf[pos++] = m;
		buf[pos++] = 0;
		buf[pos++] = 0;
		// bot-left
		buf[pos++] = 0;
		buf[pos++] = size;
		buf[pos++] = m;
		buf[pos++] = p;
		buf[pos++] = 0;
		buf[pos++] = t;
		// top-right
		buf[pos++] = size;
		buf[pos++] = 0;
		buf[pos++] = p;
		buf[pos++] = m;
		buf[pos++] = t;
		buf[pos++] = 0;
		// bot-right
		buf[pos++] = size;
		buf[pos++] = size;
		buf[pos++] = p;
		buf[pos++] = p;
		buf[pos++] = t;
		buf[pos++] = t;

		vertexItems.used = 24;

		TextureItem ti = this.textures = new TextureItem(-1);
		ti.ownBitmap = true;
		ti.width = mBitmap.getWidth();
		ti.height = mBitmap.getHeight();
		ti.bitmap = mBitmap;
		ti.vertices = TextureRenderer.INDICES_PER_SPRITE;

		verticesCnt = 4;
	}

	@Override
	public boolean prepare() {
		return false;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
		if (mBitmap == null)
			return;

		super.compile(sbuf);

		mBitmap.recycle();
		mBitmap = null;
		textures.bitmap = null;
	}

	@Override
	protected void clear() {

		if (mBitmap != null) {
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
