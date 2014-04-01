/*
 * Copyright 2012, 2013 Hannes Janetzek
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

import java.util.ArrayList;

import javax.annotation.CheckReturnValue;

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

public class TextureItem extends Inlist<TextureItem> {
	static final Logger log = LoggerFactory.getLogger(TextureItem.class);

	/** texture ID */
	public int id;

	/** current settings */
	public final int width;
	public final int height;
	public final boolean repeat;

	/** vertex offset from which this texture is referenced */
	public short offset;
	public short vertices;

	/** temporary Bitmap */
	public Bitmap bitmap;

	/** do not release the texture when TextureItem is released. */
	private boolean ref;

	/** texture data is ready */
	private boolean ready;

	final TexturePool pool;

	private TextureItem(TexturePool pool, int id) {
		this.id = id;
		this.width = pool.mWidth;
		this.height = pool.mHeight;
		this.pool = pool;
		this.repeat = false;
	}

	public TextureItem(Bitmap bitmap) {
		this.bitmap = bitmap;
		this.id = -1;
		this.width = bitmap.getWidth();
		this.height = bitmap.getHeight();
		this.pool = NOPOOL;
		this.repeat = false;

	}

	public TextureItem(Bitmap bitmap, boolean repeat) {
		this.bitmap = bitmap;
		this.id = -1;
		this.width = bitmap.getWidth();
		this.height = bitmap.getHeight();
		this.repeat = repeat;
		this.pool = NOPOOL;

	}

	private TextureItem(TexturePool pool, int id, int width, int height) {
		this.id = id;
		this.width = width;
		this.height = height;
		this.pool = pool;
		this.repeat = false;
	}

	public static TextureItem clone(TextureItem ti) {
		// original texture needs to be loaded
		if (!ti.ready)
			throw new IllegalStateException();

		TextureItem clone = new TextureItem(ti.pool, ti.id, ti.width, ti.height);
		clone.id = ti.id;
		clone.ref = true;
		clone.ready = true;
		return clone;
	}

	/**
	 * Upload Image to Texture
	 * [on GL-Thread]
	 */
	public void upload() {
		if (!ready) {
			pool.uploadTexture(this);
			ready = true;
		}
	}

	/**
	 * Bind Texture for rendering
	 * [on GL-Thread]
	 */
	public void bind() {
		if (!ready) {
			pool.uploadTexture(this);
			ready = true;
		} else {
			GLState.bindTex2D(id);
		}
	}

	/**
	 * Dispose TextureItem
	 * [Threadsafe]
	 * 
	 * @return this.next
	 */
	@CheckReturnValue
	public TextureItem dispose() {
		TextureItem n = this.next;
		this.next = null;
		pool.release(this);
		return n;
	}

	public static class TexturePool extends SyncPool<TextureItem> {
		private final ArrayList<Bitmap> mBitmaps = new ArrayList<Bitmap>(10);

		private final int mHeight;
		private final int mWidth;
		private final boolean mUseBitmapPool;

		//private final int mBitmapFormat;
		//private final int mBitmapType;

		protected int mTexCnt = 0;

		public TexturePool(int maxFill, int width, int height) {
			super(maxFill);
			mWidth = width;
			mHeight = height;
			mUseBitmapPool = true;
		}

		public TexturePool(int maxFill) {
			super(maxFill);
			mWidth = 0;
			mHeight = 0;
			mUseBitmapPool = false;
		}

		@Override
		public TextureItem releaseAll(TextureItem t) {
			throw new RuntimeException("use TextureItem.dispose()");
		}

		/**
		 * Retrieve a TextureItem from pool.
		 */
		public synchronized TextureItem get() {
			TextureItem t = super.get();

			if (!mUseBitmapPool)
				return t;

			synchronized (mBitmaps) {
				int size = mBitmaps.size();
				if (size == 0)
					t.bitmap = CanvasAdapter.g.getBitmap(mWidth, mHeight, 0);
				else {
					t.bitmap = mBitmaps.remove(size - 1);
					t.bitmap.eraseColor(Color.TRANSPARENT);
				}
			}

			return t;
		}

		public synchronized TextureItem get(Bitmap bitmap) {
			TextureItem t = super.get();
			t.bitmap = bitmap;

			return t;
		}

		@Override
		protected TextureItem createItem() {
			return new TextureItem(this, -1);
		}

		@Override
		protected boolean clearItem(TextureItem t) {

			if (t.ref)
				return false;

			t.ready = false;

			if (mUseBitmapPool)
				releaseBitmap(t);

			return t.id >= 0;
		}

		@Override
		protected void freeItem(TextureItem t) {
			if (!t.ref && t.id >= 0) {
				mTexCnt--;
				synchronized (disposedTextures) {
					disposedTextures.add(Integer.valueOf(t.id));
					t.id = -1;
				}
			}
		}

		protected void releaseBitmap(TextureItem t) {

			if (t.bitmap == null)
				return;

			synchronized (mBitmaps) {
				mBitmaps.add(t.bitmap);
				t.bitmap = null;
			}
		}

		private void uploadTexture(TextureItem t) {

			if (t.bitmap == null)
				throw new RuntimeException("Missing bitmap for texture");

			if (t.id < 0) {
				int[] textureIds = GLUtils.glGenTextures(1);
				t.id = textureIds[0];

				initTexture(t);

				if (TextureLayer.Renderer.debug)
					log.debug("fill:" + getFill()
					        + " count:" + mTexCnt
					        + " new texture " + t.id);

				mTexCnt++;

				t.bitmap.uploadToTexture(false);
			} else {
				GLState.bindTex2D(t.id);
				// use faster subimage upload 
				t.bitmap.uploadToTexture(true);
			}

			if (TextureLayer.Renderer.debug)
				GLUtils.checkGlError(TextureItem.class.getName());

			if (mUseBitmapPool)
				releaseBitmap(t);
		}

		protected void initTexture(TextureItem t) {
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
	};

	/* Pool for not-pooled textures. Disposed items will only be released
	 * on the GL-Thread and will not be put back in any pool. */
	final static TexturePool NOPOOL = new TexturePool(0);
	final static ArrayList<Integer> disposedTextures = new ArrayList<Integer>();

	private static GL20 GL;

	static void init(GL20 gl) {
		GL = gl;
	}

	/** disposed textures are released by MapRenderer after each frame */
	public static void disposeTextures() {
		synchronized (disposedTextures) {

			int size = disposedTextures.size();
			if (size > 0) {
				int[] tmp = new int[size];
				for (int i = 0; i < size; i++)
					tmp[i] = disposedTextures.get(i).intValue();

				disposedTextures.clear();
				GLUtils.glDeleteTextures(size, tmp);
				//mTexCnt -= size;
			}
		}
	}
}
