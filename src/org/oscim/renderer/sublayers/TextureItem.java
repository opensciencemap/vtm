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
package org.oscim.renderer.sublayers;

import java.util.ArrayList;

import org.oscim.utils.GlUtils;
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

	boolean ownBitmap;

	TextureItem(int id) {
		this.id = id;
	}

	public synchronized static void releaseAll(TextureItem ti) {
		pool.releaseAll(ti);
	}

	public synchronized static TextureItem get(boolean initBitmap) {
		TextureItem ti = pool.get();
		if (initBitmap) {
			ti.bitmap = getBitmap();
			ti.bitmap.eraseColor(Color.TRANSPARENT);
		}
		return ti;
	}

	private final static SyncPool<TextureItem> pool = new SyncPool<TextureItem>(20) {

		@Override
		public void init(int num) {
			super.init(num);

			int[] textureIds = new int[num];
			GLES20.glGenTextures(num, textureIds, 0);

			for (int i = 1; i < num; i++) {
				initTexture(textureIds[i]);
				TextureItem to = new TextureItem(textureIds[i]);
				pool = Inlist.push(pool, to);
			}
		}

		@Override
		protected TextureItem createItem() {
			return new TextureItem(-1);
		}

		@Override
		protected void clearItem(TextureItem it) {
			//Log.d(TAG, it.ownBitmap + " " + (it.bitmap == null));
			if (it.ownBitmap)
				return;

			releaseBitmap(it);
		}

		@Override
		protected void freeItem(TextureItem it) {
			it.width = -1;
			it.height = -1;
			releaseTexture(it);
		}
	};

	private static ArrayList<Integer> mFreeTextures = new ArrayList<Integer>();
	private static ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>(10);

	public final static int TEXTURE_WIDTH = 512;
	public final static int TEXTURE_HEIGHT = 256;

	private static int mBitmapFormat;
	private static int mBitmapType;
	private static int mTexCnt = 0;

	static void releaseTexture(TextureItem it) {
		synchronized (mFreeTextures) {
			if (it.id >= 0) {
				mFreeTextures.add(Integer.valueOf(it.id));
				it.id = -1;
			}
		}
	}

	static void releaseBitmap(TextureItem it) {
		synchronized (mBitmaps) {
			if (it.bitmap != null) {
				mBitmaps.add(it.bitmap);
				it.bitmap = null;
			}
			it.ownBitmap = false;

		}
	}

	/**
	 * This function may only be used in GLRenderer Thread.
	 *
	 * @param to the TextureObjet to compile and upload
	 */
	public static void uploadTexture(TextureItem to) {

		// free unused textures, find a better place for this TODO
		synchronized (mFreeTextures) {
			int size = mFreeTextures.size();
			int[] tmp = new int[size];
			for (int i = 0; i < size; i++)
				tmp[i] = mFreeTextures.get(i).intValue();
			mFreeTextures.clear();
			GLES20.glDeleteTextures(size, tmp, 0);
		}

		if (to.id < 0) {
			mTexCnt++;
			int[] textureIds = new int[1];
			GLES20.glGenTextures(1, textureIds, 0);
			to.id = textureIds[0];
			initTexture(to.id);
			if (TextureRenderer.debug)
				Log.d(TAG, pool.getCount() + " " + pool.getFill()
						+ " " + mTexCnt + " new texture " + to.id);
		}

		uploadTexture(to, to.bitmap, mBitmapFormat, mBitmapType,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (!to.ownBitmap)
			TextureItem.releaseBitmap(to);
	}

	public static void uploadTexture(TextureItem to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d(TAG, "no texture!");
			return;
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, to.id);

		if (to.ownBitmap) {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		} else if (to.width == w && to.height == h) {
			GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);

		} else {
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, format, bitmap, type, 0);
			to.width = w;
			to.height = h;
		}

		if (TextureRenderer.debug)
			GlUtils.checkGlError(TAG);
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

		mTexCnt = num;
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
}
