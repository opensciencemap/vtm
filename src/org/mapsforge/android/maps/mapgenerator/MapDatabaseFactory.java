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
package org.mapsforge.android.maps.mapgenerator;

import org.mapsforge.map.IMapDatabase;

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
		String mapDatabaseName = attributeSet.getAttributeValue(null, MAP_DATABASE_ATTRIBUTE_NAME);
		if (mapDatabaseName == null) {
			return new org.mapsforge.map.reader.MapDatabase();
		}

		MapDatabaseInternal mapDatabaseInternal = MapDatabaseInternal.valueOf(mapDatabaseName);
		return MapDatabaseFactory.createMapDatabase(mapDatabaseInternal);
	}

	/**
	 * @param mapDatabaseInternal
	 *            the internal MapDatabase implementation.
	 * @return a new MapGenerator instance.
	 */
	public static IMapDatabase createMapDatabase(MapDatabaseInternal mapDatabaseInternal) {
		switch (mapDatabaseInternal) {
			case MAP_READER:
				return new org.mapsforge.map.reader.MapDatabase();
			case JSON_READER:
				return new org.mapsforge.map.jsonreader.MapDatabase();
			case POSTGIS_READER:
				return new org.mapsforge.map.postgisreader.MapDatabase();

		}

		throw new IllegalArgumentException("unknown enum value: " + mapDatabaseInternal);
	}

	private MapDatabaseFactory() {
		throw new IllegalStateException();
	}
}
