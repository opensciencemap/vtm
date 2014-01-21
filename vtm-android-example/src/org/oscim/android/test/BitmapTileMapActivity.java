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
import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.tiling.source.bitmap.DefaultSources.OpenStreetMap;

import android.os.Bundle;

public class BitmapTileMapActivity extends MapActivity {
	MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);
		registerMapView(mMapView);

		mMap.getLayers().add(new BitmapTileLayer(mMap, new OpenStreetMap(), 20));
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new ImagicoLandcover(), 20));
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new ArcGISWorldShaded(), 20));
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new HillShadeHD(), 20));

		mMap.setMapPosition(0, 0, 1 << 2);
	}
}
