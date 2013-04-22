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

import org.oscim.layers.tile.MapTile;

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
	abstract QueryResult executeQuery(MapTile tile,
			IMapDatabaseCallback mapDatabaseCallback);

	/**
	 * @return the metadata for the current map file.
	 * @throws IllegalStateException
	 *             if no map is currently opened.
	 */
	abstract MapInfo getMapInfo();

	/**
	 * @return true if a map database is currently opened, false otherwise.
	 */
	abstract boolean isOpen();

	/**
	 * Opens MapDatabase
	 *
	 * @param options
	 *            the options.
	 * @return a OpenResult containing an error message in case of a failure.
	 */
	abstract OpenResult open(MapOptions options);

	/**
	 * Closes the map file and destroys all internal caches. Has no effect if no
	 * map file is currently opened. Should also force to close Socket so that
	 * thread cannot hang in socket.read
	 */
	abstract void close();

	abstract String getMapProjection();

	/**
	 * Cancel loading
	 */
	abstract void cancel();

	public static enum QueryResult {
		SUCCESS,
		FAILED,
		TILE_NOT_FOUND,
		DELAYED,
	}

	/**
	 * A FileOpenResult is a simple DTO which is returned by
	 * IMapDatabase#open().
	 */
	public static class OpenResult {
		/**
		 * Singleton for a FileOpenResult instance with {@code success=true}.
		 */
		public static final OpenResult SUCCESS = new OpenResult();

		private final String errorMessage;
		private final boolean success;

		/**
		 * @param errorMessage
		 *            a textual message describing the error, must not be null.
		 */
		public OpenResult(String errorMessage) {
			if (errorMessage == null) {
				throw new IllegalArgumentException("error message must not be null");
			}

			this.success = false;
			this.errorMessage = errorMessage;
		}

		/**
		 *
		 */
		public OpenResult() {
			this.success = true;
			this.errorMessage = null;
		}

		/**
		 * @return a textual error description (might be null).
		 */
		public String getErrorMessage() {
			return this.errorMessage;
		}

		/**
		 * @return true if the file could be opened successfully, false
		 *         otherwise.
		 */
		public boolean isSuccess() {
			return this.success;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("FileOpenResult [success=");
			stringBuilder.append(this.success);
			stringBuilder.append(", errorMessage=");
			stringBuilder.append(this.errorMessage);
			stringBuilder.append("]");
			return stringBuilder.toString();
		}
	}

}
