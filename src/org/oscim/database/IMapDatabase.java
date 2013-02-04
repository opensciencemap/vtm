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
package org.oscim.database;

import org.oscim.generator.JobTile;

/**
 *
 *
 */
public interface IMapDatabase {

	/**
	 * Starts a database query with the given parameters.
	 *
	 * @param tile
	 *            the tile to read.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 * @return true if successful
	 */
	public abstract QueryResult executeQuery(JobTile tile,
			IMapDatabaseCallback mapDatabaseCallback);

	/**
	 * @return the metadata for the current map file.
	 * @throws IllegalStateException
	 *             if no map is currently opened.
	 */
	public abstract MapInfo getMapInfo();

	/**
	 * @return true if a map database is currently opened, false otherwise.
	 */
	public abstract boolean isOpen();

	/**
	 * Opens MapDatabase
	 *
	 * @param options
	 *            the options.
	 * @return a OpenResult containing an error message in case of a failure.
	 */
	public abstract OpenResult open(MapOptions options);

	/**
	 * Closes the map file and destroys all internal caches. Has no effect if no
	 * map file is currently opened. Should also force to close Socket so that
	 * thread cannot hang in socket.read
	 */
	public abstract void close();

	public abstract String getMapProjection();

	/**
	 * Cancel loading
	 */
	public abstract void cancel();

}
