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
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeLoader;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.bitmap.DefaultSources.StamenToner;
import org.oscim.tiling.source.geojson.HighroadJsonTileSource;
import org.oscim.tiling.source.geojson.OsmBuildingJsonTileSource;
import org.oscim.tiling.source.geojson.OsmLanduseJsonTileSource;
import org.oscim.tiling.source.geojson.OsmWaterJsonTileSource;

import android.os.Bundle;

public class OsmJsonMapActivity extends MapActivity {

	MapView mMapView;
	VectorTileLayer mBaseLayer;
	TileSource mTileSource;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);
		registerMapView(mMapView);

		mTileSource = new OsmWaterJsonTileSource();

		mMap.setBackgroundMap(new BitmapTileLayer(mMap, new StamenToner()));
		mMap.layers().add(new TileGridLayer(mMap));

		IRenderTheme theme = ThemeLoader.load(VtmThemes.OSMARENDER);
		MapRenderer.setBackgroundColor(theme.getMapBackground());

		VectorTileLayer l;
		l = new VectorTileLayer(mMap, new OsmLanduseJsonTileSource());
		l.setRenderTheme(theme);
		l.tileRenderer().setOverdrawColor(0);
		mMap.layers().add(l);

		l = new VectorTileLayer(mMap, new HighroadJsonTileSource());
		l.setRenderTheme(theme);
		l.tileRenderer().setOverdrawColor(0);
		mMap.layers().add(l);

		l = new VectorTileLayer(mMap, new OsmBuildingJsonTileSource());
		l.setRenderTheme(theme);
		l.tileRenderer().setOverdrawColor(0);
		mMap.layers().add(l);
		mMap.layers().add(new BuildingLayer(mMap, l));

		mMap.setMapPosition(53.08, 8.83, Math.pow(2, 16));
	}
}
