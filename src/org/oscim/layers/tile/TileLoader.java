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

import org.oscim.utils.PausableThread;

public abstract class TileLoader extends PausableThread {
	private static int id;

	public interface Factory<T extends TileLoader>{
		T create(JobQueue jobQueue,	TileManager tileManager);
	}

	private final String THREAD_NAME;
	private final JobQueue mJobQueue;
	private final TileManager mTileManager;

	public TileLoader(JobQueue jobQueue, TileManager tileManager) {
		super();
		mJobQueue = jobQueue;
		mTileManager = tileManager;
		THREAD_NAME = "TileLoader" + (id++);
	}

	public abstract void cleanup();

	/**
	 * Load data for 'tile' and file tile.layers for rendering.
	 * (executed on MapWorker threads)
	 */

	protected abstract boolean executeJob(MapTile tile);


	@Override
	protected void doWork() {
		MapTile tile = mJobQueue.poll();

		if (tile == null)
			return;

		try {
			executeJob(tile);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (!isInterrupted()) {
			// pass tile to main thread
			mTileManager.passTile(tile);
		}
	}


	@Override
	protected String getThreadName() {
		return THREAD_NAME;
	}

	@Override
	protected int getThreadPriority() {
		return (Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2;
	}

	@Override
	protected boolean hasWork() {
		return !mJobQueue.isEmpty();
	}
}
