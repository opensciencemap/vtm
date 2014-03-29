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
package org.oscim.android.test;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class BaseMapActivity extends MapActivity {

	final static boolean USE_CACHE = false;

	MapView mMapView;
	VectorTileLayer mBaseLayer;
	UrlTileSource mTileSource;

	private TileCache mCache;

	protected final int mContentView;

	public BaseMapActivity(int contentView) {
		mContentView = contentView;
	}

	public BaseMapActivity() {
		this(R.layout.activity_map);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(mContentView);

		mMapView = (MapView) findViewById(R.id.mapView);
		registerMapView(mMapView);

		mTileSource = new OSciMap4TileSource();
		mTileSource.setHttpEngine(new OkHttpFactory());

		if (USE_CACHE) {
			mCache = new TileCache(this, null, "tile.db");
			mCache.setCacheSize(512 * (1 << 10));
			mTileSource.setCache(mCache);
		}
		mBaseLayer = mMap.setBaseMap(mTileSource);

		/* set initial position on first run */
		MapPosition pos = new MapPosition();
		mMap.getMapPosition(pos);
		if (pos.x == 0.5 && pos.y == 0.5)
			mMap.setMapPosition(53.08, 8.83, Math.pow(2, 16));

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mCache != null)
			mCache.dispose();
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
			case R.id.theme_default:
				mMap.setTheme(VtmThemes.DEFAULT);
				item.setChecked(true);
				return true;

			case R.id.theme_tubes:
				mMap.setTheme(VtmThemes.TRONRENDER);
				item.setChecked(true);
				return true;

			case R.id.theme_osmarender:
				mMap.setTheme(VtmThemes.OSMARENDER);
				item.setChecked(true);
				return true;

			case R.id.theme_newtron:
				mMap.setTheme(VtmThemes.NEWTRON);
				item.setChecked(true);
				return true;
		}

		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.theme_menu, menu);
		return true;
	}
}
