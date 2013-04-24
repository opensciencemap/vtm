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
package org.oscim.layers.tile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.oscim.layers.tile.bitmap.TileSource;
import org.oscim.renderer.layer.BitmapLayer;
import org.oscim.renderer.layer.Layers;
import org.oscim.view.MapView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BitmapTileLayer extends TileLayer<TileLoader> {
	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_READ = 10000;

	final TileSource mTileSource;

	public BitmapTileLayer(MapView mapView, TileSource tileSource) {
		super(mapView);
		mTileSource = tileSource;
	}

	@Override
	protected TileLoader createLoader(JobQueue q, TileManager tm) {
		return new TileLoader(q, tm) {

			@Override
			protected boolean executeJob(MapTile tile) {
				URL url;
				try {
					url = mTileSource.getTileUrl(tile);
					URLConnection urlConnection = getURLConnection(url);
					InputStream inputStream = getInputStream(urlConnection);
					Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

					tile.layers = new Layers();
					BitmapLayer l = new BitmapLayer();
					l.setBitmap(bitmap);

					tile.layers.textureLayers = l;

				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}
				return false;
			}

			@Override
			public void cleanup() {
			}

			private InputStream getInputStream(URLConnection urlConnection) throws IOException {
				if ("gzip".equals(urlConnection.getContentEncoding())) {
					return new GZIPInputStream(urlConnection.getInputStream());
				}
				return urlConnection.getInputStream();
			}

			private URLConnection getURLConnection(URL url) throws IOException {
				URLConnection urlConnection = url.openConnection();
				urlConnection.setConnectTimeout(TIMEOUT_CONNECT);
				urlConnection.setReadTimeout(TIMEOUT_READ);
				return urlConnection;
			}
		};
	}
}
