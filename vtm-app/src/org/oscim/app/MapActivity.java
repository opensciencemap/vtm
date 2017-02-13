/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2017 devemux86
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
package org.oscim.app;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.oscim.android.MapView;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.map.Map;

/**
 * MapActivity is the abstract base class which can be extended in order to use
 * a {@link MapView}. There are no abstract methods in this implementation which
 * subclasses need to override and no API key or registration is required.
 * <p/>
 * A subclass may create a MapView either via one of the MapView constructors or
 * by inflating an XML layout file.
 * <p/>
 * When the MapActivity is shut down, the current center position, zoom level
 * and map file of the MapView are saved in a preferences file and restored in
 * the next startup process.
 */
public abstract class MapActivity extends Activity {
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_MAP_SCALE = "map_scale";

    private static final String PREFERENCES_FILE = "MapActivity";

    private static boolean containsViewport(SharedPreferences sharedPreferences) {
        return sharedPreferences.contains(KEY_LATITUDE)
                && sharedPreferences.contains(KEY_LONGITUDE)
                && sharedPreferences.contains(KEY_MAP_SCALE);
    }

    protected Map mMap;
    protected MapView mMapView;

    public Map map() {
        return mMap;
    }

    @Override
    protected void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Editor editor = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE).edit();
        editor.clear();

        // save the map position
        MapPosition mapPosition = new MapPosition();

        mMap.viewport().getMapPosition(mapPosition);

        GeoPoint geoPoint = mapPosition.getGeoPoint();

        editor.putInt(KEY_LATITUDE, geoPoint.latitudeE6);
        editor.putInt(KEY_LONGITUDE, geoPoint.longitudeE6);
        editor.putFloat(KEY_MAP_SCALE, (float) mapPosition.scale);

        editor.apply();

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    protected void onStop() {
        mMapView.onPause();
        super.onStop();
    }

    /**
     * This method is called once by each MapView during its setup process.
     *
     * @param mapView the calling MapView.
     */
    public final void registerMapView(MapView mapView) {
        mMapView = mapView;
        mMap = mapView.map();

        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE,
                MODE_PRIVATE);

        if (containsViewport(sharedPreferences)) {
            // get and set the map position and zoom level
            int latitudeE6 = sharedPreferences.getInt(KEY_LATITUDE, 0);
            int longitudeE6 = sharedPreferences.getInt(KEY_LONGITUDE, 0);
            float scale = sharedPreferences.getFloat(KEY_MAP_SCALE, 1);

            MapPosition mapPosition = new MapPosition();
            mapPosition.setPosition(latitudeE6 / 1E6, longitudeE6 / 1E6);
            mapPosition.setScale(scale);

            mMap.setMapPosition(mapPosition);
        }
    }
}
