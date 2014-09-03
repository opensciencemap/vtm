/*
 * Copyright 2014 Charles Greb
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
package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map.Entry;

import org.oscim.core.Tile;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.okhttp.OkHttpClient;

public class OkHttpEngine implements HttpEngine {
	private final OkHttpClient mClient;
	private final UrlTileSource mTileSource;
	static final Logger log = LoggerFactory.getLogger(OkHttpEngine.class);

	public static class OkHttpFactory implements HttpEngine.Factory {
		private final OkHttpClient mClient;

		public OkHttpFactory() {
			mClient = new OkHttpClient();
		}

		@Override
		public HttpEngine create(UrlTileSource tileSource) {
			return new OkHttpEngine(mClient, tileSource);
		}
	}

	private InputStream inputStream;

	public OkHttpEngine(OkHttpClient client, UrlTileSource tileSource) {
		mClient = client;
		mClient.setResponseCache(tileSource.getResponseCache());
		mTileSource = tileSource;
	}

	@Override
	public InputStream read() throws IOException {
		return inputStream;
	}

	@Override
	public void sendRequest(Tile tile) throws IOException {
		if (tile == null) {
			throw new IllegalArgumentException("Tile cannot be null.");
		}
		URL url = new URL(mTileSource.getTileUrl(tile));
		HttpURLConnection conn = mClient.open(url);

		for (Entry<String, String> opt : mTileSource.getRequestHeader().entrySet())
			conn.addRequestProperty(opt.getKey(), opt.getValue());

		inputStream = conn.getInputStream();
	}

	@Override
	public void close() {
		if (inputStream == null)
			return;

		final InputStream is = inputStream;
		inputStream = null;
		new Thread(new Runnable() {
			@Override
			public void run() {
				IOUtils.closeQuietly(is);
			}
		}).start();
	}

	@Override
	public void setCache(OutputStream os) {
		// OkHttp cache implented through tileSource setResponseCache
	}

	@Override
	public boolean requestCompleted(boolean success) {
		IOUtils.closeQuietly(inputStream);
		inputStream = null;

		return success;
	}
}
