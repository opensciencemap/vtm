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

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

public class TextureObject {
	private final static String TAG = TextureObject.class.getName();

	private static TextureObject pool;
	private static int poolCount;

	private static ArrayList<Bitmap> mBitmaps;

	public final static int TEXTURE_WIDTH = 256;
	public final static int TEXTURE_HEIGHT = 256;

	private static int mBitmapFormat;
	private static int mBitmapType;

	/**
	 * Get a TextureObject with Bitmap to draw to.
	 * 'uploadTexture()' uploads the Bitmap as texture.
	 *
	 * @return obtained TextureObject
	 */
	public static synchronized TextureObject get() {
		TextureObject to;

		if (pool == null) {
			poolCount += 1;
			if (TextureRenderer.debug)
				Log.d(TAG, "textures: " + poolCount);
			pool = new TextureObject(-1);
		}

		to = pool;
		pool = pool.next;

		to.next = null;

		to.bitmap = getBitmap();
		to.bitmap.eraseColor(Color.TRANSPARENT);

		if (TextureRenderer.debug)
			Log.d(TAG, "get texture " + to.id + " " + to.bitmap);

		return to;
	}

	public static synchronized void release(TextureObject to) {
		while (to != null) {
			if (TextureRenderer.debug)
				Log.d(TAG, "release texture " + to.id);

			TextureObject next = to.next;

			if (to.bitmap != null) {
				mBitmaps.add(to.bitmap);
				to.bitmap = null;
			}

			to.next = pool;
			pool = to;

			to = next;
		}
	}

	/**
	 * This function may only be used in GLRenderer Thread.
	 *
	 * @param to the TextureObjet to compile and upload
	 */
	public static synchronized void uploadTexture(TextureObject to) {
		if (TextureRenderer.debug)
			Log.d(TAG, "upload texture " + to.id);

		if (to.id < 0) {
			int[] textureIds = new int[1];
			GLES20.glGenTextures(1, textureIds, 0);
			to.id = textureIds[0];
			initTexture(to.id);
			if (TextureRenderer.debug)
				Log.d(TAG, "new texture " + to.id);
		}

		uploadTexture(to, to.bitmap, mBitmapFormat, mBitmapType,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);

		mBitmaps.add(to.bitmap);
		to.bitmap = null;
	}

	public static void uploadTexture(TextureObject to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d(TAG, "no texture!");
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

	static void initTexture(int id) {
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);

		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
				GLES20.GL_LINEAR);
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
				GLES20.GL_CLAMP_TO_EDGE); // Set U Wrapping
		GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
				GLES20.GL_CLAMP_TO_EDGE); // Set V Wrapping
	}

	static void init(int num) {
		pool = null;
		poolCount = num;

		int[] textureIds = new int[num];
		GLES20.glGenTextures(num, textureIds, 0);

		for (int i = 1; i < num; i++) {
			initTexture(textureIds[i]);
			TextureObject to = new TextureObject(textureIds[i]);

			to.next = pool;
			pool = to;
		}

		mBitmaps = new ArrayList<Bitmap>(10);

		for (int i = 0; i < 4; i++) {
			Bitmap bitmap = Bitmap.createBitmap(
					TEXTURE_WIDTH, TEXTURE_HEIGHT,
					Bitmap.Config.ARGB_8888);

			mBitmaps.add(bitmap);
		}

		mBitmapFormat = GLUtils.getInternalFormat(mBitmaps.get(0));
		mBitmapType = GLUtils.getType(mBitmaps.get(0));
	}

	private static Bitmap getBitmap() {
		int size = mBitmaps.size();
		if (size == 0) {
			Bitmap bitmap = Bitmap.createBitmap(
					TEXTURE_WIDTH, TEXTURE_HEIGHT,
					Bitmap.Config.ARGB_8888);

			return bitmap;
		}
		return mBitmaps.remove(size - 1);
	}

	// -----------------------
	public TextureObject next;

	//  texture ID
	int id;

	int width;
	int height;

	// vertex offset from which this texture is referenced
	public short offset;
	public short vertices;

	// temporary Bitmap
	public Bitmap bitmap;

	TextureObject(int id) {
		this.id = id;
	}
}
