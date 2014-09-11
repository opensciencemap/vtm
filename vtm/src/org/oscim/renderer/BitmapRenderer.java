/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.bucket.BitmapBucket;

/**
 * RenderLayer to draw a custom Bitmap.
 * NOTE: Only modify the Bitmap within a synchronized block!
 * synchronized(bitmap){} Then call updateBitmap().
 */
public class BitmapRenderer extends BucketRenderer {

	private Bitmap mBitmap;
	private int mWidth;
	private int mHeight;
	private boolean initialized;
	private boolean mUpdateBitmap;

	/**
	 * @param bitmap
	 *            with dimension being power of two
	 * @param srcWidth
	 *            TODO width used
	 * @param srcHeight
	 *            TODO height used
	 */
	public synchronized void setBitmap(Bitmap bitmap,
	        int srcWidth, int srcHeight,
	        int targetWidth, int targetHeight) {
		mWidth = targetWidth;
		mHeight = targetHeight;
		mBitmap = bitmap;
		initialized = false;
	}

	public synchronized void updateBitmap() {
		mUpdateBitmap = true;
	}

	@Override
	public synchronized void update(GLViewport v) {
		if (!initialized) {
			buckets.clear();

			BitmapBucket l = new BitmapBucket(true);
			l.setBitmap(mBitmap, mWidth, mHeight);
			buckets.set(l);

			mUpdateBitmap = true;
		}

		if (mUpdateBitmap) {
			mUpdateBitmap = false;
			compile();
		}
	}

	@Override
	protected synchronized void compile() {
		if (mBitmap == null)
			return;

		synchronized (mBitmap) {
			super.compile();
		}
	}

	@Override
	public synchronized void render(GLViewport v) {
		v.useScreenCoordinates(false, 8);
		BitmapBucket.Renderer.draw(buckets.get(), v, 1, 1);
	}
}
