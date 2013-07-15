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
import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.utils.GlUtils;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;

// FIXME

public class TextureItem extends Inlist<TextureItem> {
	private final static String TAG = TextureItem.class.getName();
	private static final GL20 GL = GLAdapter.get();

	// texture ID
	public int id;

	public int width;
	public int height;
	public boolean repeat;

	// vertex offset from which this texture is referenced
	public short offset;
	public short vertices;

	// temporary Bitmap
	public Bitmap bitmap;

	// external bitmap (not from pool)
	private boolean ownBitmap;

	// is only referencing a textureId, does not
	// release the texture when TextureItem is
	// released.
	private boolean isClone;

	// texture data is ready
	private boolean isReady;

	private TextureItem(int id) {
		this.id = id;
	}

	public static TextureItem clone(TextureItem ti) {
		TextureItem clone = new TextureItem(ti.id);
		clone.id = ti.id;
		clone.width = ti.width;
		clone.height = ti.height;
		clone.isClone = true;
		return clone;
	}

	public TextureItem(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.id = -1;
		this.ownBitmap = true;
		this.width = bitmap.getWidth();
		this.height = bitmap.getHeight();
	}

	public TextureItem(Bitmap bitmap, boolean repeat) {
		this.bitmap = bitmap;
		this.id = -1;
		this.ownBitmap = true;
		this.width = bitmap.getWidth();
		this.height = bitmap.getHeight();
		this.repeat = repeat;
	}

	public void bind(){
		if (!isReady){
			TextureItem.uploadTexture(this);
			isReady = true;
		}
		GL.glBindTexture(GL20.GL_TEXTURE_2D, id);
	}

	public synchronized static void releaseAll(TextureItem ti) {
		pool.releaseAll(ti);
	}

	/**
	 * Retrieve a TextureItem from pool with default Bitmap with dimension
	 * TextureRenderer.TEXTURE_WIDTH/HEIGHT.
	 */
	public synchronized static TextureItem get() {
		TextureItem ti = pool.get();
		ti.bitmap = getBitmap();
		ti.bitmap.eraseColor(Color.TRANSPARENT);

		return ti;
	}

	static class TextureItemPool extends SyncPool<TextureItem> {

		public TextureItemPool() {
			super(20);
		}

		@Override
		public void init(int num) {
			//int[] textureIds = GlUtils.glGenTextures(num);
			//
			//for (int i = 0; i < num; i++) {
			//	initTexture(textureIds[i]);
			//	TextureItem to = new TextureItem(textureIds[i]);
			//	pool = Inlist.push(pool, to);
			//}
			//fill = num;

			fill = 0;
		}

		public TextureItem get(int width, int height) {
			return null;
		}

		@Override
		protected TextureItem createItem() {
			return new TextureItem(-1);
		}

		/** called when item is added back to pool */
		@Override
		protected boolean clearItem(TextureItem it) {

			if (it.ownBitmap) {
				it.bitmap = null;
				it.ownBitmap = false;
				releaseTexture(it);
				return false;
			}

			if (it.isClone) {
				it.isClone = false;
				it.id = -1;
				it.width = -1;
				it.height = -1;
				return false;
			}

			releaseBitmap(it);

			return true;
		}

		@Override
		protected void freeItem(TextureItem it) {
			it.width = -1;
			it.height = -1;

			if (!it.isClone)
				releaseTexture(it);
		}
	};

	private final static SyncPool<TextureItem> pool = new TextureItemPool();

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
	 * @param to
	 *            the TextureObjet to compile and upload
	 */
	public static void uploadTexture(TextureItem to) {

		// free unused textures -> TODO find a better place for this
		synchronized (mTextures) {
			int size = mTextures.size();
			if (size > 0) {
				int[] tmp = new int[size];
				for (int i = 0; i < size; i++)
					tmp[i] = mTextures.get(i).intValue();

				mTextures.clear();
				GlUtils.glDeleteTextures(size, tmp);

				mTexCnt -= size;
			}
		}

		if (to.id < 0) {
			mTexCnt++;
			int[] textureIds = GlUtils.glGenTextures(1);
			to.id = textureIds[0];
			initTexture(to);
			// if (TextureRenderer.debug)
			Log.d(TAG, "poolFill:" + pool.getFill()
					+ " texCnt:" + mTexCnt
					+ " new texture " + to.id);
		}

		uploadTexture(to, to.bitmap,
				mBitmapFormat, mBitmapType,
				TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (!to.ownBitmap)
			TextureItem.releaseBitmap(to);
		else {
			// FIXME when in doubt
			// to.bitmap = null;
		}
	}

	public static void uploadTexture(TextureItem to, Bitmap bitmap,
			int format, int type, int w, int h) {

		if (to == null) {
			Log.d(TAG, "no texture!");
			return;
		}

		GL.glBindTexture(GL20.GL_TEXTURE_2D, to.id);

		if (to.ownBitmap) {
			bitmap.uploadToTexture(false);
		} else if (to.width == w && to.height == h) {
			bitmap.uploadToTexture(true);
		} else {
			bitmap.uploadToTexture(false);
			to.width = w;
			to.height = h;
		}

		if (TextureRenderer.debug)
			GlUtils.checkGlError(TAG);
	}

	static void initTexture(TextureItem it) {
		GL.glBindTexture(GL20.GL_TEXTURE_2D, it.id);

		GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER,
				GL20.GL_LINEAR);
		GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER,
				GL20.GL_LINEAR);

		if (it.repeat) {
			GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S,
					GL20.GL_REPEAT);
			GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T,
					GL20.GL_REPEAT);
		} else {
			GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S,
					GL20.GL_CLAMP_TO_EDGE);
			GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T,
					GL20.GL_CLAMP_TO_EDGE);
		}
	}

	static void init(int num) {

		pool.init(num);
		mTexCnt = num;

		mBitmaps.clear();
		mTextures.clear();

		// for (int i = 0; i < 4; i++) {
		// Bitmap bitmap = CanvasAdapter.g.getBitmap(TEXTURE_WIDTH,
		// TEXTURE_HEIGHT, 0);
		// // Bitmap bitmap = Bitmap.createBitmap(
		// // TEXTURE_WIDTH, TEXTURE_HEIGHT,
		// // Bitmap.Config.ARGB_8888);
		// mBitmaps.add(bitmap);
		// }

		// mBitmapFormat = GLUtils.getInternalFormat(mBitmaps.get(0));
		// mBitmapType = GLUtils.getType(mBitmaps.get(0));

	}

	static Bitmap getBitmap() {
		synchronized (mBitmaps) {

			int size = mBitmaps.size();
			if (size == 0) {
				// Bitmap bitmap = Bitmap.createBitmap(
				// TEXTURE_WIDTH, TEXTURE_HEIGHT,
				// Bitmap.Config.ARGB_8888);
				//
				// if (TextureRenderer.debug)
				// Log.d(TAG, "alloc bitmap: " +
				// android.os.Debug.getNativeHeapAllocatedSize() / (1024 *
				// 1024));

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
