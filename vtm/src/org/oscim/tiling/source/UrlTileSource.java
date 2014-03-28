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
package org.oscim.tiling.source;

import java.net.MalformedURLException;
import java.net.URL;

import org.oscim.core.Tile;
import org.oscim.tiling.TileSource;

public abstract class UrlTileSource extends TileSource {

	private final URL mUrl;
	private byte[] mExt;
	private HttpEngine httpEngine;

	public UrlTileSource(String urlString) {
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		mUrl = url;
	}

	public UrlTileSource(String url, int zoomMin, int zoomMax) {
		this(url);
		mZoomMin = zoomMin;
		mZoomMax = zoomMax;
	}

	@Override
	public OpenResult open() {
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {

	}

	protected void setExtension(String ext) {
		if (ext == null) {
			mExt = null;
			return;
		}
		mExt = ext.getBytes();
	}

	protected void setMimeType(String string) {

	}

	/**
	 * Create url path for tile
	 */
	protected String getTileUrl(Tile tile) {
		return null;
	}

	/**
	 * Write tile url - the low level, no-allocations method,
	 * 
	 * override getTileUrl() for custom url formatting using
	 * Strings
	 * 
	 * @param tile the Tile
	 * @param buf to write url string
	 * @param pos current position
	 * @return new position
	 */
	public int formatTilePath(Tile tile, byte[] buf, int pos) {
		String p = getTileUrl(tile);
		if (p != null) {
			byte[] b = p.getBytes();
			System.arraycopy(b, 0, buf, pos, b.length);
			return pos + b.length;
		}

		buf[pos++] = '/';
		pos = LwHttp.writeInt(tile.zoomLevel, pos, buf);
		buf[pos++] = '/';
		pos = LwHttp.writeInt(tile.tileX, pos, buf);
		buf[pos++] = '/';
		pos = LwHttp.writeInt(tile.tileY, pos, buf);
		if (mExt == null)
			return pos;

		System.arraycopy(mExt, 0, buf, pos, mExt.length);
		return pos + mExt.length;
	}

	public URL getUrl() {
		return mUrl;
	}

	public void setHttpEngine(HttpEngine httpEngine) {
		this.httpEngine = httpEngine;
	}

	public HttpEngine getHttpEngine() {
		if (httpEngine == null) {
			httpEngine = new LwHttp(getUrl());
		}

		return httpEngine;
	}
}
