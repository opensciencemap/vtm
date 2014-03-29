/*
 * Copyright 2014 Hannes Janetzek
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
package org.oscim.android.test;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
import org.oscim.backend.canvas.Color;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;

import android.os.Bundle;

public class BitmapTileMapActivity extends MapActivity {

	private final static boolean USE_CACHE = false;
	private final BitmapTileSource mTileSource;
	protected BitmapTileLayer mBitmapLayer;

	public BitmapTileMapActivity() {
		mTileSource = new DefaultSources.OpenStreetMap();
	}

	public BitmapTileMapActivity(BitmapTileSource tileSource) {
		mTileSource = tileSource;
	}

	MapView mMapView;

	private TileCache mCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);
		registerMapView(mMapView);

		MapRenderer.setBackgroundColor(0xff777777);
		mMap.layers().add(new TileGridLayer(mMap, Color.GRAY, 1.8f, 8));

		if (USE_CACHE) {
			mCache = new TileCache(this, null, mTileSource.getClass().getSimpleName());
			mCache.setCacheSize(512 * (1 << 10));
			mTileSource.setCache(mCache);
		}

		mTileSource.setHttpEngine(new OkHttpFactory());
		mBitmapLayer = new BitmapTileLayer(mMap, mTileSource);
		mMap.layers().add(mBitmapLayer);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (USE_CACHE)
			mCache.dispose();
	}
}
