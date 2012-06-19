/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android;

import org.mapsforge.android.mapgenerator.MapGenerator;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.MapPosition;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * MapActivity is the abstract base class which must be extended in order to use a {@link MapView}. There are no
 * abstract methods in this implementation which subclasses need to override and no API key or registration is required.
 * <p>
 * A subclass may create a MapView either via one of the MapView constructors or by inflating an XML layout file. It is
 * possible to use more than one MapView at the same time.
 * <p>
 * When the MapActivity is shut down, the current center position, zoom level and map file of the MapView are saved in a
 * preferences file and restored in the next startup process.
 */
public abstract class MapActivity extends Activity {
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	private static final String KEY_MAP_FILE = "mapFile";
	private static final String KEY_ZOOM_LEVEL = "zoomLevel";
	private static final String PREFERENCES_FILE = "MapActivity";

	private static boolean containsMapViewPosition(SharedPreferences sharedPreferences) {
		return sharedPreferences.contains(KEY_LATITUDE) && sharedPreferences.contains(KEY_LONGITUDE)
				&& sharedPreferences.contains(KEY_ZOOM_LEVEL);
	}

	/**
	 * Counter to store the last ID given to a MapView.
	 */
	private int lastMapViewId;

	/**
	 * Internal list which contains references to all running MapView objects.
	 */
	private MapView mMapView;

	private void destroyMapViews() {
		mMapView.destroy();
	}

	private void restoreMapView(MapView mapView) {
		SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE);
		if (containsMapViewPosition(sharedPreferences)) {
			MapGenerator mapGenerator = mapView.getMapGenerator();
			if (!mapGenerator.requiresInternetConnection() && sharedPreferences.contains(KEY_MAP_FILE)) {
				// get and set the map file
				mapView.setMapFile(sharedPreferences.getString(KEY_MAP_FILE, null));
			}

			// get and set the map position and zoom level
			int latitudeE6 = sharedPreferences.getInt(KEY_LATITUDE, 0);
			int longitudeE6 = sharedPreferences.getInt(KEY_LONGITUDE, 0);
			int zoomLevel = sharedPreferences.getInt(KEY_ZOOM_LEVEL, -1);

			GeoPoint geoPoint = new GeoPoint(latitudeE6, longitudeE6);
			MapPosition mapPosition = new MapPosition(geoPoint, (byte) zoomLevel, 1);
			mapView.setCenterAndZoom(mapPosition);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		destroyMapViews();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mMapView.onPause();

		Editor editor = getSharedPreferences(PREFERENCES_FILE, MODE_PRIVATE).edit();
		editor.clear();

		// save the map position and zoom level
		MapPosition mapPosition = mMapView.getMapPosition().getMapPosition();
		if (mapPosition != null) {
			GeoPoint geoPoint = mapPosition.geoPoint;
			editor.putInt(KEY_LATITUDE, geoPoint.latitudeE6);
			editor.putInt(KEY_LONGITUDE, geoPoint.longitudeE6);
			editor.putInt(KEY_ZOOM_LEVEL, mapPosition.zoomLevel);
		}

		if (!mMapView.getMapGenerator().requiresInternetConnection() && mMapView.getMapFile() != null) {
			// save the map file
			editor.putString(KEY_MAP_FILE, mMapView.getMapFile());
		}

		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}

	/**
	 * @return a unique MapView ID on each call.
	 */
	final int getMapViewId() {
		return ++lastMapViewId;
	}

	/**
	 * This method is called once by each MapView during its setup process.
	 * 
	 * @param mapView
	 *            the calling MapView.
	 */
	final void registerMapView(MapView mapView) {
		if (mMapView != null)
			return;

		mMapView = mapView;
		restoreMapView(mapView);
	}
}
