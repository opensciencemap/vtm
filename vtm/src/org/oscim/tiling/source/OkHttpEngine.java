package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.oscim.core.Tile;

import com.squareup.okhttp.OkHttpClient;

public class OkHttpEngine implements HttpEngine {
	private final OkHttpClient client;

	public static class OkHttpFactory implements HttpEngine.Factory {
		private final OkHttpClient client;

		public OkHttpFactory() {
			this.client = new OkHttpClient();
		}

		@Override
		public HttpEngine create() {
			return new OkHttpEngine(client);
		}
	}

	private InputStream inputStream;

	public OkHttpEngine(OkHttpClient client) {
		this.client = client;
	}

	@Override
	public InputStream read() throws IOException {
		return inputStream;
	}

	@Override
	public boolean sendRequest(UrlTileSource tileSource, Tile tile) throws IOException {
		if (tile == null) {
			throw new IllegalArgumentException("Tile cannot be null.");
		}

		final URL requestUrl = new URL(tileSource.getUrl()
		        + "/"
		        + Byte.toString(tile.zoomLevel)
		        + "/"
		        + tile.tileX
		        + "/"
		        + tile.tileY
		        + ".vtm");

		final HttpURLConnection connection = client.open(requestUrl);

		try {
			inputStream = connection.getInputStream();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	@Override
	public void close() {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void setCache(OutputStream os) {
		// TODO: Evaluate OkHttp response cache and determine if additional caching is required.
	}

	@Override
	public boolean requestCompleted(boolean success) {
		close();
		return success;
	}
}
