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
package org.oscim.view;

import org.oscim.theme.RenderTheme;
import org.oscim.view.mapgenerator.IMapGenerator;
import org.oscim.view.mapgenerator.JobTile;

import android.opengl.GLSurfaceView;

/**
 *
 */
public interface IMapRenderer extends GLSurfaceView.Renderer {

	/**
	 * @param tile
	 *            Tile data ready for rendering
	 * @return true
	 */
	public boolean passTile(JobTile tile);

	/**
	 * called by MapView on position and map changes
	 * 
	 * @param clear
	 *            recreate all tiles (i.e. on theme change)
	 */
	public void updateMap(boolean clear);

	/**
	 * @return new MapGenerator Instance for this Renderer
	 */
	public IMapGenerator createMapGenerator();

	/**
	 * @param t
	 *            the theme
	 */
	public void setRenderTheme(RenderTheme t);

}
