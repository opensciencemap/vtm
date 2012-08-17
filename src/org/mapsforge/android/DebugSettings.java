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
package org.mapsforge.android;

/**
 * A simple DTO to stores flags for debugging rendered map tiles.
 */
public class DebugSettings {

	/**
	 * True if drawing of tile coordinates is enabled, false otherwise.
	 */
	public final boolean mDrawTileCoordinates;

	/**
	 * True if drawing of tile frames is enabled, false otherwise.
	 */
	public final boolean mDrawTileFrames;

	public final boolean mDrawUnmatchted;

	/**
	 * True if highlighting of water tiles is enabled, false otherwise.
	 */
	public final boolean mDisablePolygons;

	/**
	 * @param drawTileCoordinates
	 *            if drawing of tile coordinates is enabled.
	 * @param drawTileFrames
	 *            if drawing of tile frames is enabled.
	 * @param disablePolygons
	 *            if highlighting of water tiles is enabled.
	 * @param drawUnmatched
	 *            ...
	 */
	public DebugSettings(boolean drawTileCoordinates, boolean drawTileFrames,
			boolean disablePolygons, boolean drawUnmatched) {
		mDrawTileCoordinates = drawTileCoordinates;
		mDrawTileFrames = drawTileFrames;
		mDrawUnmatchted = drawUnmatched;
		mDisablePolygons = disablePolygons;
	}
}
