/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.tiling.source;

import org.oscim.tiling.MapTile;

/**
 *
 *
 */
public interface ITileDataSource {

	/**
	 * Starts a database query with the given parameters.
	 *
	 * @param tile
	 *            the tile to read.
	 * @param mapDataSink
	 *            the callback which handles the extracted map elements.
	 * @return true if successful
	 */
	abstract QueryResult executeQuery(MapTile tile,
			ITileDataSink mapDataSink);


	abstract void destroy();

	public static enum QueryResult {
		SUCCESS,
		FAILED,
		TILE_NOT_FOUND,
		DELAYED,
	}
}
