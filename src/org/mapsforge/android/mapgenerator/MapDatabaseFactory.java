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
package org.mapsforge.android.mapgenerator;

import org.mapsforge.database.IMapDatabase;

import android.util.AttributeSet;

/**
 * 
 *
 */
public final class MapDatabaseFactory {
	private static final String MAP_DATABASE_ATTRIBUTE_NAME = "mapDatabase";

	/**
	 * @param attributeSet
	 *            A collection of attributes which includes the desired MapGenerator.
	 * @return a new MapGenerator instance.
	 */
	public static IMapDatabase createMapDatabase(AttributeSet attributeSet) {
		String mapDatabaseName = attributeSet.getAttributeValue(null,
				MAP_DATABASE_ATTRIBUTE_NAME);
		if (mapDatabaseName == null) {
			return new org.mapsforge.database.postgis.MapDatabase();
		}

		MapDatabases mapDatabaseInternal = MapDatabases.valueOf(mapDatabaseName);

		return MapDatabaseFactory.createMapDatabase(mapDatabaseInternal);
	}

	public static MapDatabases getMapDatabase(AttributeSet attributeSet) {
		String mapDatabaseName = attributeSet.getAttributeValue(null,
				MAP_DATABASE_ATTRIBUTE_NAME);
		if (mapDatabaseName == null) {
			return MapDatabases.POSTGIS_READER;
		}

		return MapDatabases.valueOf(mapDatabaseName);
	}

	/**
	 * @param mapDatabase
	 *            the internal MapDatabase implementation.
	 * @return a new MapGenerator instance.
	 */
	public static IMapDatabase createMapDatabase(MapDatabases mapDatabase) {
		switch (mapDatabase) {
			case MAP_READER:
				return new org.mapsforge.database.mapfile.MapDatabase();
			case JSON_READER:
				return new org.mapsforge.database.json.MapDatabase();
			case POSTGIS_READER:
				return new org.mapsforge.database.postgis.MapDatabase();
			case PBMAP_READER:
				return new org.mapsforge.database.pbmap.MapDatabase();

		}

		throw new IllegalArgumentException("unknown enum value: " + mapDatabase);
	}

	private MapDatabaseFactory() {
		throw new IllegalStateException();
	}
}
