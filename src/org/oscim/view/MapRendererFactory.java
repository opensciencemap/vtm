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
package org.oscim.view;

import android.util.AttributeSet;

/**
 * A factory for the internal MapRenderer implementations.
 */
public final class MapRendererFactory {
	private static final String MAP_RENDERER_ATTRIBUTE_NAME = "mapRenderer";

	/**
	 * @param mapView
	 *            ...
	 * @param attributeSet
	 *            A collection of attributes which includes the desired MapRenderer.
	 * @return a new MapRenderer instance.
	 */
	public static IMapRenderer createMapRenderer(MapView mapView,
			AttributeSet attributeSet) {
		String mapRendererName = attributeSet.getAttributeValue(null,
				MAP_RENDERER_ATTRIBUTE_NAME);
		if (mapRendererName == null) {
			return new org.oscim.view.glrenderer.MapRenderer(mapView);
		}

		MapRenderers mapRendererInternal = MapRenderers.valueOf(mapRendererName);
		return MapRendererFactory.createMapRenderer(mapView, mapRendererInternal);
	}

	public static MapRenderers getMapRenderer(AttributeSet attributeSet) {
		String mapRendererName = attributeSet.getAttributeValue(null,
				MAP_RENDERER_ATTRIBUTE_NAME);
		if (mapRendererName == null) {
			return MapRenderers.GL_RENDERER;
		}

		return MapRenderers.valueOf(mapRendererName);
	}

	/**
	 * @param mapView
	 *            ...
	 * @param mapRendererInternal
	 *            the internal MapRenderer implementation.
	 * @return a new MapRenderer instance.
	 */
	public static IMapRenderer createMapRenderer(MapView mapView,
			MapRenderers mapRendererInternal) {
		switch (mapRendererInternal) {
			case SW_RENDERER:
				return new org.oscim.view.swrenderer.MapRenderer(mapView);
			case GL_RENDERER:
				return new org.oscim.view.glrenderer.MapRenderer(mapView);
		}

		throw new IllegalArgumentException("unknown enum value: " + mapRendererInternal);
	}

	private MapRendererFactory() {
		throw new IllegalStateException();
	}
}
