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
package org.oscim.view;

import java.io.FileNotFoundException;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.theme.InternalRenderTheme;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * MapActivity is the abstract base class which must be extended in order to use
 * a {@link MapView}. There are no abstract methods in this implementation which
 * subclasses need to override and no API key or registration is required.
 * <p>
 * A subclass may create a MapView either via one of the MapView constructors or
 * by inflating an XML layout file. It is possible to use more than one MapView
 * at the same time.
 * <p>
 * When the MapActivity is shut down, the current center position, zoom level
 * and map file of the MapView are saved in a preferences file and restored in
 * the next startup process.
 */
public abstract class MapActivity extends Activity {
	private static final String KEY_LATITUDE = "latitude";
	private static final String KEY_LONGITUDE = "longitude";
	// private static final String KEY_MAP_FILE = "mapFile";
	private static final String KEY_ZOOM_LEVEL = "zoomLevel";
	private static final String PREFERENCES_FILE = "MapActivity";
	private static final String KEY_THEME = "Theme";

	private static boolean containsMapViewPosition(SharedPreferences sharedPreferences) {
		return sharedPreferences.contains(KEY_LATITUDE)
				&& sharedPreferences.contains(KEY_LONGITUDE)
				&& sharedPreferences.contains(KEY_ZOOM_LEVEL);
	}

	private MapView mMapView;

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mMapView.destroy();
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
			GeoPoint geoPoint = new GeoPoint(mapPosition.lat, mapPosition.lon);
			editor.putInt(KEY_LATITUDE, geoPoint.latitudeE6);
			editor.putInt(KEY_LONGITUDE, geoPoint.longitudeE6);
			editor.putInt(KEY_ZOOM_LEVEL, mapPosition.zoomLevel);
		}

		// if (mMapView.getMapFile() != null) {
		// // save the map file
		// editor.putString(KEY_MAP_FILE, mMapView.getMapFile());
		// }

		editor.putString(KEY_THEME, mMapView.getRenderTheme());

		editor.commit();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mMapView.onResume();
	}

	@Override
	protected void onStop() {
		super.onStop();
		mMapView.onStop();
	}

	/**
	 * This method is called once by each MapView during its setup process.
	 *
	 * @param mapView
	 *            the calling MapView.
	 */
	final void registerMapView(MapView mapView) {
		mMapView = mapView;

		SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_FILE,
				MODE_PRIVATE);

		if (containsMapViewPosition(sharedPreferences)) {
			//
			// if (sharedPreferences.contains(KEY_MAP_FILE)) {
			// // get and set the map file
			// mapView.setMapFile(sharedPreferences.getString(KEY_MAP_FILE,
			// null));
			// }

			// get and set the map position and zoom level
			int latitudeE6 = sharedPreferences.getInt(KEY_LATITUDE, 0);
			int longitudeE6 = sharedPreferences.getInt(KEY_LONGITUDE, 0);
			int zoomLevel = sharedPreferences.getInt(KEY_ZOOM_LEVEL, -1);

			GeoPoint geoPoint = new GeoPoint(latitudeE6, longitudeE6);
			MapPosition mapPosition = new MapPosition(geoPoint, (byte) zoomLevel, 1);

			mMapView.getMapViewPosition().setMapCenter(mapPosition);
			// mapView.setMapCenter(mapPosition);
		}

		String theme = sharedPreferences.getString(KEY_THEME,
				InternalRenderTheme.DEFAULT.name());

		if (theme.startsWith("/")) {
			try {
				mapView.setRenderTheme(theme);
			} catch (FileNotFoundException e) {
				mapView.setRenderTheme(InternalRenderTheme.DEFAULT);
			}
		} else {
			try {
				mapView.setRenderTheme(InternalRenderTheme.valueOf(theme));
			} catch (IllegalArgumentException e) {
				mapView.setRenderTheme(InternalRenderTheme.DEFAULT);
			}
		}
	}
}
