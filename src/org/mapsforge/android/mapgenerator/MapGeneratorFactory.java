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

import android.util.AttributeSet;

/**
 * A factory for the internal MapGenerator implementations.
 */
public final class MapGeneratorFactory {
	private static final String MAP_GENERATOR_ATTRIBUTE_NAME = "mapGenerator";

	/**
	 * @param attributeSet
	 *            A collection of attributes which includes the desired MapGenerator.
	 * @return a new MapGenerator instance.
	 */
	public static MapGenerator createMapGenerator(AttributeSet attributeSet) {
		String mapGeneratorName = attributeSet.getAttributeValue(null, MAP_GENERATOR_ATTRIBUTE_NAME);
		if (mapGeneratorName == null) {
			return new org.mapsforge.android.glrenderer.DatabaseRenderer();
		}

		MapGeneratorInternal mapGeneratorInternal = MapGeneratorInternal.valueOf(mapGeneratorName);
		return MapGeneratorFactory.createMapGenerator(mapGeneratorInternal);
	}

	/**
	 * @param mapGeneratorInternal
	 *            the internal MapGenerator implementation.
	 * @return a new MapGenerator instance.
	 */
	public static MapGenerator createMapGenerator(MapGeneratorInternal mapGeneratorInternal) {
		switch (mapGeneratorInternal) {
			case SW_RENDERER:
				return new org.mapsforge.android.swrenderer.DatabaseRenderer();
			case GL_RENDERER:
				return new org.mapsforge.android.glrenderer.DatabaseRenderer();
		}

		throw new IllegalArgumentException("unknown enum value: " + mapGeneratorInternal);
	}

	private MapGeneratorFactory() {
		throw new IllegalStateException();
	}
}
