/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.core.Tile;

import android.util.Log;

/**
 * 
 */
public class JobTile extends Tile implements Comparable<JobTile> {
	private final static String TAG = JobTile.class.getName();
	// public final static int LOADING = 1;
	// public final static int NEWDATA = 1 << 1;
	// public final static int READY = 1 << 2;
	// public final static int AVAILABLE = 1 << 1 | 1 << 2;
	// public final static int CANCELED = 1 << 3;
	// public int state;

	public final static int STATE_NONE = 0;
	public final static int STATE_LOADING = 1 << 0;
	public final static int STATE_NEW_DATA = 1 << 1;
	public final static int STATE_READY = 1 << 2;

	public void clearState() {
		state = STATE_NONE;
	}

	public void setLoading() {
		if (state != STATE_NONE)
			Log.d(TAG, "wrong state: " + state);

		state = STATE_LOADING;
	}

	/**
	 * tile is in JobQueue
	 */
	//public boolean isLoading;
	public byte state;

	/**
	 * distance from map center.
	 */
	public float distance;

	/**
	 * @param tileX
	 *            ...
	 * @param tileY
	 *            ...
	 * @param zoomLevel
	 *            ..
	 */
	public JobTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
	}

	@Override
	public int compareTo(JobTile o) {
		if (this.distance < o.distance) {
			return -1;
		}
		if (this.distance > o.distance) {
			return 1;
		}
		return 0;
	}
}
