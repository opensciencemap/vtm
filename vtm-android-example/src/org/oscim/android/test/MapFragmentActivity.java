/*
 * Copyright 2016 Mathieu De Brito
 * Copyright 2017 devemux86
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

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class MapFragmentActivity extends FragmentActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tile.SIZE = Tile.calculateTileSize(getResources().getDisplayMetrics().scaledDensity);
        setContentView(R.layout.activity_map_fragment);

        setTitle(getClass().getSimpleName());

        MapFragment newFragment = new MapFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment_container, newFragment)
                .commit();
    }

    public static class MapFragment extends Fragment {

        private MapView mMapView;
        private MapPreferences mPrefs;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View contentView = super.onCreateView(inflater, container, savedInstanceState);
            if (contentView == null) {
                contentView = inflater.inflate(R.layout.fragment_map, container, false);
            }
            return contentView;
        }

        @Override
        public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            mMapView = (MapView) view.findViewById(R.id.mapView);
            mPrefs = new MapPreferences(MapFragment.class.getName(), getContext());

            VectorTileLayer l = mMapView.map().setBaseMap(new OSciMap4TileSource());

            GroupLayer groupLayer = new GroupLayer(mMapView.map());
            groupLayer.layers.add(new BuildingLayer(mMapView.map(), l));
            groupLayer.layers.add(new LabelLayer(mMapView.map(), l));
            mMapView.map().layers().add(groupLayer);

            mMapView.map().setTheme(VtmThemes.DEFAULT);

            // set initial position on first run
            MapPosition pos = new MapPosition();
            mMapView.map().getMapPosition(pos);
            if (pos.x == 0.5 && pos.y == 0.5)
                mMapView.map().setMapPosition(53.08, 8.83, Math.pow(2, 16));
        }

        @Override
        public void onResume() {
            super.onResume();

            mPrefs.load(mMapView.map());
            mMapView.onResume();
        }

        @Override
        public void onPause() {
            super.onPause();

            mMapView.onPause();
            mPrefs.save(mMapView.map());
        }
    }
}
