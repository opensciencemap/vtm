/* Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
 * Copyright 2012 osmdroidbonuspack: M.Kergall
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

import java.io.FileFilter;
import java.io.FileNotFoundException;

import org.oscim.app.filefilter.FilterByFileExtension;
import org.oscim.app.filefilter.ValidRenderTheme;
import org.oscim.app.filepicker.FilePicker;
import org.oscim.app.preferences.EditPreferences;
import org.oscim.database.MapDatabases;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.utils.AndroidUtils;
import org.oscim.view.DebugSettings;
import org.oscim.view.MapActivity;
import org.oscim.view.MapView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

public class TileMap extends MapActivity {
	static final String TAG = TileMap.class.getSimpleName();

	MapView map;

	// private static final String BUNDLE_CENTER_AT_FIRST_FIX =
	// "centerAtFirstFix";

	private static final String BUNDLE_SHOW_MY_LOCATION = "showMyLocation";
	private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
	private static final int DIALOG_ENTER_COORDINATES = 0;
	//	private static final int DIALOG_INFO_MAP_FILE = 1;
	private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;

	// private static final FileFilter FILE_FILTER_EXTENSION_MAP =
	// new FilterByFileExtension(".map");
	private static final FileFilter FILE_FILTER_EXTENSION_XML =
			new FilterByFileExtension(".xml");
	// private static final int SELECT_MAP_FILE = 0;

	// Intents
	private static final int SELECT_RENDER_THEME_FILE = 1;
	protected static final int POIS_REQUEST = 2;

	LocationHandler mLocation;

	private MapDatabases mMapDatabase;

	private WakeLock mWakeLock;
	private Menu mMenu = null;

	POISearch mPoiSearch;
	RouteSearch mRouteSearch;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// set up the layout views
		setContentView(R.layout.activity_tilemap);

		map = (MapView) findViewById(R.id.mapView);

		App.map = map;

		// configure the MapView and activate the zoomLevel buttons
		map.setClickable(true);
		// map.setBuiltInZoomControls(true);
		map.setFocusable(true);

		mLocation = new LocationHandler(this);

		// get the pointers to different system services
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

		mWakeLock = powerManager
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(BUNDLE_SHOW_MY_LOCATION)) {

				// enableShowMyLocation(savedInstanceState
				// .getBoolean(BUNDLE_CENTER_AT_FIRST_FIX));

				if (savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
					mLocation.enableSnapToLocation(false);
				}
			}
		}

		App.poiSearch = mPoiSearch = new POISearch(this);

		registerForContextMenu(map);
		mRouteSearch = new RouteSearch(this);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);
		mMenu = menu;

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.menu_info_about:
				startActivity(new Intent(this, InfoView.class));
				return true;

			case R.id.menu_position:
				return true;

			case R.id.menu_rotation_enable:
				map.enableRotation(true);
				toggleMenuRotation();
				return true;

			case R.id.menu_rotation_disable:
				map.enableRotation(false);
				toggleMenuRotation();
				return true;

			case R.id.menu_compass_enable:
				map.enableCompass(true);
				toggleMenuRotation();
				return true;

			case R.id.menu_compass_disable:
				map.enableCompass(false);
				toggleMenuRotation();
				return true;

			case R.id.menu_position_my_location_enable:
				toggleMenuItem(mMenu,
						R.id.menu_position_my_location_enable,
						R.id.menu_position_my_location_disable,
						!mLocation.enableShowMyLocation(true));
				return true;

			case R.id.menu_position_my_location_disable:
				toggleMenuItem(mMenu,
						R.id.menu_position_my_location_enable,
						R.id.menu_position_my_location_disable,
						mLocation.disableShowMyLocation());
				return true;

			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				return true;

			case R.id.menu_preferences:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.menu_render_theme:
				return true;

			case R.id.menu_render_theme_osmarender:
				map.setRenderTheme(InternalRenderTheme.OSMARENDER);
				return true;

			case R.id.menu_render_theme_tronrender:
				map.setRenderTheme(InternalRenderTheme.TRONRENDER);
				return true;

			case R.id.menu_render_theme_select_file:
				startRenderThemePicker();
				return true;

				// case R.id.menu_position_map_center:
				// // disable GPS follow mode if it is enabled
				// location.disableSnapToLocation(true);
				//
				// map.setCenter(map.getMapDatabase()
				// .getMapInfo().mapCenter);
				// return true;
				// case R.id.menu_mapfile:
				// startMapFilePicker();
				// return true;

			case R.id.menu_pois:
				mPoiSearch.getPOIAsync("bar");
				//				Intent myIntent = new Intent(this, POIActivity.class);
				//				myIntent.putParcelableArrayListExtra("POI", mPOIs);
				//				//				myIntent.putExtra("ID", poiMarkers.getBubbledItemId());
				//				startActivityForResult(myIntent, POIS_REQUEST);
				return true;

			case R.id.menu_poi_list:
				Intent myIntent = new Intent(this, POIActivity.class);
				myIntent.putParcelableArrayListExtra("POI", mPoiSearch.mPOIs);
				myIntent.putExtra("ID", mPoiSearch.poiMarkers.getBubbledItemId());
				startActivityForResult(myIntent, POIS_REQUEST);
				return true;
			default:
				return false;
		}
	}

	private void toggleMenuRotation() {

		toggleMenuItem(mMenu,
				R.id.menu_rotation_enable,
				R.id.menu_rotation_disable,
				!map.enableRotation);

		toggleMenuItem(mMenu,
				R.id.menu_compass_enable,
				R.id.menu_compass_disable,
				!map.enableCompass);
	}

	private static void toggleMenuItem(Menu menu, int id, int id2, boolean enable) {
		menu.findItem(id).setVisible(enable);
		menu.findItem(id).setEnabled(enable);
		menu.findItem(id2).setVisible(!enable);
		menu.findItem(id2).setEnabled(!enable);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {

		if (!isPreHoneyComb()) {
			menu.clear();
			onCreateOptionsMenu(menu);
		}

		toggleMenuItem(menu,
				R.id.menu_position_my_location_enable,
				R.id.menu_position_my_location_disable,
				!mLocation.isShowMyLocationEnabled());

		if (mMapDatabase == MapDatabases.MAP_READER) {
			//menu.findItem(R.id.menu_mapfile).setVisible(true);
			menu.findItem(R.id.menu_position_map_center).setVisible(true);
		}
		// else {
		// menu.findItem(R.id.menu_mapfile).setVisible(false);
		// menu.findItem(R.id.menu_position_map_center).setVisible(false);
		// }

		toggleMenuRotation();

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to the MapView
		return map.onTrackballEvent(event);
	}

	// private void startMapFilePicker() {
	// FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
	// FilePicker.setFileSelectFilter(new ValidMapFile());
	// startActivityForResult(new Intent(this, FilePicker.class),
	// SELECT_MAP_FILE);
	// }

	private void startRenderThemePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(new ValidRenderTheme());
		startActivityForResult(new Intent(this, FilePicker.class),
				SELECT_RENDER_THEME_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
			case POIS_REQUEST:
				Log.d(TAG, "result: POIS_REQUEST");
				if (resultCode == RESULT_OK) {
					int id = intent.getIntExtra("ID", 0);
					Log.d(TAG, "result: POIS_REQUEST: " + id);
					//	map.getController().setCenter(mPOIs.get(id).mLocation);
					mPoiSearch.poiMarkers.showBubbleOnItem(id, map);
					map.getMapViewPosition().animateTo(mPoiSearch.mPOIs.get(id).location);
				}
				break;
			case SELECT_RENDER_THEME_FILE:
				if (resultCode == RESULT_OK && intent != null
						&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
					try {
						map.setRenderTheme(intent
								.getStringExtra(FilePicker.SELECTED_FILE));
					} catch (FileNotFoundException e) {
						showToastOnUiThread(e.getLocalizedMessage());
					}
				}
				break;
			default:
				break;
		}

		// if (requestCode == SELECT_MAP_FILE) {
		// if (resultCode == RESULT_OK) {
		//
		// location.disableSnapToLocation(true);
		//
		// if (intent != null) {
		// if (intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
		// map.setMapFile(intent
		// .getStringExtra(FilePicker.SELECTED_FILE));
		// }
		// }
		// } else if (resultCode == RESULT_CANCELED) {
		// startActivity(new Intent(this, EditPreferences.class));
		// }
		// } else
		//		if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK
		//				&& intent != null
		//				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
		//			try {
		//				map.setRenderTheme(intent
		//						.getStringExtra(FilePicker.SELECTED_FILE));
		//			} catch (FileNotFoundException e) {
		//				showToastOnUiThread(e.getLocalizedMessage());
		//			}
		//		}
	}

	static boolean isPreHoneyComb() {
		return Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (id == DIALOG_ENTER_COORDINATES) {
			if (mLocationDialog == null)
				mLocationDialog = new LocationDialog();

			return mLocationDialog.createDialog(this);

		} else if (id == DIALOG_LOCATION_PROVIDER_DISABLED) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.no_location_provider_available);
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else {
			// no dialog will be created
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mLocation.disableShowMyLocation();
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
		// release the wake lock if necessary
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	LocationDialog mLocationDialog;

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		if (id == DIALOG_ENTER_COORDINATES) {

			mLocationDialog.prepareDialog(map, dialog);

		} else {
			super.onPrepareDialog(id, dialog);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(this);

		// MapScaleBar mapScaleBar = mapView.getMapScaleBar();
		// mapScaleBar.setShowMapScaleBar(preferences.getBoolean("showScaleBar",
		// false));
		// String scaleBarUnitDefault =
		// getString(R.string.preferences_scale_bar_unit_default);
		// String scaleBarUnit = preferences.getString("scaleBarUnit",
		// scaleBarUnitDefault);
		// mapScaleBar.setImperialUnits(scaleBarUnit.equals("imperial"));

		if (preferences.contains("mapDatabase")) {
			String name = preferences.getString("mapDatabase",
					MapDatabases.PBMAP_READER.name());

			MapDatabases mapDatabaseNew;

			try {
				mapDatabaseNew = MapDatabases.valueOf(name);
			} catch (IllegalArgumentException e) {
				mapDatabaseNew = MapDatabases.PBMAP_READER;
			}

			if (mapDatabaseNew != mMapDatabase) {
				Log.d(TAG, "set map database " + mapDatabaseNew);

				map.setMapDatabase(mapDatabaseNew);
				mMapDatabase = mapDatabaseNew;
			}
		}

		// try {
		// String textScaleDefault =
		// getString(R.string.preferences_text_scale_default);
		// map.setTextScale(Float.parseFloat(preferences.getString("textScale",
		// textScaleDefault)));
		// } catch (NumberFormatException e) {
		// map.setTextScale(1);
		// }

		if (preferences.getBoolean("fullscreen", false)) {
			Log.i("mapviewer", "FULLSCREEN");
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			Log.i("mapviewer", "NO FULLSCREEN");
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}
		if (preferences.getBoolean("fixOrientation", true)) {
			this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			// this all returns the orientation which is not currently active?!
			// getWindow().getWindowManager().getDefaultDisplay().getRotation());
			// getWindow().getWindowManager().getDefaultDisplay().getOrientation());
		}

		if (preferences.getBoolean("wakeLock", false) && !mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}

		boolean drawTileFrames =
				preferences.getBoolean("drawTileFrames", false);
		boolean drawTileCoordinates =
				preferences.getBoolean("drawTileCoordinates", false);
		boolean disablePolygons =
				preferences.getBoolean("disablePolygons", false);
		boolean drawUnmatchedWays =
				preferences.getBoolean("drawUnmatchedWays", false);

		DebugSettings cur = map.getDebugSettings();
		if (cur.mDisablePolygons != disablePolygons
				|| cur.mDrawTileCoordinates != drawTileCoordinates
				|| cur.mDrawTileFrames != drawTileFrames
				|| cur.mDrawUnmatchted != drawUnmatchedWays) {
			Log.d(TAG, "set map debug settings");

			DebugSettings debugSettings = new DebugSettings(drawTileCoordinates,
					drawTileFrames, disablePolygons, drawUnmatchedWays);

			map.setDebugSettings(debugSettings);
		}

		// if (mMapDatabase == MapDatabases.MAP_READER) {
		// if (map.getMapFile() == null)
		// startMapFilePicker();
		// } else {
		// map.setMapFile(map.getMapFile());
		// }

		map.redrawMap();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BUNDLE_SHOW_MY_LOCATION, mLocation.isShowMyLocationEnabled());
		// outState.putBoolean(BUNDLE_CENTER_AT_FIRST_FIX,
		// mMyLocationListener.isCenterAtFirstFix());

		// outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, mSnapToLocation);
	}

	/**
	 * Uses the UI thread to display the given text message as toast
	 * notification.
	 * @param text
	 *            the text message to display
	 */
	void showToastOnUiThread(final String text) {

		if (AndroidUtils.currentThreadIsUiThread()) {
			Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
			toast.show();
		} else {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast toast = Toast.makeText(TileMap.this, text, Toast.LENGTH_LONG);
					toast.show();
				}
			});
		}
	}

	//----------- Context Menu when clicking on the map
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		Log.d(TAG, "create context menu");

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_menu, menu);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		Log.d(TAG, "context menu item selected " + item.getItemId());

		if (mRouteSearch.onContextItemSelected(item))
			return true;

		return super.onContextItemSelected(item);
	}

}
