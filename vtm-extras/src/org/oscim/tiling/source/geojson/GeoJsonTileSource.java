/*
 * Copyright 2014 Hannes Janetzek
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
package org.oscim.tiling.source.geojson;

import java.util.HashMap;
import java.util.Map;

import org.oscim.core.MapElement;
import org.oscim.core.Tag;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public abstract class GeoJsonTileSource extends UrlTileSource {

	public GeoJsonTileSource(String url) {
		super(url, "/{Z}/{X}/{Y}.json");
	}

	public GeoJsonTileSource(String url, int zoomMin, int zoomMax) {
		super(url, "/{Z}/{X}/{Y}.json", zoomMin, zoomMax);
	}

	@Override
	public ITileDataSource getDataSource() {
		Map<String, String> opt = new HashMap<String, String>();
		opt.put("Accept-Encoding", "gzip");
		return new UrlTileDataSource(this, new GeoJsonTileDecoder(this), getHttpEngine());
	}

	public Tag getFeatureTag() {
		return null;
	}

	/** allow overriding tag handling */
	public abstract void decodeTags(MapElement mapElement, Map<String, Object> properties);

	public Tag rewriteTag(String key, Object value) {

		if (value == null)
			return null;

		String val = (value instanceof String) ? (String) value : String.valueOf(value);

		return new Tag(key, val);
	}

	/** modify mapElement before process() */
	public void postGeomHook(MapElement mapElement) {

	}
}
