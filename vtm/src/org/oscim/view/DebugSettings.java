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
package org.oscim.view;

/**
 * A simple DTO to stores flags for debugging rendered map tiles.
 */
public class DebugSettings {

	/**
	 * True if drawing of tile coordinates is enabled, false otherwise.
	 */
	public final boolean drawTileCoordinates;

	/**
	 * True if drawing of tile frames is enabled, false otherwise.
	 */
	public final boolean drawTileFrames;

	public final boolean debugTheme;

	/**
	 * True if highlighting of water tiles is enabled, false otherwise.
	 */
	public final boolean disablePolygons;

	public final boolean debugLabels;

	/**
	 * @param drawTileCoordinates
	 *            if drawing of tile coordinates is enabled.
	 * @param drawTileFrames
	 *            if drawing of tile frames is enabled.
	 * @param disablePolygons
	 *            if highlighting of water tiles is enabled.
	 * @param debugTheme
	 *            ...
	 * @param debugLabels ...
	 */
	public DebugSettings(boolean drawTileCoordinates, boolean drawTileFrames,
			boolean disablePolygons, boolean debugTheme, boolean debugLabels) {
		this.drawTileCoordinates = drawTileCoordinates;
		this.drawTileFrames = drawTileFrames;
		this.debugTheme = debugTheme;
		this.disablePolygons = disablePolygons;
		this.debugLabels = debugLabels;
	}

	public DebugSettings() {
		this.drawTileCoordinates = false;
		this.drawTileFrames = false;
		this.debugTheme = false;
		this.disablePolygons = false;
		this.debugLabels = false;
	}
}
