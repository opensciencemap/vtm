/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import java.util.ArrayList;

import org.oscim.renderer.TextureRenderer;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class TextureItem extends Inlist<TextureItem> {
	private final static String TAG = TextureItem.class.getName();

	//  texture ID
	public int id;

	int width;
	int height;

	// vertex offset from which this texture is referenced
	public short offset;
	public short vertices;

	// temporary Bitmap
	public Bitmap bitmap;

	TextureItem(int id) {
		this.id = id;
	}

	public final static SyncPool<TextureItem> pool = new SyncPool<TextureItem>() {

		@Override
		public void init(int num) {
			this.pool = null;

			int[] textureIds = new int[num];
			GLES20.glGenTextures(num, textureIds, 0);

			for (int i = 1; i < num; i++) {
				initTexture(textureIds[i]);
				TextureItem to = new TextureItem(textureIds[i]);

				to.next = this.pool;
				this.pool = to;
			}
		}

		@Override
		public TextureItem get() {
			TextureItem it = super.get();

			it.bitmap = TextureItem.getBitmap();
			it.bitmap.eraseColor(Color.TRANSPARENT);

			return it;
		}

		@Override
		protected TextureItem createItem() {
			return new TextureItem(-1);
		}

		@Override
		protected void clearItem(TextureItem it) {
			TextureItem.releaseBitmap(it);
		}
	};

	private static ArrayList<Bitmap> mBitmaps;

	public final static int TEXTURE_WIDTH = 512;
	public final static int TEXTURE_HEIGHT = 256;

	private static int mBitmapFormat;
	private static int mBitmapType;

	/**
	 * This function may only be used in GLRenderer Thread.
	 *
	 * @param to the TextureObjet to compile and upload
	 */
	public static void uploadTexture(TextureItem to) {

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

		TextureItem.releaseBitmap(to);
	}

	public static void uploadTexture(TextureItem to, Bitmap bitmap,
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

	public static void init(int num) {
		pool.init(num);

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

	static Bitmap getBitmap() {
		synchronized (mBitmaps) {

			int size = mBitmaps.size();
			if (size == 0) {
				Bitmap bitmap = Bitmap.createBitmap(
						TEXTURE_WIDTH, TEXTURE_HEIGHT,
						Bitmap.Config.ARGB_8888);

				if (TextureRenderer.debug)
					Log.d(TAG, "alloc bitmap: " +
							android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024));

				return bitmap;
			}
			return mBitmaps.remove(size - 1);
		}
	}

	static void releaseBitmap(TextureItem it) {
		synchronized (mBitmaps) {

			if (it.bitmap != null) {
				mBitmaps.add(it.bitmap);
				it.bitmap = null;
			}
		}
	}
}
