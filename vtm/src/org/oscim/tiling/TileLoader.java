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
package org.oscim.tiling;

import org.oscim.utils.PausableThread;

public abstract class TileLoader extends PausableThread {
	private static int id;

	private final String THREAD_NAME;
	private final TileManager mTileManager;

	public TileLoader(TileManager tileManager) {
		super();
		mTileManager = tileManager;
		THREAD_NAME = "TileLoader" + (id++);
	}

	public abstract void cleanup();

	protected abstract boolean executeJob(MapTile tile);

	public void go() {
		synchronized (this) {
			notify();
		}
	}

	@Override
	protected void doWork() {
		MapTile tile = mTileManager.getTileJob();

		if (tile == null)
			return;

		boolean success = false;

		try {
			success = executeJob(tile);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		if (isInterrupted())
			success = false;

		mTileManager.jobCompleted(tile, success);
	}

	public void jobCompleted(MapTile tile, boolean success) {

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
		return mTileManager.hasTileJobs();
	}
}
