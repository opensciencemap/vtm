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

import org.mapsforge.android.IMapRenderer;
import org.mapsforge.android.MapView;

import android.util.AttributeSet;

/**
 * A factory for the internal MapGenerator implementations.
 */
public final class MapRendererFactory {
	private static final String MAP_GENERATOR_ATTRIBUTE_NAME = "mapGenerator";

	/**
	 * @param mapView
	 *            ...
	 * @param attributeSet
	 *            A collection of attributes which includes the desired MapGenerator.
	 * @return a new MapGenerator instance.
	 */
	public static IMapRenderer createMapRenderer(MapView mapView,
			AttributeSet attributeSet) {
		String mapGeneratorName = attributeSet.getAttributeValue(null,
				MAP_GENERATOR_ATTRIBUTE_NAME);
		if (mapGeneratorName == null) {
			return new org.mapsforge.android.glrenderer.MapRenderer(mapView);
		}

		MapRenderers mapGeneratorInternal = MapRenderers.valueOf(mapGeneratorName);
		return MapRendererFactory.createMapRenderer(mapView, mapGeneratorInternal);
	}

	public static MapRenderers getMapGenerator(AttributeSet attributeSet) {
		String mapGeneratorName = attributeSet.getAttributeValue(null,
				MAP_GENERATOR_ATTRIBUTE_NAME);
		if (mapGeneratorName == null) {
			return MapRenderers.GL_RENDERER;
		}

		return MapRenderers.valueOf(mapGeneratorName);
	}

	/**
	 * @param mapView
	 *            ...
	 * @param mapGeneratorInternal
	 *            the internal MapGenerator implementation.
	 * @return a new MapGenerator instance.
	 */
	public static IMapRenderer createMapRenderer(MapView mapView,
			MapRenderers mapGeneratorInternal) {
		switch (mapGeneratorInternal) {
			case SW_RENDERER:
				return new org.mapsforge.android.swrenderer.MapRenderer(mapView);
			case GL_RENDERER:
				return new org.mapsforge.android.glrenderer.MapRenderer(mapView);
		}

		throw new IllegalArgumentException("unknown enum value: " + mapGeneratorInternal);
	}

	private MapRendererFactory() {
		throw new IllegalStateException();
	}
}
