package org.oscim.tiling.source;

import com.squareup.okhttp.OkHttpClient;

import org.oscim.core.Tile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class OkHttpEngine implements HttpEngine {
	private final URL baseUrl;
	private final OkHttpClient client;
	private InputStream inputStream;

	public OkHttpEngine(URL baseUrl) {
		this.baseUrl = baseUrl;
		this.client = new OkHttpClient();
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

		final URL requestUrl = new URL(baseUrl.toString()
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
