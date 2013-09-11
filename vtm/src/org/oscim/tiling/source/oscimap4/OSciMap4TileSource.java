/*
 * Copyright 2013 Hannes Janetzek
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

import java.net.URL;

import org.oscim.tiling.source.ITileDataSource;
import org.oscim.tiling.source.common.LwHttp;
import org.oscim.tiling.source.common.PbfTileDataSource;
import org.oscim.tiling.source.common.UrlTileSource;

public class OSciMap4TileSource extends UrlTileSource {

	@Override
	public ITileDataSource getDataSource() {
		return new TileDataSource(mUrl);
	}

	class TileDataSource extends PbfTileDataSource {
		public TileDataSource(URL url) {
			super(new TileDecoder());
			//mConn = new LwHttp(url, "application/x-protobuf", "vtm", false);
			mConn = new LwHttp(url, "image/png", "vtm", false);
		}
	}
}
