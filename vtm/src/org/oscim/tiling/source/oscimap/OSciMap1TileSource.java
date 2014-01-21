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
package org.oscim.tiling.source.oscimap;

import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.common.LwHttp;
import org.oscim.tiling.source.common.UrlTileDataSource;
import org.oscim.tiling.source.common.UrlTileSource;

/**
 * Deprecated
 * 
 */
public class OSciMap1TileSource extends UrlTileSource {

	public OSciMap1TileSource(String url) {
		super(url);
	}

	@Override
	public ITileDataSource getDataSource() {
		LwHttp conn = new LwHttp(mUrl, "application/osmtile", ".osmtile", false);
		return new UrlTileDataSource(this, new TileDecoder(), conn);
	}
}
