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
package org.oscim.renderer;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureObject {
	private static TextureObject pool;

	// shared bitmap and canvas for default texture size
	public final static int TEXTURE_WIDTH = 256;
	public final static int TEXTURE_HEIGHT = 256;
	private static Bitmap[] mBitmap;
	private static Canvas[] mCanvas;
	private static int mBitmapFormat;
	private static int mBitmapType;
	private static int objectCount = 10;

	public static synchronized TextureObject get() {
		TextureObject to;

		if (pool == null) {
			init(10);
			objectCount += 10;
			Log.d("...", "textures: " + objectCount);
		}

		to = pool;
		pool = pool.next;
		to.next = null;
		return to;
	}

	public static synchronized void release(TextureObject to) {

		while (to != null) {
			TextureObject next = to.next;

			to.next = pool;
			pool = to;

			to = next;
		}
	}

	public static void uploadTexture(TextureObject to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d("...", "no texture!");
			return;
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, to.id);
		if (to.width == w && to.height == h)
			GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);
		else {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, format, bitmap, type, 0);
			to.width = w;
			to.height = h;
		}
	}

	static void init(int num) {
		TextureObject to;

		int[] textureIds = new int[num];
		GLES20.glGenTextures(num, textureIds, 0);

		for (int i = 1; i < num; i++) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[i]);

			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
					GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
					GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
					GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping

			to = new TextureObject(textureIds[i]);
			to.next = pool;
			pool = to;
		}

		mBitmap = new Bitmap[4];
		mCanvas = new Canvas[4];

		for (int i = 0; i < 4; i++) {
			mBitmap[i] = Bitmap.createBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT,
					Bitmap.Config.ARGB_8888);
			mCanvas[i] = new Canvas(mBitmap[i]);
		}
		mBitmapFormat = GLUtils.getInternalFormat(mBitmap[0]);
		mBitmapType = GLUtils.getType(mBitmap[0]);
	}

	private static int curCanvas = 0;

	public static Canvas getCanvas() {
		curCanvas = ++curCanvas % 4;

		mBitmap[curCanvas].eraseColor(Color.TRANSPARENT);

		return mCanvas[curCanvas];
	}

	public static TextureObject uploadCanvas(short offset, short indices) {
		TextureObject to = get();
		uploadTexture(to, mBitmap[curCanvas],
				mBitmapFormat, mBitmapType,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);

		to.offset = offset;
		to.vertices = (short) (indices - offset);

		return to;
	}

	public TextureObject next;

	int id;
	int width;
	int height;

	// vertex offset from which this texture is referenced
	// or store texture id with vertex?
	short offset;
	short vertices;

	TextureObject(int id) {
		this.id = id;
	}
}
