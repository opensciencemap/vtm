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
package org.oscim.view.mapgenerator;

import org.oscim.database.IMapDatabase;
import org.oscim.theme.RenderTheme;

/**
 * A MapGenerator provides map tiles either by downloading or rendering them.
 */
public interface IMapGenerator {
	/**
	 * Called once at the end of the MapGenerator lifecycle.
	 */
	void cleanup();

	/**
	 * Called when a job needs to be executed.
	 * 
	 * @param tile
	 *            the job that should be executed.
	 * @return true if the job was executed successfully, false otherwise.
	 */
	boolean executeJob(JobTile tile);

	/**
	 * @param mapDatabase
	 *            the MapDatabase from which the map data will be read.
	 */
	void setMapDatabase(IMapDatabase mapDatabase);

	IMapDatabase getMapDatabase();

	void setRenderTheme(RenderTheme theme);
}
