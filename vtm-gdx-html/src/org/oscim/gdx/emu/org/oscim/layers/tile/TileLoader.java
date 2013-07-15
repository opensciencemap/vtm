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

import com.badlogic.gdx.Gdx;
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

	private static int jobs;

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
		mWorking = false;
		if (!mTileManager.jobQueue.isEmpty())
			go();
	}

	public void awaitPausing() {

	}

	public void start() {
		mPausing = false;
	}

	boolean mWorking;

	public void go() {
		if (mWorking) {
			//Log.d("...", "has work " + jobs);
			return;
		}

		MapTile tile = mTileManager.jobQueue.poll();

		if (tile == null)
			return;

		try {
			tile.loader = this;
			executeJob(tile);

			mWorking = true;
			jobs++;
			//Log.d("...", "add job " + jobs);

		} catch (Exception e) {
			e.printStackTrace();

			tile.clear();
			jobCompleted(tile, false);
		}
	}

	public void jobCompleted(MapTile tile, boolean success) {
		if (success) {
			if (!isInterrupted) {
				// pass tile to main thread
				mTileManager.passTile(tile);
			}
		}
		mWorking = false;
		jobs--;
		//Log.d("...", "finish job " + jobs + " " + success);

		if (!mPausing && !mTileManager.jobQueue.isEmpty()){

			Gdx.app.postRunnable(new Runnable(){

				@Override
				public void run() {
					go();
				}
			}
			);
		}
	}
}
