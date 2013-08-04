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

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.utils.GlUtils;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

import android.opengl.GLES20;
import org.oscim.backend.Log;

// FIXME

public class TextureItem extends Inlist<TextureItem> {
	private final static String TAG = TextureItem.class.getName();

	//  texture ID
	public int id;

	public int width;
	public int height;

	// vertex offset from which this texture is referenced
	public short offset;
	public short vertices;

	// temporary Bitmap
	public Bitmap bitmap;

	// external bitmap (not from pool)
	boolean ownBitmap;

	// is only referencing a textureId, does not
	// release the texture when TextureItem is
	// released.
	boolean isClone;

	TextureItem(int id) {
		this.id = id;
	}

	TextureItem(TextureItem ti) {
		this.id = ti.id;
		this.width = ti.width;
		this.height = ti.height;
		this.isClone = true;
	}

	public TextureItem(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.id = -1;
		this.ownBitmap = true;
		this.width = bitmap.getWidth();
		this.height = bitmap.getHeight();
	}

	public synchronized static void releaseAll(TextureItem ti) {
		pool.releaseAll(ti);
	}

	/**
	 * Retrieve a TextureItem from pool with default Bitmap
	 * with dimension TextureRenderer.TEXTURE_WIDTH/HEIGHT.
	 */
	public synchronized static TextureItem get(boolean initBitmap) {
		TextureItem ti = pool.get();
		if (initBitmap) {
			ti.bitmap = getBitmap();
			ti.bitmap.eraseColor(Color.TRANSPARENT);
			ti.ownBitmap = false;
		} else {
			ti.ownBitmap = true;
		}
		return ti;
	}

	private final static SyncPool<TextureItem> pool = new SyncPool<TextureItem>(20) {

		@Override
		public void init(int num) {

			int[] textureIds = new int[num];
			GLES20.glGenTextures(num, textureIds, 0);

			for (int i = 0; i < num; i++) {
				initTexture(textureIds[i]);
				TextureItem to = new TextureItem(textureIds[i]);
				pool = Inlist.push(pool, to);
			}
			count = num;
			fill = num;
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

			if (it.isClone){
				it.isClone = false;
				it.id = -1;
				it.width = -1;
				it.height = -1;
				return;
			}

			releaseBitmap(it);
		}

		@Override
		protected void freeItem(TextureItem it) {
			it.width = -1;
			it.height = -1;

			if (!it.isClone)
				releaseTexture(it);
		}
	};

	private final static ArrayList<Integer> mTextures = new ArrayList<Integer>();
	private final static ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>(10);

	public final static int TEXTURE_WIDTH = 512;
	public final static int TEXTURE_HEIGHT = 256;

	private static int mBitmapFormat;
	private static int mBitmapType;
	private static int mTexCnt = 0;

	static void releaseTexture(TextureItem it) {

		synchronized (mTextures) {
			if (it.id >= 0) {
				mTextures.add(Integer.valueOf(it.id));
				it.id = -1;
			}
		}
	}

	/**
	 * This function may only be used in GLRenderer Thread.
	 *
	 * @param to the TextureObjet to compile and upload
	 */
	public static void uploadTexture(TextureItem to) {

		// free unused textures, find a better place for this TODO
		synchronized (mTextures) {
			int size = mTextures.size();
			int[] tmp = new int[size];
			for (int i = 0; i < size; i++)
				tmp[i] = mTextures.get(i).intValue();
			mTextures.clear();
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
		else {
			// FIXME when in doubt
			//to.bitmap = null;
		}
	}

	public static void uploadTexture(TextureItem to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d(TAG, "no texture!");
			return;
		}
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, to.id);

		if (to.ownBitmap) {
			bitmap.uploadToTexture(false);
			//GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		} else if (to.width == w && to.height == h) {
			bitmap.uploadToTexture(true);
			//GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap, format, type);

		} else {
			bitmap.uploadToTexture(false);
			//GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, format, bitmap, type, 0);
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

		mBitmaps.clear();
		mTextures.clear();

		for (int i = 0; i < 4; i++) {
			Bitmap bitmap = CanvasAdapter.g.getBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT, 0);
			//Bitmap bitmap = Bitmap.createBitmap(
			//		TEXTURE_WIDTH, TEXTURE_HEIGHT,
			//		Bitmap.Config.ARGB_8888);
			mBitmaps.add(bitmap);
		}

		//mBitmapFormat = GLUtils.getInternalFormat(mBitmaps.get(0));
		//mBitmapType = GLUtils.getType(mBitmaps.get(0));

		mTexCnt = num;
	}

	static Bitmap getBitmap() {
		synchronized (mBitmaps) {

			int size = mBitmaps.size();
			if (size == 0) {
				//Bitmap bitmap = Bitmap.createBitmap(
				//		TEXTURE_WIDTH, TEXTURE_HEIGHT,
				//		Bitmap.Config.ARGB_8888);
				//
				//if (TextureRenderer.debug)
				//	Log.d(TAG, "alloc bitmap: " +
				//		android.os.Debug.getNativeHeapAllocatedSize() / (1024 * 1024));

				return CanvasAdapter.g.getBitmap(TEXTURE_WIDTH, TEXTURE_HEIGHT, 0);
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
			it.ownBitmap = false;
		}
	}
}
