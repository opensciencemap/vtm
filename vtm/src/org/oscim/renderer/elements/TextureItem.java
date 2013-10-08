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
package org.oscim.renderer.elements;

import java.util.ArrayList;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GL20;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLUtils;
import org.oscim.utils.pool.Inlist;
import org.oscim.utils.pool.SyncPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME needs rewrite!
// TODO use separate pools for different bitmap types and dimensions

public class TextureItem extends Inlist<TextureItem> {
	static final Logger log = LoggerFactory.getLogger(TextureItem.class);

	private static GL20 GL;

	/** texture ID */
	public int id;

	public int width;
	public int height;
	public boolean repeat;

	/** vertex offset from which this texture is referenced */
	public short offset;
	public short vertices;

	/** temporary Bitmap */
	public Bitmap bitmap;

	/** external bitmap (not from pool) */
	private boolean ownBitmap;

	/** do not release the texture when TextureItem is released. */
	private boolean isClone;

	/** texture data is ready */
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
		// original texture needs to be loaded
		clone.isReady = true;
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

	public void upload() {
		if (!isReady) {
			TextureItem.uploadTexture(this);
			isReady = true;
		}
	}

	public void bind() {
		if (!isReady) {
			TextureItem.uploadTexture(this);
			isReady = true;
		} else {
			GLState.bindTex2D(id);
		}
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
			super(10);
		}

		@Override
		public void init(int num) {
			if (pool != null) {
				log.debug("still textures in pool! " + fill);
				pool = null;
			}

			int[] textureIds = GLUtils.glGenTextures(num);

			for (int i = 0; i < num; i++) {
				TextureItem to = new TextureItem(textureIds[i]);
				initTexture(to);

				pool = Inlist.push(pool, to);
			}

			fill = num;
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
		protected boolean clearItem(TextureItem t) {

			if (t.ownBitmap) {
				t.bitmap = null;
				t.ownBitmap = false;
				releaseTexture(t);
				return false;
			}

			if (t.isClone) {
				t.isClone = false;
				t.id = -1;
				t.width = -1;
				t.height = -1;
				return false;
			}

			t.isReady = false;

			releaseBitmap(t);

			return true;
		}

		@Override
		protected void freeItem(TextureItem t) {
			t.width = -1;
			t.height = -1;

			if (!t.isClone)
				releaseTexture(t);
		}
	};

	public final static SyncPool<TextureItem> pool = new TextureItemPool();

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
	 * @param t
	 *            the TextureObjet to compile and upload
	 */
	private static void uploadTexture(TextureItem t) {
		GL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
		if (t.bitmap == null) {
			throw new RuntimeException("Missing bitmap for texture");
		}

		// free unused textures -> TODO find a better place for this
		synchronized (mTextures) {
			int size = mTextures.size();
			if (size > 0) {
				int[] tmp = new int[size];
				for (int i = 0; i < size; i++) {
					tmp[i] = mTextures.get(i).intValue();
				}
				mTextures.clear();
				GLUtils.glDeleteTextures(size, tmp);

				mTexCnt -= size;
			}
		}

		if (t.id < 0) {
			mTexCnt++;
			int[] textureIds = GLUtils.glGenTextures(1);
			t.id = textureIds[0];
			initTexture(t);
			if (TextureLayer.Renderer.debug)
				log.debug("fill:" + pool.getFill()
				        + " count:" + mTexCnt
				        + " new texture " + t.id);
		}

		//log.debug("UPLOAD ID: " + t.id);

		uploadTexture(t, t.bitmap,
		              mBitmapFormat, mBitmapType,
		              TEXTURE_WIDTH, TEXTURE_HEIGHT);

		if (!t.ownBitmap)
			TextureItem.releaseBitmap(t);
		else {
			// FIXME when in doubt
			// to.bitmap = null;
		}
	}

	public static void uploadTexture(TextureItem t, Bitmap bitmap,
	        int format, int type, int w, int h) {

		if (t == null) {
			log.debug("no texture!");
			return;
		}

		GLState.bindTex2D(t.id);

		if (t.ownBitmap) {
			bitmap.uploadToTexture(false);
		} else if (t.width == w && t.height == h) {
			bitmap.uploadToTexture(true);
		} else {
			bitmap.uploadToTexture(false);
			t.width = w;
			t.height = h;
		}

		if (TextureLayer.Renderer.debug)
			GLUtils.checkGlError(TextureItem.class.getName());
	}

	private static void initTexture(TextureItem t) {
		GLState.bindTex2D(t.id);

		GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER,
		                   GL20.GL_LINEAR);
		GL.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER,
		                   GL20.GL_LINEAR);

		if (t.repeat) {
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

	static void init(GL20 gl, int num) {
		GL = gl;

		log.debug("init textures " + num);
		mTexCnt = num;
		pool.init(num);

		//mTexCnt = num;

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
				// log.debug("alloc bitmap: " +
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
		}
	}
}
