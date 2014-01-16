/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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

// TODO
// - make this class (and description) more generic or
// move it to tilesource package

/**
 * The MapElement class is a reusable containter for a geometry
 * with tags.
 * MapElement is created by TileDataSource(s) and passed to
 * MapTileLoader via ITileDataSink.process().
 * This is just a buffer that belongs to TileDataSource,
 * so dont keep a reference to it when passed as parameter.
 */
public class MapElement extends GeometryBuffer {

	/** OSM layer of the way */
	public int layer;

	/** priority for labeling */
	public int priority;

	public final TagSet tags = new TagSet();

	public MapElement() {
		super(1024, 16);
	}

	public MapElement(int points, int indices) {
		super(points, indices);
	}

	public void setLayer(int layer) {
		this.layer = layer;
	}

	@Override
	public void clear() {
		super.clear();
	}
}
