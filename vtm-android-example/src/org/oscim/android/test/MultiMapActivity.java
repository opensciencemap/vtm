/*
 * Copyright 2016 devemux86
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

import android.app.Activity;
import android.os.Bundle;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.oscim.theme.VtmThemes;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;


public class MultiMapActivity extends Activity {
    MapView mMapView1;
    MapView mMapView2;
    Map mMap1;
    Map mMap2;
    MapPreferences mPrefs;

    VectorTileLayer mBaseLayer1;
    VectorTileLayer mBaseLayer2;
    TileSource mTileSource;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tile.SIZE = Tile.calculateTileSize(getResources().getDisplayMetrics().scaledDensity);
	setContentView(R.layout.activity_map_multi);

        setTitle(getClass().getSimpleName());

        mTileSource = new OSciMap4TileSource();

        mMapView1 = (MapView) findViewById(R.id.mapView1);
        mMap1 = mMapView1.map();
        mBaseLayer1 = mMap1.setBaseMap(mTileSource);

        GroupLayer groupLayer1 = new GroupLayer(mMap1);
        groupLayer1.layers.add(new BuildingLayer(mMap1, mBaseLayer1));
        groupLayer1.layers.add(new LabelLayer(mMap1, mBaseLayer1));
        mMap1.layers().add(groupLayer1);
	mMap1.setTheme(VtmThemes.DEFAULT);

        mMapView2 = (MapView) findViewById(R.id.mapView2);
        mMap2 = mMapView2.map();
        mBaseLayer2 = mMap2.setBaseMap(mTileSource);

        GroupLayer groupLayer2 = new GroupLayer(mMap2);
        groupLayer2.layers.add(new BuildingLayer(mMap2, mBaseLayer2));
        groupLayer2.layers.add(new LabelLayer(mMap2, mBaseLayer2));
        mMap2.layers().add(groupLayer2);
	mMap2.setTheme(VtmThemes.DEFAULT);

        mPrefs = new MapPreferences(MapActivity.class.getName(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPrefs.load(mMapView1.map());
        mMapView1.onResume();
        mMapView2.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMapView1.onPause();
        mMapView2.onPause();
        mPrefs.save(mMapView1.map());
    }
}
