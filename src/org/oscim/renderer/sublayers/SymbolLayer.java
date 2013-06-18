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
package org.oscim.renderer.sublayers;

import java.nio.ShortBuffer;

import org.oscim.utils.pool.Inlist;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;

// TODO share one static texture for all poi map symabols

public final class SymbolLayer extends TextureLayer {
	private final static String TAG = SymbolLayer.class.getSimpleName();

	private final static float SCALE = 8.0f;
	private final static int VERTICES_PER_SPRITE = 4;

	private SymbolItem symbols;

	public SymbolLayer() {
		type = Layer.SYMBOL;
		fixed = true;
	}

	// TODO move sorting items to 'prepare'
	public void addSymbol(SymbolItem item) {

		verticesCnt += VERTICES_PER_SPRITE;

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

		verticesCnt += VERTICES_PER_SPRITE;

		SymbolItem item = SymbolItem.pool.get();
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

	@Override
	public boolean prepare() {

		return true;
	}

	@Override
	protected void compile(ShortBuffer sbuf) {
		// offset of layer data in vbo
		this.offset = sbuf.position() * 2; //SHORT_BYTES;

		short numIndices = 0;
		//short offsetIndices = 0;
		//short curIndices = 0;

		//curItem =
		//vertexItems = curItem;
		VertexItem si = VertexItem.pool.get();

		int pos = 0;
		short buf[] = si.vertices;

		//int advanceY = 0;
		final float x = 0;
		final float y = 0;

		TextureItem prevTextures = textures;
		//TextureItem prev = textures;

		textures = null;
		TextureItem to = null;

		//TextureItem to = TextureItem.get(true);
		//textures = to;
		//mCanvas.setBitmap(to.bitmap);

		for (SymbolItem it = symbols; it != null;) {
			int width, height;

			if (it.bitmap != null) {
				// add bitmap
				width = it.bitmap.getWidth();
				height = it.bitmap.getHeight();
			} else {
				width = it.drawable.getIntrinsicWidth();
				height = it.drawable.getIntrinsicHeight();
			}

			for (to = prevTextures; to != null; to = to.next){
				if (to.bitmap == it.bitmap){
					prevTextures = Inlist.remove(prevTextures, to);
					textures = Inlist.append(textures, to);
					break;
				}
			}

			if (to == null){
				to = TextureItem.get(false);
				to.bitmap = it.bitmap;
				to.width = width;
				to.height= height;
				textures = Inlist.append(textures, to);

				TextureItem.uploadTexture(to);
			}

			to.offset = numIndices;
			to.vertices = 0;

			short x1, y1, x2, y2;

			if (it.bitmap != null) {
				float hw = width / 2f;
				float hh = height / 2f;
				x1 = (short) (SCALE * (-hw));
				x2 = (short) (SCALE * (hw));
				y1 = (short) (SCALE * (hh));
				y2 = (short) (SCALE * (-hh));
			} else {
				// use drawable offsets (for marker hotspot)
				Rect mRect = it.drawable.getBounds();
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

				if (pos == VertexItem.SIZE) {
					sbuf.put(buf, 0, VertexItem.SIZE);
					pos = 0;
				}

				// top-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y1;
				buf[pos++] = u1;
				buf[pos++] = v2;
				// bot-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x1;
				buf[pos++] = y2;
				buf[pos++] = u1;
				buf[pos++] = v1;
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

				// six elements used to draw the four vertices
				to.vertices += TextureRenderer.INDICES_PER_SPRITE;
			}

			numIndices += to.vertices;
			//offsetIndices = numIndices;

			//to.offset = offsetIndices;
			 //= curIndices;
			//x += width;

		}
//		if (to != null) {
//			to.offset = offsetIndices;
//			to.vertices = curIndices;
//		}
		//si.used = pos;
		//curItem = si;

		if (pos > 0)
			sbuf.put(buf, 0, pos);

		VertexItem.pool.release(si);

		TextureItem.releaseAll(prevTextures);
		prevTextures = null;
	}


	@Override
	protected void clear() {

		TextureItem.releaseAll(textures);
		SymbolItem.pool.releaseAll(symbols);

		//VertexItem.pool.releaseAll(vertexItems);

		textures = null;
		symbols = null;
		vertexItems = null;
		verticesCnt = 0;
	}
}
