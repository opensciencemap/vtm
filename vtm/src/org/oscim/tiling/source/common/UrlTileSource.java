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
package org.oscim.tiling.source.common;

import java.net.MalformedURLException;
import java.net.URL;

import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.tiling.source.MapInfo;
import org.oscim.tiling.source.TileSource;

public abstract class UrlTileSource extends TileSource{
	private final static String KEY_URL = "url";

	private static final MapInfo mMapInfo =
			new MapInfo(new BoundingBox(-180, -90, 180, 90),
					new Byte((byte) 4), new GeoPoint(53.11, 8.85),
					null, 0, 0, 0, "de", "comment", "author", null);


	protected URL mUrl;

	@Override
	public OpenResult open() {
		if (!options.containsKey(KEY_URL))
			return new OpenResult("no url set");
		String urlString = options.get(KEY_URL);
		try {
			mUrl = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return new OpenResult("invalid url " + urlString);
		}

		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {

	}

	public boolean setUrl(String urlString){
		options.put("url", urlString);
		return open() == OpenResult.SUCCESS;
	}

	@Override
	public MapInfo getMapInfo() {
		return mMapInfo;
	}

}

