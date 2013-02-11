/*
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

import org.oscim.core.Tile;

import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class JobTile extends Tile {
	private final static String TAG = JobTile.class.getName();

	public final static int STATE_NONE = 0;
	public final static int STATE_LOADING = 1 << 0;
	public final static int STATE_NEW_DATA = 1 << 1;
	public final static int STATE_READY = 1 << 2;
	public final static int STATE_ERROR = 1 << 3;

	public void clearState() {
		state = STATE_NONE;
	}

	/**
	 * @return true if tile is loading, has new data or is ready for rendering
	 * */
	public boolean isActive() {
		return state > STATE_NONE && state < STATE_ERROR;
	}

	public void setLoading() {
		if (state != STATE_NONE)
			Log.d(TAG, "wrong state: " + state);

		state = STATE_LOADING;
	}

	public byte state;

	/**
	 * distance from map center
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
}
