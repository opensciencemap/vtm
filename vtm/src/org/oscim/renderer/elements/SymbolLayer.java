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
package org.oscim.renderer.elements;

import java.nio.ShortBuffer;

import org.oscim.backend.Log;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.atlas.TextureAtlas;
import org.oscim.utils.pool.Inlist;

public final class SymbolLayer extends TextureLayer {
	private final static String TAG = SymbolLayer.class.getName();

	private final static float SCALE = 8.0f;
	private final static int VERTICES_PER_SPRITE = 4;
	private final static int LBIT_MASK = 0xfffffffe;

	private TextureItem prevTextures;
	private SymbolItem symbols;

	public SymbolLayer() {
		super(RenderElement.SYMBOL);
		fixed = true;
	}

	// TODO move sorting items to 'prepare'
	public void addSymbol(SymbolItem item) {

		// needed to calculate 'sbuf' size for compile
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

	@Override
	protected void compile(ShortBuffer sbuf) {
		// offset of layer data in vbo
		this.offset = sbuf.position() * 2; //SHORT_BYTES;

		short numIndices = 0;

		VertexItem si = VertexItem.pool.get();

		int pos = 0;
		short buf[] = si.vertices;

		prevTextures = textures;
		textures = null;
		TextureItem to = null;

		for (SymbolItem it = symbols; it != null;) {
			int width = 0, height = 0;
			int x = 0;
			int y = 0;

			if (it.texRegion != null) {

				// FIXME this work only with one TextureAtlas per
				// SymbolLayer.
				if (textures == null) {
					to = it.texRegion.atlas.loadTexture();
					// clone TextureItem to use same texID with
					// multiple TextureItem
					to = TextureItem.clone(to);
					textures = Inlist.appendItem(textures, to);
				}

				TextureAtlas.Rect r = it.texRegion.rect;
				x = r.x;
				y = r.y;
				width = r.w;
				height = r.h;

			} else if (it.bitmap != null) {
				width = it.bitmap.getWidth();
				height = it.bitmap.getHeight();
				to = getTexture(it.bitmap);
				if (to == null) {
					to = new TextureItem(it.bitmap);
					textures = Inlist.appendItem(textures, to);

					to.upload();
				}
				to.offset = numIndices;
				to.vertices = 0;
			}

			if (to == null) {
				Log.d(TAG, "Bad SymbolItem");
				continue;
			}

			short x1, y1, x2, y2;

			if (it.offset == null) {
				float hw = width / 2f;
				float hh = height / 2f;

				x1 = (short) (SCALE * (-hw));
				x2 = (short) (SCALE * (hw));
				y1 = (short) (SCALE * (hh));
				y2 = (short) (SCALE * (-hh));
			} else {
				float hw = (float) (it.offset.x * width);
				float hh = (float) (it.offset.y * height);
				x1 = (short) (SCALE * (-hw));
				x2 = (short) (SCALE * (width - hw));
				y1 = (short) (SCALE * (height - hh));
				y2 = (short) (SCALE * (-hh));
			}

			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add symbol items referencing the same bitmap /
			for (SymbolItem it2 = it;; it2 = it2.next) {

				if (it2 == null
				        || (it.bitmap != null && it2.bitmap != it.bitmap)
				        || (it.texRegion != null && it2.texRegion != it.texRegion)) {
					it = it2;
					break;
				}

				// add vertices
				short tx = (short) ((int) (SCALE * it2.x) & LBIT_MASK
				        | (it2.billboard ? 1 : 0));

				short ty = (short) (SCALE * it2.y);

				if (pos == VertexItem.SIZE) {
					sbuf.put(buf, 0, VertexItem.SIZE);
					pos = 0;
				}

				TextureLayer.putSprite(buf, pos, tx, ty,
				                       x1, y1, x2, y2, u1, v1, u2, v2);

				// TextureRenderer.VERTICES_PER_SPRITE
				// * TextureRenderer.SHORTS_PER_VERTICE;
				pos += 24;

				// six elements used to draw the four vertices
				to.vertices += TextureLayer.Renderer.INDICES_PER_SPRITE;
			}
			numIndices += to.vertices;
		}

		if (pos > 0)
			sbuf.put(buf, 0, pos);

		VertexItem.pool.release(si);

		TextureItem.releaseAll(prevTextures);
		prevTextures = null;
	}

	private TextureItem getTexture(Bitmap bitmap) {
		TextureItem to;

		for (to = prevTextures; to != null; to = to.next) {
			if (to.bitmap == bitmap) {
				prevTextures = Inlist.remove(prevTextures, to);
				textures = Inlist.appendItem(textures, to);
				break;
			}
		}

		return to;
	}

	public void clearItems() {
		SymbolItem.pool.releaseAll(symbols);
		symbols = null;
		verticesCnt = 0;
	}

	@Override
	public void clear() {
		TextureItem.releaseAll(textures);
		SymbolItem.pool.releaseAll(symbols);

		textures = null;
		symbols = null;
		vertexItems = null;
		verticesCnt = 0;
	}

	@Override
	public boolean prepare() {
		return true;
	}
}
