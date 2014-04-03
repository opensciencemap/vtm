package org.oscim.tiling.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.oscim.core.Tile;

import com.squareup.okhttp.OkHttpClient;

public class OkHttpEngine implements HttpEngine {
	private final OkHttpClient mClient;
	private final UrlTileSource mTileSource;

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
		mTileSource = tileSource;
	}

	@Override
	public InputStream read() throws IOException {
		return inputStream;
	}

	HttpURLConnection openConnection(Tile tile) throws MalformedURLException {
		return mClient.open(new URL(mTileSource.getUrl() +
		        mTileSource.formatTilePath(tile)));
	}

	@Override
	public boolean sendRequest(Tile tile) throws IOException {
		if (tile == null) {
			throw new IllegalArgumentException("Tile cannot be null.");
		}

		final HttpURLConnection connection = openConnection(tile);

		inputStream = connection.getInputStream();

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
