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
package org.mapsforge.database;

import java.io.File;

import org.mapsforge.core.Tile;

/**
 *
 *
 */
public interface IMapDatabase {

	/**
	 * Closes the map file and destroys all internal caches. Has no effect if no map file is currently opened.
	 */
	public abstract void closeFile();

	/**
	 * Starts a database query with the given parameters.
	 * 
	 * @param tile
	 *            the tile to read.
	 * @param mapDatabaseCallback
	 *            the callback which handles the extracted map elements.
	 * @return true if successful
	 */
	public abstract QueryResult executeQuery(Tile tile,
			IMapDatabaseCallback mapDatabaseCallback);

	/**
	 * @return the metadata for the current map file.
	 * @throws IllegalStateException
	 *             if no map is currently opened.
	 */
	public abstract MapFileInfo getMapFileInfo();

	/**
	 * @return true if a map file is currently opened, false otherwise.
	 */
	public abstract boolean hasOpenFile();

	/**
	 * Opens the given map file, reads its header data and validates them.
	 * 
	 * @param mapFile
	 *            the map file.
	 * @return a FileOpenResult containing an error message in case of a failure.
	 * @throws IllegalArgumentException
	 *             if the given map file is null.
	 */
	public abstract FileOpenResult openFile(File mapFile);

	public abstract String getMapProjection();

	/**
	 * @param position
	 *            ....
	 * @return ...
	 */
	public abstract String readString(int position);

	public abstract void cancel();

}
