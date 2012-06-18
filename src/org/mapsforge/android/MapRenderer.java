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
package org.mapsforge.android;

import org.mapsforge.android.mapgenerator.MapGeneratorJob;

import android.opengl.GLSurfaceView;

/**
 *
 */
public interface MapRenderer extends GLSurfaceView.Renderer {

	/**
	 * @param mapGeneratorJob
	 *            the mapGeneratorJob holding Tile data
	 * @return true if the tile was processed
	 */
	public boolean passTile(MapGeneratorJob mapGeneratorJob);

	/**
	 * @return true when tile passed to renderer is processed false otherwise.
	 *         used to lock overwriting resources passed with the tile
	 *         (e.g. lock until bitmap is loaded to texture)
	 */
	public boolean processedTile();

	/**
	 * called by MapView on position and map changes
	 * 
	 * @param clear
	 *            ...
	 */
	public void redrawTiles(boolean clear);
}
