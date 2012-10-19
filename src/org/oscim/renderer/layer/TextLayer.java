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
import org.oscim.renderer.TextureRenderer;

import android.graphics.Canvas;
import android.util.FloatMath;
import android.util.Log;

public final class TextLayer extends TextureLayer {

	private static String TAG = TextureLayer.class.getSimpleName();

	private final static int TEXTURE_WIDTH = TextureObject.TEXTURE_WIDTH;
	private final static int TEXTURE_HEIGHT = TextureObject.TEXTURE_HEIGHT;
	private final static float SCALE = 8.0f;
	private final static int LBIT_MASK = 0xfffffffe;

	private static short[] mVertices;
	private static int mFontPadX = 1;
	private static int mFontPadY = 1;

	TextItem labels;

	public TextItem getLabels() {
		return labels;
	}

	public TextLayer() {
		if (mVertices == null)
			mVertices = new short[TextureRenderer.MAX_ITEMS * 24];
	}

	public void addText(TextItem item) {
		verticesCnt += 4;
		TextItem it = labels;

		for (; it != null; it = it.next) {
			if (it.text == item.text) {
				item.next = it.next;
				it.next = item;
				return;
			}
		}

		item.next = labels;
		labels = item;
	}

	@Override
	public void compile(ShortBuffer sbuf) {
		int numLabel = 0;
		// int numTextures = 0;
		short numIndices = 0;
		short offsetIndices = 0;

		int pos = 0;
		short buf[] = mVertices;
		int bufLen = buf.length;

		int advanceY = 0;
		float x = 0;
		float y = 0;
		float yy;

		Canvas canvas = TextureObject.getCanvas();
		for (TextItem it = labels; it != null; it = it.next) {
			numLabel++;

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

					// need to sync bitmap upload somehow???
					TextureObject to = TextureObject.uploadCanvas(offsetIndices, numIndices);
					offsetIndices = numIndices;

					to.next = textures;
					textures = to;

					sbuf.put(buf, 0, pos);
					pos = 0;

					x = 0;
					y = 0;
					advanceY = (int) height;

					// clear bitmap, TODO rotate two canvas to reduce the chance
					// of having upload lock draing to the canvas?
					canvas = TextureObject.getCanvas();
					// numTextures++;
				}
			}

			yy = y + (height - 1) - it.text.fontDescent - mFontPadY;

			if (it.text.stroke != null)
				canvas.drawText(it.string, x + it.width / 2, yy, it.text.stroke);

			canvas.drawText(it.string, x + it.width / 2, yy, it.text.paint);

			// FIXME !!!
			if (width > TEXTURE_WIDTH)
				width = TEXTURE_WIDTH;

			float hw = width / 2.0f;
			float hh = height / 2.0f;

			short x1, x2, x3, x4, y1, y3, y2, y4;

			if (it.text.caption) {
				x1 = x3 = (short) (SCALE * -hw);
				x2 = x4 = (short) (SCALE * hw);
				y1 = y2 = (short) (SCALE * hh);
				y3 = y4 = (short) (SCALE * -hh);
				// x1 = x3 = (short) (0);
				// x2 = x4 = (short) (SCALE * width);
			} else {
				float vx = it.x1 - it.x2;
				float vy = it.y1 - it.y2;
				float a = FloatMath.sqrt(vx * vx + vy * vy);
				vx = vx / a;
				vy = vy / a;

				float ux = -vy;
				float uy = vx;
				float hh2 = hh + it.text.fontDescent / 2;
				hh -= it.text.fontDescent / 2;
				x1 = (short) (SCALE * (vx * hw - ux * hh));
				y1 = (short) (SCALE * (vy * hw - uy * hh));
				x2 = (short) (SCALE * (-vx * hw - ux * hh));
				y2 = (short) (SCALE * (-vy * hw - uy * hh));
				x4 = (short) (SCALE * (-vx * hw + ux * hh2));
				y4 = (short) (SCALE * (-vy * hw + uy * hh2));
				x3 = (short) (SCALE * (vx * hw + ux * hh2));
				y3 = (short) (SCALE * (vy * hw + uy * hh2));
			}

			short u1 = (short) (SCALE * x);
			short v1 = (short) (SCALE * y);
			short u2 = (short) (SCALE * (x + width));
			short v2 = (short) (SCALE * (y + height));

			// add vertices
			int tmp = (int) (SCALE * it.x) & LBIT_MASK;
			short tx = (short) (tmp | (it.text.caption ? 1 : 0));

			short ty = (short) (SCALE * it.y);

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
			numIndices += 6;

			// FIXME this does not work, need to draw bitmap on next
			// texture...
			if (pos == bufLen) {
				Log.d(TAG, "--- reached max label per texture " + numLabel);
				sbuf.put(buf, 0, pos);
				pos = 0;
			}

			x += width;
		}

		TextureObject to = TextureObject.uploadCanvas(offsetIndices, numIndices);

		to.next = textures;
		textures = to;

		sbuf.put(buf, 0, pos);

		// Log.d(TAG, "added labels " + numTextures + " " + numLabel);
	}

	@Override
	protected void clear() {
		TextureObject.release(textures);
		TextItem.release(labels);
	}
}
