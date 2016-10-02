/*
 * Copyright 2016 Marvin W
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
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class MultiMapActivity extends Activity {
    private MapView mMapView1, mMapView2;
    private MapPreferences mPrefs1, mPrefs2;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tile.SIZE = Tile.calculateTileSize(getResources().getDisplayMetrics().scaledDensity);
        setContentView(R.layout.activity_map_multi);

        setTitle(getClass().getSimpleName());

        // 1st map view
        mMapView1 = (MapView) findViewById(R.id.mapView1);
        Map map1 = mMapView1.map();
        mPrefs1 = new MapPreferences(MultiMapActivity.class.getName() + "1", this);
        VectorTileLayer baseLayer1 = map1.setBaseMap(new OSciMap4TileSource());
        map1.layers().add(new BuildingLayer(map1, baseLayer1));
        map1.layers().add(new LabelLayer(map1, baseLayer1));
        map1.setTheme(VtmThemes.DEFAULT);

        // 2nd map view
        mMapView2 = (MapView) findViewById(R.id.mapView2);
        Map map2 = mMapView2.map();
        mPrefs2 = new MapPreferences(MultiMapActivity.class.getName() + "2", this);
        VectorTileLayer baseLayer2 = map2.setBaseMap(new OSciMap4TileSource());
        map2.layers().add(new BuildingLayer(map2, baseLayer2));
        map2.layers().add(new LabelLayer(map2, baseLayer2));
        map2.setTheme(VtmThemes.DEFAULT);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPrefs1.load(mMapView1.map());
        mMapView1.onResume();

        mPrefs2.load(mMapView2.map());
        mMapView2.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mMapView1.onPause();
        mPrefs1.save(mMapView1.map());

        mMapView2.onPause();
        mPrefs2.save(mMapView2.map());
    }
}
