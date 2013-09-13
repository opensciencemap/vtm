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

import org.oscim.event.EventListener;
import org.oscim.event.MapEvent;
import org.oscim.event.UpdateEvent;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.tiling.TileRenderer;

public abstract class TileLayer<T extends TileLoader> extends Layer implements EventListener {
	//private final static String TAG = TileLayer.class.getName();
	private final static int MAX_ZOOMLEVEL = 17;
	private final static int MIN_ZOOMLEVEL = 2;
	private final static int CACHE_LIMIT = 250;

	protected final TileManager mTileManager;
	protected final TileRenderer mRenderLayer;

	protected final int mNumTileLoader = 4;
	protected final ArrayList<T> mTileLoader;

	protected boolean mInitial = true;

	public TileLayer(Map map) {
		this(map, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL, CACHE_LIMIT);
	}

	public TileLayer(Map map, int minZoom, int maxZoom, int cacheLimit) {
		super(map);

		// TileManager responsible for adding visible tiles
		// to load queue and managing in-memory tile cache.
		mTileManager = new TileManager(map, minZoom, maxZoom, cacheLimit);

		// Instantiate TileLoader threads
		mTileLoader = new ArrayList<T>();
		for (int i = 0; i < mNumTileLoader; i++) {
			T tileGenerator = createLoader(mTileManager);
			mTileLoader.add(tileGenerator);
			tileGenerator.start();
		}

		// RenderLayer is working in GL Thread and actually
		// drawing loaded tiles to screen.
		mRenderer = mRenderLayer = new TileRenderer(mTileManager);

		map.addListener(UpdateEvent.TYPE, this);
	}

	abstract protected T createLoader(TileManager tm);

	public TileRenderer getTileLayer() {
		return (TileRenderer) mRenderer;
	}

	//	@Override
	//	public void onUpdate(MapPosition mapPosition, boolean changed, boolean clear) {
	//
	//		if (clear || mInitial) {
	//			mRenderLayer.clearTiles();
	//			mTileManager.init(mInitial);
	//			mInitial = false;
	//			changed = true;
	//		}
	//
	//		if (changed && mTileManager.update(mapPosition))
	//			notifyLoaders();
	//	}

	@Override
	public void handleEvent(MapEvent event) {
		if (event instanceof UpdateEvent) {

			UpdateEvent e = (UpdateEvent) event;

			boolean changed = e.positionChanged;

			if (e.clearMap || mInitial) {
				mRenderLayer.clearTiles();
				mTileManager.init(mInitial);
				mInitial = false;
				changed = true;
			}

			if (changed && mTileManager.update(mMap.getMapPosition()))
				notifyLoaders();
		}
	}

	@Override
	public void onDetach() {
		mMap.removeListener(UpdateEvent.TYPE, this);

		for (T loader : mTileLoader) {
			loader.pause();
			loader.interrupt();
			loader.cleanup();

			//			try {
			//				tileWorker.join(10000);
			//			} catch (InterruptedException e) {
			//				// restore the interrupted status
			//				Thread.currentThread().interrupt();
			//			}
		}
		mTileManager.destroy();
	}

	void notifyLoaders() {
		for (int i = 0; i < mNumTileLoader; i++) {
			T m = mTileLoader.get(i);
			synchronized (m) {
				m.go();
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
