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
package org.oscim.layers.tile;

import java.util.ArrayList;

import org.oscim.core.MapPosition;
import org.oscim.layers.Layer;
import org.oscim.view.MapView;

public abstract class TileLayer<T extends TileLoader> extends Layer {
	//private final static String TAG = TileLayer.class.getName();
	private final static int MAX_ZOOMLEVEL = 17;

	protected final TileManager mTileManager;
	protected final TileRenderLayer mRenderLayer;

	protected final int mNumTileLoader = 4;
	protected final ArrayList<T> mTileLoader;

	public TileLayer(MapView mapView) {
		this(mapView, MAX_ZOOMLEVEL);
	}

	public TileLayer(MapView mapView, int maxZoom) {
		super(mapView);

		// TileManager responsible for adding visible tiles
		// to load queue and managing in-memory tile cache.
		mTileManager = new TileManager(mapView, this, maxZoom);

		// Instantiate TileLoader threads
		mTileLoader = new ArrayList<T>();
		for (int i = 0; i < mNumTileLoader; i++) {
			T tileGenerator = createLoader(mTileManager);
			mTileLoader.add(tileGenerator);
			tileGenerator.start();
		}

		// RenderLayer is working in GL Thread and actually
		// drawing loaded tiles to screen.
		mLayer = mRenderLayer = new TileRenderLayer(mapView, mTileManager);
	}

	abstract protected T createLoader(TileManager tm);

	public TileRenderLayer getTileLayer() {
		return (TileRenderLayer) mLayer;
	}

	@Override
	public void onUpdate(MapPosition mapPosition, boolean changed, boolean clear) {

		if (clear) {
			mRenderLayer.clearTiles();
			mTileManager.init(mMapView.getWidth(), mMapView.getHeight());
			changed = true;
		}
		if (changed)
			mTileManager.update(mapPosition);
	}

	@Override
	public void destroy() {
		for (T tileWorker : mTileLoader) {
			tileWorker.pause();
			tileWorker.interrupt();
			tileWorker.cleanup();

			try {
				tileWorker.join(10000);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
		mTileManager.destroy();
	}

	void notifyLoaders() {
		for (int i = 0; i < mNumTileLoader; i++) {
			T m = mTileLoader.get(i);
			synchronized (m) {
				m.notify();
			}
		}
	}

	protected void pauseLoaders(boolean wait) {
		for (T loader : mTileLoader) {
			if (!loader.isPausing())
				loader.pause();
		}
		if (wait) {
			for (T loader : mTileLoader) {
				if (!loader.isPausing())
					loader.awaitPausing();
			}
		}
	}

	protected void resumeLoaders() {
		for (T loader : mTileLoader)
			loader.proceed();
	}
}
