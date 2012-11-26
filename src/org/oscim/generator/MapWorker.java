/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.generator;

import org.oscim.renderer.TileGenerator;
import org.oscim.renderer.TileManager;
import org.oscim.utils.PausableThread;

/**
 * A MapWorker uses a {@link TileGenerator} to generate map tiles. It runs in a
 * separate thread to avoid blocking the UI thread.
 */
public class MapWorker extends PausableThread {
	private final String THREAD_NAME;
	private final JobQueue mJobQueue;
	private final TileGenerator mMapGenerator;
	private final TileManager mTileManager;

	/**
	 * @param id
	 *            thread id
	 * @param jobQueue
	 *            ...
	 * @param tileGenerator
	 *            ...
	 * @param tileManager
	 *            ...
	 */
	public MapWorker(int id, JobQueue jobQueue, TileGenerator tileGenerator,
			TileManager tileManager) {
		super();
		mJobQueue = jobQueue;
		mMapGenerator = tileGenerator;
		mTileManager = tileManager;

		THREAD_NAME = "MapWorker" + id;
	}

	public TileGenerator getTileGenerator() {
		return mMapGenerator;
	}

	@Override
	protected void afterRun() {
		// empty
	}

	@Override
	protected void doWork() {
		JobTile tile = mJobQueue.poll();

		if (tile == null)
			return;

		// Log.d("...", "load: " + tile);

		mMapGenerator.executeJob(tile);

		if (!isInterrupted()) {
			mTileManager.passTile(tile);
		}
	}

	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected void takeabreak() {
		mMapGenerator.getMapDatabase().cancel();
	}

	@Override
	protected int getThreadPriority() {
		return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 3;
	}

	@Override
	protected boolean hasWork() {
		return !mJobQueue.isEmpty();
	}
}
