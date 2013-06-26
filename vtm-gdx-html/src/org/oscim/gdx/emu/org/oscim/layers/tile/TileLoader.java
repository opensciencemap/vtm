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

import com.badlogic.gdx.utils.Timer;

public abstract class TileLoader {

	private final TileManager mTileManager;
	private Timer mTimer;

	public TileLoader(TileManager tileManager) {
		if (mTimer == null)
			mTimer = new Timer();

		mTileManager = tileManager;
	}

	public abstract void cleanup();

	protected abstract boolean executeJob(MapTile tile);

	boolean isInterrupted;

	void interrupt() {
		isInterrupted = true;
		// cancel loading
	}

	boolean mPausing;

	public boolean isPausing() {
		return mPausing;
	}

	public void pause() {
		mPausing = true;
	}

	public void proceed() {
		mPausing = false;
		// FIXME
		hasWork = false;
		if (!mTileManager.jobQueue.isEmpty())
			go();
	}

	public void awaitPausing() {

	}

	public void start() {
		mPausing = false;
	}
	boolean hasWork;

	public void go() {
		if (hasWork)
			return;

		final TileLoader loader = this;
		mTimer.scheduleTask(new Timer.Task() {

			@Override
			public void run() {

				MapTile tile = mTileManager.jobQueue.poll();

				if (tile == null)
					return;

				try {
					tile.loader = loader;
					executeJob(tile);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
		}, 0.01f);
		hasWork = true;
	}

	public void jobCompleted(MapTile tile, boolean success) {
		if (success) {
			if (!isInterrupted) {
				// pass tile to main thread
				mTileManager.passTile(tile);
			}
		}
		hasWork = false;
		if (!mPausing && !mTileManager.jobQueue.isEmpty())
			go();
	}
}
