/*
 * Copyright 2013 Hannes Janetzek
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
import org.oscim.core.MapPosition;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.BitmapLayer;

/**
 * RenderLayer to draw a custom Bitmap.
 * NOTE: Only modify the Bitmap within a synchronized block!
 * synchronized(bitmap){} Then call updateBitmap().
 */
public class BitmapRenderer extends ElementRenderer {

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
	protected synchronized void update(MapPosition pos, boolean changed, Matrices m) {
		if (!initialized) {
			layers.clear();

			BitmapLayer l = new BitmapLayer(true);
			l.setBitmap(mBitmap, mWidth, mHeight);
			layers.textureLayers = l;

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
	protected synchronized void render(MapPosition pos, Matrices m) {
		m.useScreenCoordinates(false, 8);
		BitmapLayer.Renderer.draw(layers.textureLayers, m, 1, 1);
	}
}
