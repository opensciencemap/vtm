/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012, 2013 OpenScienceMap
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

import static org.oscim.generator.JobTile.STATE_LOADING;
import static org.oscim.generator.JobTile.STATE_NONE;

/**
 * A JobQueue keeps the list of pending jobs for a MapView and prioritizes them.
 */
public class JobQueue {

	private int mCurrentJob = 0;
	private JobTile[] mJobs;

	/**
	 * @param tiles
	 *            the jobs to be added to this queue.
	 */
	public synchronized void setJobs(JobTile[] tiles) {
		for (JobTile t : tiles)
			t.state = STATE_LOADING;

		mJobs = tiles;
		mCurrentJob = 0;
	}

	/**
	 * Removes all jobs from this queue.
	 */
	public synchronized void clear() {
		if (mJobs == null) {
			mCurrentJob = 0;
			return;
		}
		JobTile[] tiles = mJobs;

		for (int i = mCurrentJob, n = mJobs.length; i < n; i++) {
			tiles[i].state = STATE_NONE;
			tiles[i] = null;
		}
		mCurrentJob = 0;
		mJobs = null;
	}

	/**
	 * @return true if this queue contains no jobs, false otherwise.
	 */
	public synchronized boolean isEmpty() {
		return (mJobs == null);
	}

	/**
	 * @return the most important job from this queue or null, if empty.
	 */
	public synchronized JobTile poll() {
		if (mJobs == null)
			return null;

		if (mCurrentJob == 0) {
			int len = mJobs.length;
			if (len > 1)
				TileDistanceSort.sort(mJobs, 0, len);
		}

		JobTile t = mJobs[mCurrentJob];
		mJobs[mCurrentJob] = null;

		if (++mCurrentJob == mJobs.length)
			mJobs = null;

		return t;

	}
}
