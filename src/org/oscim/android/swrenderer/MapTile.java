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
package org.oscim.android.swrenderer;

import org.oscim.android.mapgenerator.JobTile;

/**
 * 
 */
public class MapTile extends JobTile {
	private float mScale;

	final MapTile[] child = { null, null, null, null };
	MapTile parent;

	// private long mLoadTime;
	private int mTextureID;

	boolean isVisible;

	/**
	 * @param tileX
	 *            ...
	 * @param tileY
	 *            ...
	 * @param zoomLevel
	 *            ..
	 */
	public MapTile(int tileX, int tileY, byte zoomLevel) {
		super(tileX, tileY, zoomLevel);
		mScale = 1;
		mTextureID = -1;
	}

	/**
	 * @return ...
	 */
	public int getTexture() {
		return mTextureID;
	}

	/**
	 * @param mTextureID
	 *            ...
	 */
	public void setTexture(int mTextureID) {
		this.mTextureID = mTextureID;
	}

	/**
	 * @return ...
	 */
	public boolean hasTexture() {
		return mTextureID >= 0;
	}

	/**
	 * @return ...
	 */
	public float getScale() {
		return mScale;
	}

	/**
	 * @param scale
	 *            ...
	 */
	public void setScale(float scale) {
		mScale = scale;
	}

}
