/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.tiling.source.mapnik;

import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.LwHttp;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class MapnikVectorTileSource extends UrlTileSource {

	public MapnikVectorTileSource() {
		super("http://d1s11ojcu7opje.cloudfront.net/dev/764e0b8d");
	}

	@Override
	public ITileDataSource getDataSource() {
		return new UrlTileDataSource(this, new TileDecoder(), new LwHttp(getUrl()));
	}

	public int formatTilePath(Tile tile, byte[] path, int pos) {
		// url formatter for mapbox streets
		byte[] hexTable = {
		        '0', '1', '2', '3',
		        '4', '5', '6', '7',
		        '8', '9', 'a', 'b',
		        'c', 'd', 'e', 'f'
		};

		path[pos++] = '/';
		path[pos++] = hexTable[(tile.tileX) % 16];
		path[pos++] = hexTable[(tile.tileY) % 16];
		path[pos++] = '/';
		pos = LwHttp.writeInt(tile.zoomLevel, pos, path);
		path[pos++] = '/';
		pos = LwHttp.writeInt(tile.tileX, pos, path);
		path[pos++] = '/';
		pos = LwHttp.writeInt(tile.tileY, pos, path);

		return pos;
	}
}
