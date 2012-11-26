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
package org.oscim.database;

import java.util.HashMap;

public class MapOptions extends HashMap<String, String> {

	private static final long serialVersionUID = 1L;

	public final MapDatabases db;

	public MapOptions(MapDatabases db) {
		this.db = db;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof MapOptions))
			return false;

		if (this.db != ((MapOptions) other).db)
			return false;

		// FIXME test if this is correct!
		if (!this.entrySet().equals(((MapOptions) other).entrySet()))
			return false;

		return true;
	}
}
