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
package org.oscim.layers.tile.bitmap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.GZIPInputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.tile.TileLayer;
import org.oscim.layers.tile.bitmap.TileSource.FadeStep;
import org.oscim.map.Map;
import org.oscim.renderer.elements.BitmapLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.utils.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitmapTileLayer extends TileLayer<TileLoader> {

	protected static final Logger log = LoggerFactory.getLogger(BitmapTileLayer.class);

	private final static int CACHE_LIMIT = 50;

	private static final int TIMEOUT_CONNECT = 5000;
	private static final int TIMEOUT_READ = 10000;

	final TileSource mTileSource;
	private final FadeStep[] mFade;

	public BitmapTileLayer(Map map, TileSource tileSource) {
		this(map, tileSource, CACHE_LIMIT);
	}

	public BitmapTileLayer(Map map, TileSource tileSource, int cacheLimit) {
		super(map, tileSource.getZoomLevelMin(), tileSource.getZoomLevelMax(), cacheLimit);
		mTileSource = tileSource;
		mFade = mTileSource.getFadeSteps();
		initLoader();
	}

	@Override
	public void onMapUpdate(MapPosition pos, boolean changed, boolean clear) {
		super.onMapUpdate(pos, changed, clear);

		if (mFade == null) {
			mRenderLayer.setBitmapAlpha(1);
			return;
		}

		float alpha = 0;
		for (FadeStep f : mFade) {
			if (pos.scale < f.scaleStart || pos.scale > f.scaleEnd)
				continue;

			if (f.alphaStart == f.alphaEnd) {
				alpha = f.alphaStart;
				break;
			}
			double range = f.scaleEnd / f.scaleStart;
			float a = (float) ((range - (pos.scale / f.scaleStart)) / range);
			a = FastMath.clamp(a, 0, 1);
			// interpolate alpha between start and end
			alpha = a * f.alphaStart + (1 - a) * f.alphaEnd;
			break;
		}

		mRenderLayer.setBitmapAlpha(alpha);
	}

	@Override
	protected TileLoader createLoader(TileManager tm) {
		return new TileLoader(tm) {

			@Override
			protected boolean executeJob(MapTile tile) {
				URL url;
				try {
					url = mTileSource.getTileUrl(tile);
					URLConnection urlConnection = getURLConnection(url);
					InputStream inputStream = getInputStream(urlConnection);
					Bitmap bitmap = CanvasAdapter.g.decodeBitmap(inputStream);

					tile.layers = new ElementLayers();
					BitmapLayer l = new BitmapLayer(false);
					l.setBitmap(bitmap, Tile.SIZE, Tile.SIZE);

					tile.layers.textureLayers = l;
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				}

				return true;
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
