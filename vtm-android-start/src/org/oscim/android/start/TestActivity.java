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
package org.oscim.android.start;

import android.app.Activity;
import android.os.Bundle;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class TestActivity extends Activity {
    MapView mMapView;
    MapPreferences mPrefs;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        mMapView = (MapView) findViewById(R.id.mapView);
        Map map = mMapView.map();
        mPrefs = new MapPreferences(TestActivity.class.getName(), this);

        VectorTileLayer baseLayer = map.setBaseMap(new OSciMap4TileSource());
        map.layers().add(new BuildingLayer(map, baseLayer));
        map.layers().add(new LabelLayer(map, baseLayer));
        map.setTheme(VtmThemes.DEFAULT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPrefs.load(mMapView.map());
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMapView.onPause();
        mPrefs.save(mMapView.map());
    }
}
