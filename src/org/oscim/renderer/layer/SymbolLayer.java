/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.renderer.TextureObject;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.opengl.GLUtils;
import android.util.Log;

// TODO share one static texture for all poi map symabols

public final class SymbolLayer extends TextureLayer {
	private static String TAG = SymbolLayer.class.getSimpleName();

	private final static int TEXTURE_WIDTH = 256;
	private final static int TEXTURE_HEIGHT = 256;
	private final static float SCALE = 8.0f;

	private static short[] mVertices;
	private static Bitmap mBitmap;
	private static Canvas mCanvas;
	private static int mBitmapFormat;
	private static int mBitmapType;

	SymbolItem symbols;

	public SymbolLayer() {
		if (mBitmap == null) {
			mBitmap = Bitmap.createBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT,
					Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
			mBitmapFormat = GLUtils.getInternalFormat(mBitmap);
			mBitmapType = GLUtils.getType(mBitmap);
			//
			mVertices = new short[40 * 24];
		}
	}

	public void addSymbol(SymbolItem item) {

		verticesCnt += 4;

		SymbolItem it = symbols;

		for (; it != null; it = it.next) {
			if (it.bitmap == item.bitmap) {
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = symbols;
		symbols = item;
	}

	private final static int LBIT_MASK = 0xfffffffe;
	private final RectF mRect = new RectF();

	// TODO ... reuse texture when only symbol position changed
	public void compile(ShortBuffer sbuf) {

		int pos = 0;
		short buf[] = mVertices;

		int advanceY = 0;
		float x = 0;
		float y = 0;

		mBitmap.eraseColor(Color.TRANSPARENT);

		for (SymbolItem it = symbols; it != null;) {

			// add bitmap
			float width = it.bitmap.getWidth();
			float height = it.bitmap.getHeight();

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = 0;
				y += advanceY;
				advanceY = (int) (height + 0.5f);

				if (y + height > TEXTURE_HEIGHT) {
					Log.d(TAG, "reached max symbols");
					// need to sync bitmap upload somehow???
					TextureObject to = TextureObject.get();
					TextureObject.uploadTexture(to, mBitmap,
							mBitmapFormat, mBitmapType,
							TEXTURE_WIDTH, TEXTURE_HEIGHT);
					to.next = textures;
					textures = to;

					sbuf.put(buf, 0, pos);
					pos = 0;
				}
			}
			mRect.left = x;
			mRect.top = y;
			mRect.right = x + width;
			mRect.bottom = y + height;
			// Log.d("...", "draw " + x + " " + y + " " + width + " " + height);

			mCanvas.drawBitmap(it.bitmap, null, mRect, null);
			// mCanvas.drawBitmap(it.bitmap, x, y, null);

			float hw = width / 2.0f;
			float hh = height / 2.0f;
			short x1, x2, x3, x4, y1, y2, y3, y4;
			x1 = x3 = (short) (SCALE * (-hw));
			x2 = x4 = (short) (SCALE * (hw));

			y1 = y3 = (short) (SCALE * (hh));
			y2 = y4 = (short) (SCALE * (-hh));

			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add symbol items referencing the same bitmap
			for (SymbolItem it2 = it;; it2 = it2.next) {

				if (it2 == null || it2.bitmap != it.bitmap) {
					it = it2;
					break;
				}

				// add vertices
				short tx = (short) ((int) (SCALE * it2.x) & LBIT_MASK | (it2.billboard ? 1 : 0));
				short ty = (short) (SCALE * it2.y);

				// top-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y1;
				buf[pos++] = u1;
				buf[pos++] = v2;

				// top-right
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x2;
				buf[pos++] = y3;
				buf[pos++] = u2;
				buf[pos++] = v2;

				// bot-right
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x4;
				buf[pos++] = y4;
				buf[pos++] = u2;
				buf[pos++] = v1;

				// bot-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x3;
				buf[pos++] = y2;
				buf[pos++] = u1;
				buf[pos++] = v1;

				x += width + 1;
			}
		}

		TextureObject to = TextureObject.get();

		TextureObject.uploadTexture(to, mBitmap,
				mBitmapFormat, mBitmapType,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);

		to.next = textures;
		textures = to;

		sbuf.put(buf, 0, pos);
	}
}
