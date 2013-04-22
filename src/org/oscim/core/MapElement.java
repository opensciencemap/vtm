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
package org.oscim.core;

/**
 * MapElement is created by MapDatabase(s) and passed to MapTileLoader
 * via IMapDatabaseCallback.renderElement() MapTileLoader processes the
 * data into MapTile.layers.
 * -----
 * This is really just a buffer object that belongs to MapDatabase, so
 * dont keep a reference to it when passed as parameter.
 */
public class MapElement extends GeometryBuffer {
	public static final int GEOM_NONE = 0;
	public static final int GEOM_POINT = 1;
	public static final int GEOM_LINE = 2;
	public static final int GEOM_POLY = 3;

	// osm layer of the way.
	public int layer;
	// osm tags of the way.
	public Tag[] tags;
	// GEOM_*
	public int geometryType;

	// ---- random stuff, to be removed ----
	// building tags
	public int height;
	public int minHeight;

	public int priority;

	public MapElement() {
		super(4096, 128);
	}

	public MapElement(int points, int indices) {
		super(points, indices);
	}

	public void set(Tag[] tags, int layer, int type) {
		this.layer = layer;
		this.tags = tags;
		this.geometryType = type;
	}
}
