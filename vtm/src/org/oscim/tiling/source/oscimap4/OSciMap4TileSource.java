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
package org.oscim.tiling.source.oscimap4;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class OSciMap4TileSource extends UrlTileSource {

	public OSciMap4TileSource() {
		this("http://opensciencemap.org/tiles/vtm");
	}

	public OSciMap4TileSource(String url) {
		super(url);
		setExtension(".vtm");
	}

	@Override
	public ITileDataSource getDataSource() {
		return new UrlTileDataSource(this, new TileDecoder(), getHttpEngine());
	}
}
