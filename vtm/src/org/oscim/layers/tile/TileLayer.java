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
package org.oscim.layers.tile;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.layers.Layer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TileLayer extends Layer implements UpdateListener {

	static final Logger log = LoggerFactory.getLogger(TileLayer.class);

	private final static int MAX_ZOOMLEVEL = 17;
	private final static int MIN_ZOOMLEVEL = 2;
	private final static int CACHE_LIMIT = 150;

	protected final TileManager mTileManager;

	protected TileLoader[] mTileLoader;

	public TileLayer(Map map) {
		this(map, MIN_ZOOMLEVEL, MAX_ZOOMLEVEL, CACHE_LIMIT);
	}

	public TileLayer(Map map, int minZoom, int maxZoom, int cacheLimit) {
		super(map);
		/* TileManager responsible for adding visible tiles
		 * to load queue and managing in-memory tile cache. */
		mTileManager = new TileManager(map, minZoom, maxZoom, cacheLimit);
	}

	abstract protected TileLoader createLoader(TileManager tm);

	protected void setRenderer(TileRenderer renderer) {
		mRenderer = renderer;
	}

	public TileRenderer tileRenderer() {
		return (TileRenderer) mRenderer;
	}

	protected void initLoader(int numLoaders) {
		mTileLoader = new TileLoader[numLoaders];

		for (int i = 0; i < numLoaders; i++) {
			mTileLoader[i] = createLoader(mTileManager);
			mTileLoader[i].start();
		}
	}

	@Override
	public void onMapEvent(Event event, MapPosition mapPosition) {

		if (event == Map.CLEAR_EVENT) {
			// sync with TileRenderer
			synchronized (mRenderer) {
				tileRenderer().clearTiles();
				mTileManager.init();
			}

			if (mTileManager.update(mapPosition))
				notifyLoaders();

		} else if (event == Map.POSITION_EVENT) {
			if (mTileManager.update(mapPosition))
				notifyLoaders();
		}
	}

	@Override
	public void onDetach() {
		for (TileLoader loader : mTileLoader) {
			loader.pause();
			loader.interrupt();
			loader.cleanup();
		}
	}

	void notifyLoaders() {
		for (TileLoader loader : mTileLoader)
			loader.go();
	}

	protected void pauseLoaders(boolean wait) {
		for (TileLoader loader : mTileLoader) {
			if (!loader.isPausing())
				loader.pause();
		}
		if (!wait)
			return;

		for (TileLoader loader : mTileLoader) {
			if (!loader.isPausing())
				loader.awaitPausing();
		}
	}

	protected void resumeLoaders() {
		for (TileLoader loader : mTileLoader)
			loader.proceed();
	}
}
