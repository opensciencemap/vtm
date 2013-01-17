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
import android.util.Log;

public final class TextLayer extends TextureLayer {

	//private static String TAG = TextureLayer.class.getName();

	private final static int TEXTURE_WIDTH = TextureObject.TEXTURE_WIDTH;
	private final static int TEXTURE_HEIGHT = TextureObject.TEXTURE_HEIGHT;
	private final static float SCALE = 8.0f;
	private final static int LBIT_MASK = 0xfffffffe;

	private static int mFontPadX = 1;
	private static int mFontPadY = 1;

	public TextItem labels;
	private Canvas mCanvas;
	private float mScale;

	public TextItem getLabels() {
		return labels;
	}

	public TextLayer() {
		type = Layer.SYMBOL;
		mCanvas = new Canvas();
		fixed = true;
		mScale = 1;
	}

	public void setScale(float scale) {
		mScale = scale;
	}

	public boolean removeText(TextItem item) {
		TextItem prev = null;

		for (TextItem it = labels; it != null; it = it.next) {
			if (it == item) {
				if (prev == null)
					labels = it.next;
				else
					prev.next = it.next;

				return true;
			}
			prev = it;
		}

		return false;
	}

	public void addText(TextItem item) {
		TextItem it = labels;

		for (; it != null; it = it.next) {
			// todo add captions at the end
			//if (item.text.caption && !it.text.caption)
			//continue;
			//if (!item.text.caption && it.text.caption)
			//continue;

			if (item.text == it.text) {
				while (it.next != null
						// break if next item uses different text style
						&& item.text == it.next.text
						// check same string instance
						&& item.string != it.string
						// check same string
						&& !item.string.equals(it.string))
					it = it.next;

				// unify duplicate string :)
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
		//		for (it = labels; it != null; it = it.next)
		//			Log.d(TAG, "> " + it.text + " " + it.string);
		//		Log.d(TAG, "< ");
	}

	@Override
	public boolean prepare() {
		if (TextureRenderer.debug)
			Log.d("...", "prepare");

		short numIndices = 0;
		short offsetIndices = 0;

		VertexPoolItem vi = pool = VertexPool.get();
		int pos = vi.used; // 0
		short buf[] = vi.vertices;

		verticesCnt = 0;

		int advanceY = 0;
		float x = 0;
		float y = 0;
		float yy;

		TextureObject to = TextureObject.get();
		textures = to;
		mCanvas.setBitmap(to.bitmap);

		for (TextItem it = labels; it != null;) {

			float width = it.width + 2 * mFontPadX;
			float height = (int) (it.text.fontHeight) + 2 * mFontPadY + 0.5f;

			if (height > advanceY)
				advanceY = (int) height;

			if (x + width > TEXTURE_WIDTH) {
				x = 0;
				y += advanceY;
				advanceY = (int) (height + 0.5f);

				if (y + height > TEXTURE_HEIGHT) {
					// Log.d(TAG, "reached max labels " + numTextures + " " +
					// numLabel + " "
					// + ((numIndices - offsetIndices) / 6));

					to.offset = offsetIndices;
					to.vertices = (short) (numIndices - offsetIndices);
					offsetIndices = numIndices;

					to.next = TextureObject.get();
					to = to.next;

					mCanvas.setBitmap(to.bitmap);

					x = 0;
					y = 0;
					advanceY = (int) height;
				}
			}

			yy = y + (height - 1) - it.text.fontDescent - mFontPadY;

			if (it.text.stroke != null)
				mCanvas.drawText(it.string, x + it.width / 2, yy, it.text.stroke);

			mCanvas.drawText(it.string, x + it.width / 2, yy, it.text.paint);

			// FIXME !!!
			if (width > TEXTURE_WIDTH)
				width = TEXTURE_WIDTH;

			float hw = width / 2.0f;
			float hh = height / 2.0f;

			float hh2 = 0;
			if (!it.text.caption) {
				hw /= mScale;
				hh2 = hh + it.text.fontDescent / 2;
				hh -= it.text.fontDescent / 2;
				hh /= mScale;
				hh2 /= mScale;
			}

			// texture coordinates
			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add symbol items referencing the same bitmap / drawable
			for (TextItem it2 = it;; it2 = it2.next) {

				if (it != it2) {
					if (it2 == null
							|| (it2.text != it.text)
							|| (it2.string != it.string)) {
						it = it2;
						break;
					}
					//Log.d(TAG, "pack strings: " + it.string);
				}

				short x1, x2, x3, x4, y1, y3, y2, y4;

				if (it.text.caption) {
					x1 = x3 = (short) (SCALE * -hw);
					x2 = x4 = (short) (SCALE * hw);
					y1 = y2 = (short) (SCALE * hh);
					y3 = y4 = (short) (SCALE * -hh);
					// x1 = x3 = (short) (0);
					// x2 = x4 = (short) (SCALE * width);
				} else {
					float vx = it2.x1 - it2.x2;
					float vy = it2.y1 - it2.y2;
					float a = (float) Math.sqrt(vx * vx + vy * vy);
					vx = vx / a;
					vy = vy / a;

					float ux = -vy;
					float uy = vx;

					x1 = (short) (SCALE * (vx * hw - ux * hh));
					y1 = (short) (SCALE * (vy * hw - uy * hh));
					x2 = (short) (SCALE * (-vx * hw - ux * hh));
					y2 = (short) (SCALE * (-vy * hw - uy * hh));
					x4 = (short) (SCALE * (-vx * hw + ux * hh2));
					y4 = (short) (SCALE * (-vy * hw + uy * hh2));
					x3 = (short) (SCALE * (vx * hw + ux * hh2));
					y3 = (short) (SCALE * (vy * hw + uy * hh2));
				}

				// add vertices
				int tmp = (int) (SCALE * it2.x) & LBIT_MASK;
				short tx = (short) (tmp | (it2.text.caption ? 1 : 0));
				short ty = (short) (SCALE * it2.y);

				if (pos == VertexPoolItem.SIZE) {
					vi.used = VertexPoolItem.SIZE;
					vi = vi.next = VertexPool.get();
					buf = vi.vertices;
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
				// bot-left
				buf[pos++] = tx;
				buf[pos++] = ty;
				buf[pos++] = x3;
				buf[pos++] = y3;
				buf[pos++] = u1;
				buf[pos++] = v1;

				// six indices to draw the four vertices
				numIndices += TextureRenderer.INDICES_PER_SPRITE;
				verticesCnt += 4;

				// FIXME this does not work, need to draw bitmap on next
				// texture...
				// if (numLabel == TextureRenderer.MAX_ITEMS) {
				// Log.d(TAG, "--- reached max label per texture " + numLabel);
				// sbuf.put(buf, 0, pos);
				// pos = 0;
				// }

			}
			x += width;
		}

		vi.used = pos;

		to.offset = offsetIndices;
		to.vertices = (short) (numIndices - offsetIndices);

		//	Log.d(TAG, "added labels " + numTextures + " " + numLabel);

		return true;
	}

	@Override
	protected void clear() {
		TextureObject.release(textures);
		TextItem.release(labels);
		VertexPool.release(pool);
		textures = null;
		labels = null;
		pool = null;
		verticesCnt = 0;
	}
}
