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

import org.oscim.renderer.TextureObject;
import org.oscim.renderer.TextureRenderer;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

// TODO share one static texture for all poi map symabols

public final class SymbolLayer extends TextureLayer {
	private final static String TAG = SymbolLayer.class.getSimpleName();

	private final static int TEXTURE_WIDTH = TextureObject.TEXTURE_WIDTH;
	private final static int TEXTURE_HEIGHT = TextureObject.TEXTURE_HEIGHT;
	private final static float SCALE = 8.0f;

	SymbolItem symbols;

	private Canvas mCanvas;
	private Rect mRect = new Rect();

	public SymbolLayer() {
		type = Layer.SYMBOL;
		fixed = true;
		mCanvas = new Canvas();
	}

	public void addSymbol(SymbolItem item) {

		verticesCnt += 4;

		for (SymbolItem it = symbols; it != null; it = it.next) {
			if (it.bitmap == item.bitmap) {
				// insert after same bitmap
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = symbols;
		symbols = item;
	}

	public void addDrawable(Drawable drawable, int state, float x, float y) {

		verticesCnt += 4;

		SymbolItem item = SymbolItem.get();
		item.drawable = drawable;
		item.x = x;
		item.y = y;
		item.billboard = true;
		item.state = state;

		for (SymbolItem it = symbols; it != null; it = it.next) {
			if (it.drawable == item.drawable && it.state == item.state) {
				// insert after same drawable
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = symbols;
		symbols = item;
	}

	private final static int LBIT_MASK = 0xfffffffe;

	// TODO reuse texture when only symbol position changed
	@Override
	public boolean prepare() {

		short numIndices = 0;
		short offsetIndices = 0;
		short curIndices = 0;

		curItem = VertexPool.get();
		pool = curItem;
		VertexPoolItem si = curItem;

		int pos = si.used;
		short buf[] = si.vertices;

		int advanceY = 0;
		float x = 0;
		float y = 0;

		TextureObject to = TextureObject.get();
		textures = to;
		mCanvas.setBitmap(to.bitmap);

		for (SymbolItem it = symbols; it != null;) {
			float width, height;

			if (it.bitmap != null) {
				// add bitmap
				width = it.bitmap.getWidth();
				height = it.bitmap.getHeight();
			} else {
				width = it.drawable.getIntrinsicWidth();
				height = it.drawable.getIntrinsicHeight();
			}

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = 0;
				y += advanceY;
				advanceY = (int) (height + 0.5f);

			}

			if (y + height > TEXTURE_HEIGHT) {
				Log.d(TAG, "reached max symbols: " + numIndices);

				to.offset = offsetIndices;
				to.vertices = curIndices;

				numIndices += curIndices;
				offsetIndices = numIndices;
				curIndices = 0;

				to.next = TextureObject.get();
				to = to.next;

				mCanvas.setBitmap(to.bitmap);

				x = 0;
				y = 0;
				advanceY = (int) height;
			}

			if (it.bitmap != null) {
				mCanvas.drawBitmap(it.bitmap, x, y, null);
			} else {
				it.drawable.copyBounds(mRect);
				it.drawable.setBounds((int) x, (int) y, (int) (x + width), (int) (y + height));
				it.drawable.draw(mCanvas);
				it.drawable.setBounds(mRect);
			}

			short x1, y1, x2, y2;

			if (it.bitmap != null) {
				float hw = width / 2.0f;
				float hh = height / 2.0f;
				x1 = (short) (SCALE * (-hw));
				x2 = (short) (SCALE * (hw));
				y1 = (short) (SCALE * (hh));
				y2 = (short) (SCALE * (-hh));
			} else {
				// use drawable offsets (for marker hotspot)
				x2 = (short) (SCALE * (mRect.left));
				y2 = (short) (SCALE * (mRect.top));
				x1 = (short) (SCALE * (mRect.right));
				y1 = (short) (SCALE * (mRect.bottom));

			}

			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add symbol items referencing the same bitmap / drawable
			for (SymbolItem it2 = it;; it2 = it2.next) {

				if (it2 == null
						|| (it.drawable != null && it2.drawable != it.drawable)
						|| (it.bitmap != null && it2.bitmap != it.bitmap)) {
					it = it2;
					break;
				}

				// add vertices
				short tx = (short) ((int) (SCALE * it2.x) & LBIT_MASK | (it2.billboard ? 1 : 0));
				short ty = (short) (SCALE * it2.y);

				if (pos == VertexPoolItem.SIZE) {
					si.used = VertexPoolItem.SIZE;
					si = si.next = VertexPool.get();
					buf = si.vertices;
					pos = 0;
				}

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
				buf[pos++] = y1;
				buf[pos++] = u2;
				buf[pos++] = v2;
				// bot-right
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x2;
				buf[pos++] = y2;
				buf[pos++] = u2;
				buf[pos++] = v1;
				// bot-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y2;
				buf[pos++] = u1;
				buf[pos++] = v1;

				// six elements used to draw the four vertices
				curIndices += TextureRenderer.INDICES_PER_SPRITE;
			}
			x += width;
		}

		to.offset = offsetIndices;
		to.vertices = curIndices;

		si.used = pos;
		curItem = si;

		return true;
	}

	@Override
	protected void clear() {
		TextureObject.release(textures);
		SymbolItem.release(symbols);
		VertexPool.release(pool);
		textures = null;
		symbols = null;
		pool = null;
		verticesCnt = 0;
	}
}
