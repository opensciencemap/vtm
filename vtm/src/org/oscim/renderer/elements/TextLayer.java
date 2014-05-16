/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Canvas;

public final class TextLayer extends TextureLayer {
	//static final Logger log = LoggerFactory.getLogger(TextureLayer.class);

	private final static int LBIT_MASK = 0xfffffffe;

	private static int mFontPadX = 1;
	//private static int mFontPadY = 1;

	public TextItem labels;
	private final Canvas mCanvas;

	public TextItem getLabels() {
		return labels;
	}

	public TextLayer() {
		super(RenderElement.SYMBOL);
		mCanvas = CanvasAdapter.g.getCanvas();
		fixed = true;
	}

	public void addText(TextItem item) {
		TextItem it = labels;

		for (; it != null; it = it.next) {

			if (item.text == it.text) {
				while (it.next != null
				        // break if next item uses different text style
				        && item.text == it.next.text
				        // check same string instance
				        && item.string != it.string
				        // check same string
				        && !item.string.equals(it.string))
					it = it.next;

				// unify duplicate string
				// Note: this is required for 'packing test' in prepare to work!
				if (item.string != it.string && item.string.equals(it.string))
					item.string = it.string;

				// insert after text of same type and/or before same string
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = labels;
		labels = item;
	}

	@Override
	public boolean prepare() {

		short numIndices = 0;
		short offsetIndices = 0;

		VertexItem vi = vertexItems = VertexItem.pool.get();
		int pos = vi.used; // 0
		short buf[] = vi.vertices;

		numVertices = 0;

		int advanceY = 0;
		float x = 0;
		float y = 0;
		float yy;

		TextureItem t = pool.get();
		textures = t;
		mCanvas.setBitmap(t.bitmap);

		for (TextItem it = labels; it != null;) {

			float width = it.width + 2 * mFontPadX;
			float height = (int) (it.text.fontHeight) + 0.5f;

			if (height > TEXTURE_HEIGHT)
				height = TEXTURE_HEIGHT;

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = 0;
				y += advanceY;
				advanceY = (int) (height + 0.5f);

				if (y + height > TEXTURE_HEIGHT) {
					t.offset = offsetIndices;
					t.indices = (short) (numIndices - offsetIndices);
					offsetIndices = numIndices;

					t.next = pool.get();
					t = t.next;

					mCanvas.setBitmap(t.bitmap);

					x = 0;
					y = 0;
					advanceY = (int) height;
				}
			}

			//yy = y + (height - 1) - it.text.fontDescent - mFontPadY;
			yy = y + height - it.text.fontDescent; // - mFontPadY;

			if (it.text.stroke != null)
				mCanvas.drawText(it.string, x, yy, it.text.stroke);

			mCanvas.drawText(it.string, x, yy, it.text.paint);

			// FIXME !!!
			if (width > TEXTURE_WIDTH)
				width = TEXTURE_WIDTH;

			while (it != null) {
				if (pos == VertexItem.SIZE) {
					vi.used = VertexItem.SIZE;
					vi = VertexItem.pool.getNext(vi);
					buf = vi.vertices;
					pos = 0;
				}
				addItem(buf, pos, it, width, height, x, y);
				pos += 24;

				// six indices to draw the four vertices
				numIndices += TextureLayer.INDICES_PER_SPRITE;
				numVertices += 4;

				if (it.next == null
				        || (it.next.text != it.text)
				        || (it.next.string != it.string)) {
					it = it.next;
					break;
				}
				it = it.next;

			}
			x += width;
		}

		vi.used = pos;

		t.offset = offsetIndices;
		t.indices = (short) (numIndices - offsetIndices);

		return true;
	}

	void addItem(short[] buf, int pos, TextItem it, float width, float height, float x, float y) {
		// texture coordinates
		short u1 = (short) (COORD_SCALE * x);
		short v1 = (short) (COORD_SCALE * y);
		short u2 = (short) (COORD_SCALE * (x + width));
		short v2 = (short) (COORD_SCALE * (y + height));

		short x1, x2, x3, x4, y1, y3, y2, y4;
		float hw = width / 2.0f;
		float hh = height / 2.0f;
		if (it.text.caption) {
			x1 = x3 = (short) (COORD_SCALE * -hw);
			x2 = x4 = (short) (COORD_SCALE * hw);
			y1 = y2 = (short) (COORD_SCALE * (it.text.dy + hh));
			y3 = y4 = (short) (COORD_SCALE * (it.text.dy - hh));
		} else {
			float vx = it.x1 - it.x2;
			float vy = it.y1 - it.y2;
			float a = (float) Math.sqrt(vx * vx + vy * vy);
			vx = vx / a;
			vy = vy / a;

			float ux = -vy * hh;
			float uy = vx * hh;

			float ux2 = -vy * hh;
			float uy2 = vx * hh;

			vx *= hw;
			vy *= hw;

			// top-left
			x1 = (short) (COORD_SCALE * (vx - ux));
			y1 = (short) (COORD_SCALE * (vy - uy));
			// top-right
			x2 = (short) (COORD_SCALE * (-vx - ux));
			y2 = (short) (COORD_SCALE * (-vy - uy));
			// bot-right
			x4 = (short) (COORD_SCALE * (-vx + ux2));
			y4 = (short) (COORD_SCALE * (-vy + uy2));
			// bot-left
			x3 = (short) (COORD_SCALE * (vx + ux2));
			y3 = (short) (COORD_SCALE * (vy + uy2));
		}

		// add vertices
		int tmp = (int) (COORD_SCALE * it.x) & LBIT_MASK;
		short tx = (short) (tmp | (it.text.caption ? 1 : 0));
		short ty = (short) (COORD_SCALE * it.y);

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
		buf[pos++] = x3;
		buf[pos++] = y3;
		buf[pos++] = u1;
		buf[pos++] = v1;
		// top-right
		buf[pos++] = tx;
		buf[pos++] = ty;
		buf[pos++] = x2;
		buf[pos++] = y2;
		buf[pos++] = u2;
		buf[pos++] = v2;
		// bot-right
		buf[pos++] = tx;
		buf[pos++] = ty;
		buf[pos++] = x4;
		buf[pos++] = y4;
		buf[pos++] = u2;
		buf[pos++] = v1;
	}

	@Override
	public void clear() {
		// release textures
		super.clear();

		clearLabels();
		//labels = TextItem.pool.releaseAll(labels);
		//vertexItems = VertexItem.pool.releaseAll(vertexItems);
		//numVertices = 0;
	}

	public void clearLabels() {
		labels = TextItem.pool.releaseAll(labels);
	}
}
