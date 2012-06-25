package org.mapsforge.tilemap;

import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.util.Date;

import org.mapsforge.android.DebugSettings;
import org.mapsforge.android.MapActivity;
import org.mapsforge.android.MapController;
import org.mapsforge.android.MapView;
import org.mapsforge.android.mapgenerator.MapDatabaseFactory;
import org.mapsforge.android.mapgenerator.MapDatabaseInternal;
import org.mapsforge.android.mapgenerator.MapGenerator;
import org.mapsforge.android.rendertheme.InternalRenderTheme;
import org.mapsforge.android.utils.AndroidUtils;
import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.MapFileInfo;
import org.mapsforge.tilemap.filefilter.FilterByFileExtension;
import org.mapsforge.tilemap.filefilter.ValidMapFile;
import org.mapsforge.tilemap.filefilter.ValidRenderTheme;
import org.mapsforge.tilemap.filepicker.FilePicker;
import org.mapsforge.tilemap.preferences.EditPreferences;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * A map application which uses the features from the mapsforge map library. The map can be centered to the current
 * location. A simple file browser for selecting the map file is also included. Some preferences can be adjusted via the
 * {@link EditPreferences} activity.
 */
public class TileMap extends MapActivity implements ActionBar.OnNavigationListener {
	private static final String BUNDLE_CENTER_AT_FIRST_FIX = "centerAtFirstFix";
	private static final String BUNDLE_SHOW_MY_LOCATION = "showMyLocation";
	private static final String BUNDLE_SNAP_TO_LOCATION = "snapToLocation";
	private static final int DIALOG_ENTER_COORDINATES = 0;
	private static final int DIALOG_INFO_MAP_FILE = 1;
	private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;
	private static final FileFilter FILE_FILTER_EXTENSION_MAP =
			new FilterByFileExtension(".map");
	private static final FileFilter FILE_FILTER_EXTENSION_XML =
			new FilterByFileExtension(".xml");
	private static final int SELECT_MAP_FILE = 0;
	private static final int SELECT_RENDER_THEME_FILE = 1;
	private LocationManager mLocationManager;
	private MapDatabaseInternal mMapDatabaseInternal;
	private MyLocationListener mMyLocationListener;
	private boolean mShowMyLocation;
	private boolean mSnapToLocation;
	private ToggleButton mSnapToLocationView;
	private WakeLock mWakeLock;
	MapController mMapController;
	MapView mMapView;
	private Menu mMenu = null;

	SpinnerAdapter mSpinnerAdapter;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.options_menu, menu);

		mMenu = menu;
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		// case R.id.menu_info_map_file:
		// showDialog(DIALOG_INFO_MAP_FILE);
		// return true;

			case R.id.menu_position:
				return true;

			case R.id.menu_position_my_location_enable:
				if (enableShowMyLocation(true)) {
					mMenu.findItem(R.id.menu_position_my_location_enable)
							.setVisible(false);
					mMenu.findItem(R.id.menu_position_my_location_enable)
							.setEnabled(false);
					mMenu.findItem(R.id.menu_position_my_location_disable)
							.setVisible(true);
					mMenu.findItem(R.id.menu_position_my_location_disable)
							.setEnabled(true);
				}
				return true;

			case R.id.menu_position_my_location_disable:
				if (disableShowMyLocation()) {
					mMenu.findItem(R.id.menu_position_my_location_enable)
							.setVisible(true);
					mMenu.findItem(R.id.menu_position_my_location_enable)
							.setEnabled(true);
					mMenu.findItem(R.id.menu_position_my_location_disable)
							.setVisible(false);
					mMenu.findItem(R.id.menu_position_my_location_disable)
							.setEnabled(false);
				}
				return true;

			case R.id.menu_position_last_known:
				gotoLastKnownPosition();
				return true;

			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				return true;

			case R.id.menu_position_map_center:
				// disable GPS follow mode if it is enabled
				disableSnapToLocation(true);
				mMapController.setCenter(mMapView.getMapDatabase()
						.getMapFileInfo().mapCenter);
				return true;

			case R.id.menu_preferences:
				startActivity(new Intent(this, EditPreferences.class));
				return true;

			case R.id.menu_render_theme:
				return true;

			case R.id.menu_options:
				return true;

			case R.id.menu_render_theme_osmarender:
				mMapView.setRenderTheme(InternalRenderTheme.OSMARENDER);
				return true;

			case R.id.menu_render_theme_select_file:
				startRenderThemePicker();
				return true;

			case R.id.menu_mapfile:
				startMapFilePicker();
				return true;

			default:
				return false;
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		MapGenerator mapGenerator = mMapView.getMapGenerator();

		// if (mapGenerator.requiresInternetConnection()) {
		// menu.findItem(R.id.menu_info_map_file).setEnabled(false);
		// } else {
		// menu.findItem(R.id.menu_info_map_file).setEnabled(true);
		// }

		if (isShowMyLocationEnabled()) {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(false);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(true);
		} else {
			menu.findItem(R.id.menu_position_my_location_enable).setVisible(true);
			menu.findItem(R.id.menu_position_my_location_enable).setEnabled(true);
			menu.findItem(R.id.menu_position_my_location_disable).setVisible(false);
			menu.findItem(R.id.menu_position_my_location_disable).setEnabled(false);
		}

		if (mapGenerator.requiresInternetConnection()) {
			menu.findItem(R.id.menu_position_map_center).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_position_map_center).setEnabled(true);
		}

		if (mapGenerator.requiresInternetConnection()) {
			menu.findItem(R.id.menu_render_theme).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_render_theme).setEnabled(true);
		}

		if (mapGenerator.requiresInternetConnection()) {
			menu.findItem(R.id.menu_mapfile).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_mapfile).setEnabled(true);
		}

		return true;
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to the MapView
		return mMapView.onTrackballEvent(event);
	}

	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		mMapView.setClickable(true);
		mMapView.setBuiltInZoomControls(true);
		mMapView.setFocusable(true);

		mMapController = mMapView.getController();
	}

	private boolean enableShowMyLocation(boolean centerAtFirstFix) {
		if (!mShowMyLocation) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			String bestProvider = mLocationManager.getBestProvider(criteria, true);
			if (bestProvider == null) {
				showDialog(DIALOG_LOCATION_PROVIDER_DISABLED);
				return false;
			}

			mShowMyLocation = true;
			mMyLocationListener.setCenterAtFirstFix(centerAtFirstFix);
			mLocationManager.requestLocationUpdates(bestProvider, 1000, 0,
					mMyLocationListener);
			mSnapToLocationView.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}

	private void gotoLastKnownPosition() {
		Location currentLocation;
		Location bestLocation = null;
		for (String provider : mLocationManager.getProviders(true)) {
			currentLocation = mLocationManager.getLastKnownLocation(provider);
			if (currentLocation == null)
				continue;
			if (bestLocation == null
					|| currentLocation.getAccuracy() < bestLocation.getAccuracy()) {
				bestLocation = currentLocation;
			}
		}

		if (bestLocation != null) {
			GeoPoint point = new GeoPoint(bestLocation.getLatitude(),
					bestLocation.getLongitude());
			mMapController.setCenter(point);
		} else {
			showToastOnUiThread(getString(R.string.error_last_location_unknown));
		}
	}

	private void startMapFilePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_MAP);
		FilePicker.setFileSelectFilter(new ValidMapFile());
		startActivityForResult(new Intent(this, FilePicker.class), SELECT_MAP_FILE);
	}

	private void startRenderThemePicker() {
		FilePicker.setFileDisplayFilter(FILE_FILTER_EXTENSION_XML);
		FilePicker.setFileSelectFilter(new ValidRenderTheme());
		startActivityForResult(new Intent(this, FilePicker.class),
				SELECT_RENDER_THEME_FILE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == SELECT_MAP_FILE) {
			if (resultCode == RESULT_OK) {

				disableSnapToLocation(true);

				if (intent != null) {
					if (intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
						mMapView.setMapFile(intent
								.getStringExtra(FilePicker.SELECTED_FILE));
					}
				}
			} else if (resultCode == RESULT_CANCELED) {
				startActivity(new Intent(this, EditPreferences.class));
				// && !mapView.getMapGenerator().requiresInternetConnection()
				// && mapView.getMapFile() == null) {
				// finish();
			}
		} else if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK
				&& intent != null
				&& intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
			try {
				mMapView.setRenderTheme(intent
						.getStringExtra(FilePicker.SELECTED_FILE));
			} catch (FileNotFoundException e) {
				showToastOnUiThread(e.getLocalizedMessage());
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSpinnerAdapter = ArrayAdapter.createFromResource(this,
				R.array.view_sections,
				android.R.layout.simple_spinner_dropdown_item);
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
		actionBar.setDisplayShowTitleEnabled(false);

		// getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
		// getActionBar().setBackgroundDrawable(
		// getResources().getDrawable(R.drawable.action_bar));
		// // getActionBar().setDisplayShowTitleEnabled(false);
		// getActionBar().setIcon(R.drawable.bar_globe2);

		// set up the layout views
		setContentView(R.layout.activity_advanced_map_viewer);

		// getActionBar().setDisplayOptions(ActionBar.NAVIGATION_MODE_TABS);

		mMapView = (MapView) findViewById(R.id.mapView);
		configureMapView();

		mSnapToLocationView = (ToggleButton) findViewById(R.id.snapToLocationView);
		mSnapToLocationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isSnapToLocationEnabled()) {
					disableSnapToLocation(true);
				} else {
					enableSnapToLocation(true);
				}
			}
		});

		// get the pointers to different system services
		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mMyLocationListener = new MyLocationListener(this);
		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager
				.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "AMV");

		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(BUNDLE_SHOW_MY_LOCATION)) {

				enableShowMyLocation(savedInstanceState
						.getBoolean(BUNDLE_CENTER_AT_FIRST_FIX));

				if (savedInstanceState.getBoolean(BUNDLE_SNAP_TO_LOCATION)) {
					enableSnapToLocation(false);
				}
			}
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		if (id == DIALOG_ENTER_COORDINATES) {
			builder.setIcon(android.R.drawable.ic_menu_mylocation);
			builder.setTitle(R.string.menu_position_enter_coordinates);
			LayoutInflater factory = LayoutInflater.from(this);
			final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
			builder.setView(view);
			builder.setPositiveButton(R.string.go_to_position,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// disable GPS follow mode if it is enabled
							disableSnapToLocation(true);

							// set the map center and zoom level
							EditText latitudeView = (EditText) view
									.findViewById(R.id.latitude);
							EditText longitudeView = (EditText) view
									.findViewById(R.id.longitude);
							double latitude = Double.parseDouble(latitudeView.getText()
									.toString());
							double longitude = Double.parseDouble(longitudeView.getText()
									.toString());
							GeoPoint geoPoint = new GeoPoint(latitude, longitude);
							TileMap.this.mMapController.setCenter(geoPoint);
							SeekBar zoomLevelView = (SeekBar) view
									.findViewById(R.id.zoomLevel);
							TileMap.this.mMapController.setZoom(zoomLevelView
									.getProgress());
						}
					});
			builder.setNegativeButton(R.string.cancel, null);
			return builder.create();
		} else if (id == DIALOG_LOCATION_PROVIDER_DISABLED) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.error);
			builder.setMessage(R.string.no_location_provider_available);
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else if (id == DIALOG_INFO_MAP_FILE) {
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setTitle(R.string.menu_info_map_file);
			LayoutInflater factory = LayoutInflater.from(this);
			builder.setView(factory.inflate(R.layout.dialog_info_map_file, null));
			builder.setPositiveButton(R.string.ok, null);
			return builder.create();
		} else {
			// do dialog will be created
			return null;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		disableShowMyLocation();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// release the wake lock if necessary
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}

	@Override
	protected void onPrepareDialog(int id, final Dialog dialog) {
		if (id == DIALOG_ENTER_COORDINATES) {
			EditText editText = (EditText) dialog.findViewById(R.id.latitude);
			GeoPoint mapCenter = mMapView.getMapPosition().getMapCenter();
			editText.setText(Double.toString(mapCenter.getLatitude()));

			editText = (EditText) dialog.findViewById(R.id.longitude);
			editText.setText(Double.toString(mapCenter.getLongitude()));

			SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
			zoomlevel.setMax(mMapView.getMapGenerator().getZoomLevelMax());
			zoomlevel.setProgress(mMapView.getMapPosition().getZoomLevel());

			final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
			textView.setText(String.valueOf(zoomlevel.getProgress()));
			zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
		} else if (id == DIALOG_INFO_MAP_FILE) {
			MapFileInfo mapFileInfo = mMapView.getMapDatabase().getMapFileInfo();

			TextView textView = (TextView) dialog.findViewById(R.id.infoMapFileViewName);
			textView.setText(mMapView.getMapFile());

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewSize);
			textView.setText(FileUtils.formatFileSize(mapFileInfo.fileSize,
					getResources()));

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewVersion);
			textView.setText(String.valueOf(mapFileInfo.fileVersion));

			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDebug);
			// if (mapFileInfo.debugFile) {
			// textView.setText(R.string.info_map_file_debug_yes);
			// } else {
			// textView.setText(R.string.info_map_file_debug_no);
			// }

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDate);
			Date date = new Date(mapFileInfo.mapDate);
			textView.setText(DateFormat.getDateTimeInstance().format(date));

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewArea);
			BoundingBox boundingBox = mapFileInfo.boundingBox;
			textView.setText(boundingBox.getMinLatitude() + ", "
					+ boundingBox.getMinLongitude() + " - \n"
					+ boundingBox.getMaxLatitude() + ", " + boundingBox.getMaxLongitude());

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartPosition);
			GeoPoint startPosition = mapFileInfo.startPosition;
			if (startPosition == null) {
				textView.setText(null);
			} else {
				textView.setText(startPosition.getLatitude() + ", "
						+ startPosition.getLongitude());
			}

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartZoomLevel);
			Byte startZoomLevel = mapFileInfo.startZoomLevel;
			if (startZoomLevel == null) {
				textView.setText(null);
			} else {
				textView.setText(startZoomLevel.toString());
			}

			textView = (TextView) dialog
					.findViewById(R.id.infoMapFileViewLanguagePreference);
			textView.setText(mapFileInfo.languagePreference);

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewComment);
			textView.setText(mapFileInfo.comment);

			textView = (TextView) dialog.findViewById(R.id.infoMapFileViewCreatedBy);
			textView.setText(mapFileInfo.createdBy);
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
		// mapScaleBar.setShowMapScaleBar(preferences.getBoolean("showScaleBar", false));
		// String scaleBarUnitDefault = getString(R.string.preferences_scale_bar_unit_default);
		// String scaleBarUnit = preferences.getString("scaleBarUnit", scaleBarUnitDefault);
		// mapScaleBar.setImperialUnits(scaleBarUnit.equals("imperial"));

		// if (preferences.contains("mapGenerator")) {
		// String name = preferences.getString("mapGenerator", MapGeneratorInternal.SW_RENDERER.name());
		// MapGeneratorInternal mapGeneratorInternalNew;
		// try {
		// mapGeneratorInternalNew = MapGeneratorInternal.valueOf(name);
		// } catch (IllegalArgumentException e) {
		// mapGeneratorInternalNew = MapGeneratorInternal.SW_RENDERER;
		// }
		//
		// if (mapGeneratorInternalNew != mapGeneratorInternal) {
		// MapGenerator mapGenerator = MapGeneratorFactory.createMapGenerator(mapGeneratorInternalNew);
		// mapView.setMapGenerator(mapGenerator);
		// mapGeneratorInternal = mapGeneratorInternalNew;
		// }
		// }

		if (preferences.contains("mapDatabase")) {
			String name = preferences.getString("mapDatabase",
					MapDatabaseInternal.POSTGIS_READER.name());

			MapDatabaseInternal mapDatabaseInternalNew;

			try {
				mapDatabaseInternalNew = MapDatabaseInternal.valueOf(name);
			} catch (IllegalArgumentException e) {
				mapDatabaseInternalNew = MapDatabaseInternal.POSTGIS_READER;
			}

			Log.d("VectorTileMap", "set map database " + mapDatabaseInternalNew);

			if (mapDatabaseInternalNew != mMapDatabaseInternal) {
				IMapDatabase mapDatabase = MapDatabaseFactory
						.createMapDatabase(mapDatabaseInternalNew);
				mMapView.setMapDatabase(mapDatabase);
				mMapDatabaseInternal = mapDatabaseInternalNew;
			}
		}

		try {
			String textScaleDefault = getString(R.string.preferences_text_scale_default);
			mMapView.setTextScale(Float.parseFloat(preferences.getString("textScale",
					textScaleDefault)));
		} catch (NumberFormatException e) {
			mMapView.setTextScale(1);
		}

		if (preferences.getBoolean("fullscreen", false)) {
			Log.i("mapviewer", "FULLSCREEN");
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
			Log.i("mapviewer", "NO FULLSCREEN");
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
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

		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates,
				drawTileFrames, disablePolygons);

		mMapView.setDebugSettings(debugSettings);

		if (mMapDatabaseInternal == MapDatabaseInternal.MAP_READER) {
			if (mMapView.getMapFile() == null)
				startMapFilePicker();
		} else {
			mMapView.setMapFile(null);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(BUNDLE_SHOW_MY_LOCATION, isShowMyLocationEnabled());
		outState.putBoolean(BUNDLE_CENTER_AT_FIRST_FIX,
				mMyLocationListener.isCenterAtFirstFix());
		outState.putBoolean(BUNDLE_SNAP_TO_LOCATION, mSnapToLocation);
	}

	/**
	 * Disables the "show my location" mode.
	 * 
	 * @return ...
	 */
	private boolean disableShowMyLocation() {
		if (mShowMyLocation) {
			mShowMyLocation = false;
			disableSnapToLocation(false);
			mLocationManager.removeUpdates(mMyLocationListener);
			// if (circleOverlay != null) {
			// mapView.getOverlays().remove(circleOverlay);
			// mapView.getOverlays().remove(itemizedOverlay);
			// circleOverlay = null;
			// itemizedOverlay = null;
			// }
			mSnapToLocationView.setVisibility(View.GONE);
			return true;
		}
		return false;
	}

	/**
	 * Disables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void disableSnapToLocation(boolean showToast) {
		if (mSnapToLocation) {
			mSnapToLocation = false;
			mSnapToLocationView.setChecked(false);
			mMapView.setClickable(true);
			if (showToast) {
				showToastOnUiThread(getString(R.string.snap_to_location_disabled));
			}
		}
	}

	/**
	 * Enables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void enableSnapToLocation(boolean showToast) {
		if (!mSnapToLocation) {
			mSnapToLocation = true;
			mMapView.setClickable(false);
			if (showToast) {
				showToastOnUiThread(getString(R.string.snap_to_location_enabled));
			}
		}
	}

	/**
	 * Returns the status of the "show my location" mode.
	 * 
	 * @return true if the "show my location" mode is enabled, false otherwise.
	 */
	boolean isShowMyLocationEnabled() {
		return mShowMyLocation;
	}

	/**
	 * Returns the status of the "snap to location" mode.
	 * 
	 * @return true if the "snap to location" mode is enabled, false otherwise.
	 */
	boolean isSnapToLocationEnabled() {
		return mSnapToLocation;
	}

	/**
	 * Uses the UI thread to display the given text message as toast notification.
	 * 
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

	@Override
	public boolean onNavigationItemSelected(int arg0, long arg1) {
		// TODO Auto-generated method stub
		return false;
	}
}
