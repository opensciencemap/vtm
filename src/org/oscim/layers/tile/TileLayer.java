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

	private boolean mClearMap = true;

	protected final TileManager mTileManager;

	protected final JobQueue mJobQueue;

	protected final int mNumTileLoader = 4;
	protected final ArrayList<T> mTileLoader;

	public TileLayer(MapView mapView) {
		super(mapView);

		// TileManager responsible for adding visible tiles
		// to load queue and managing in-memory tile cache.
		mTileManager = new TileManager(mapView, this);

		mJobQueue = new JobQueue();

		// Instantiate TileLoader threads
		mTileLoader = new ArrayList<T>();
		for (int i = 0; i < mNumTileLoader; i++) {
			T tileGenerator = createLoader(mJobQueue, mTileManager);
			mTileLoader.add(tileGenerator);
			tileGenerator.start();
		}

		// RenderLayer is working in GL Thread and actually
		// drawing loaded tiles to screen.
		mLayer = new TileRenderLayer(mapView, mTileManager);
	}

	abstract protected T createLoader(JobQueue q, TileManager tm);

	public TileRenderLayer getTileLayer() {
		return (TileRenderLayer) mLayer;
	}

	@Override
	public void onUpdate(MapPosition mapPosition, boolean changed) {

		if (mClearMap) {
			mTileManager.init(mMapView.getWidth(), mMapView.getHeight());
			mClearMap = false;
			changed = true;
		}
		if (changed)
			mTileManager.update(mapPosition);
	}

	@Override
	public void destroy() {

		mTileManager.destroy();

		for (T tileWorker : mTileLoader) {
			tileWorker.pause();
			tileWorker.interrupt();

			//tileWorker.getMapDatabase().close();
			tileWorker.cleanup();

			try {
				tileWorker.join(10000);
			} catch (InterruptedException e) {
				// restore the interrupted status
				Thread.currentThread().interrupt();
			}
		}
	}

	protected void clearMap() {
		// clear tile and overlay data before next draw
		mClearMap = true;
	}


	/**
	 * add jobs and remember TileGenerators that stuff needs to be done
	 *
	 * @param jobs
	 *            tile jobs
	 */
	public void setJobs(MapTile[] jobs) {
		if (jobs == null) {
			mJobQueue.clear();
			return;
		}
		mJobQueue.setJobs(jobs);

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
