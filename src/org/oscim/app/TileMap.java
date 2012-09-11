package org.oscim.app;

import java.io.FileFilter;
import java.io.FileNotFoundException;

import org.oscim.app.filefilter.FilterByFileExtension;
import org.oscim.app.filefilter.ValidMapFile;
import org.oscim.app.filefilter.ValidRenderTheme;
import org.oscim.app.filepicker.FilePicker;
import org.oscim.app.preferences.EditPreferences;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.database.MapDatabases;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.view.DebugSettings;
import org.oscim.view.MapActivity;
import org.oscim.view.MapView;
import org.oscim.view.utils.AndroidUtils;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
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
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A map application which uses the features from the mapsforge map library. The map can be centered to the current
 * location. A simple file browser for selecting the map file is also included. Some preferences can be adjusted via the
 * {@link EditPreferences} activity.
 */
public class TileMap extends MapActivity {
	// implements ActionBar.OnNavigationListener {
	// private static final String BUNDLE_CENTER_AT_FIRST_FIX = "centerAtFirstFix";
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

	LocationHandler mLocation;

	private MapDatabases mMapDatabase;

	private WakeLock mWakeLock;
	MapView mMapView;
	private Menu mMenu = null;

	SpinnerAdapter mSpinnerAdapter;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			getMenuInflater().inflate(R.menu.options_menu, menu);
		else
			getMenuInflater().inflate(R.menu.options_menu_pre_honeycomb, menu);
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
				mMapView.enableRotation(true);
				toggleMenuRotation(mMenu,
						mMapView.enableRotation,
						mMapView.enableCompass);
				return true;

			case R.id.menu_rotation_disable:
				mMapView.enableRotation(false);
				toggleMenuRotation(mMenu,
						mMapView.enableRotation,
						mMapView.enableCompass);
				return true;

			case R.id.menu_compass_enable:
				mMapView.enableCompass(true);
				toggleMenuRotation(mMenu,
						mMapView.enableRotation,
						mMapView.enableCompass);
				return true;

			case R.id.menu_compass_disable:
				mMapView.enableCompass(false);
				toggleMenuRotation(mMenu,
						mMapView.enableRotation,
						mMapView.enableCompass);
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

				// case R.id.menu_position_last_known:
				// mLocation.gotoLastKnownPosition();
				// return true;

			case R.id.menu_position_enter_coordinates:
				showDialog(DIALOG_ENTER_COORDINATES);
				return true;

			case R.id.menu_position_map_center:
				// disable GPS follow mode if it is enabled
				mLocation.disableSnapToLocation(true);

				mMapView.setCenter(mMapView.getMapDatabase()
						.getMapInfo().mapCenter);
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

			case R.id.menu_render_theme_tronrender:
				mMapView.setRenderTheme(InternalRenderTheme.TRONRENDER);
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

	private static void toggleMenuRotation(Menu menu, boolean rotate, boolean compass) {
		toggleMenuItem(menu,
				R.id.menu_rotation_enable,
				R.id.menu_rotation_disable,
				!rotate);

		toggleMenuItem(menu,
				R.id.menu_compass_enable,
				R.id.menu_compass_disable,
				!compass);
	}

	private static void toggleMenuItem(Menu menu, int id, int id2, boolean enable) {
		menu.findItem(id).setVisible(enable);
		menu.findItem(id).setEnabled(enable);
		menu.findItem(id2).setVisible(!enable);
		menu.findItem(id2).setEnabled(!enable);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		onCreateOptionsMenu(menu);

		toggleMenuItem(menu,
				R.id.menu_position_my_location_enable,
				R.id.menu_position_my_location_disable,
				!mLocation.isShowMyLocationEnabled());

		// if (mLocation.isShowMyLocationEnabled()) {
		// menu.findItem(R.id.menu_position_my_location_enable).setVisible(false);
		// menu.findItem(R.id.menu_position_my_location_enable).setEnabled(false);
		// menu.findItem(R.id.menu_position_my_location_disable).setVisible(true);
		// menu.findItem(R.id.menu_position_my_location_disable).setEnabled(true);
		// } else {
		// menu.findItem(R.id.menu_position_my_location_enable).setVisible(true);
		// menu.findItem(R.id.menu_position_my_location_enable).setEnabled(true);
		// menu.findItem(R.id.menu_position_my_location_disable).setVisible(false);
		// menu.findItem(R.id.menu_position_my_location_disable).setEnabled(false);
		// }

		menu.findItem(R.id.menu_render_theme).setEnabled(true);

		if (mMapDatabase == MapDatabases.MAP_READER) {
			menu.findItem(R.id.menu_mapfile).setVisible(true);
			menu.findItem(R.id.menu_position_map_center).setVisible(true);
		} else {
			menu.findItem(R.id.menu_mapfile).setVisible(false);
			menu.findItem(R.id.menu_position_map_center).setVisible(false);
		}

		toggleMenuItem(menu,
				R.id.menu_compass_enable,
				R.id.menu_compass_disable,
				!mMapView.enableCompass);

		toggleMenuItem(mMenu,
				R.id.menu_rotation_enable,
				R.id.menu_rotation_disable,
				!mMapView.enableRotation);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// forward the event to the MapView
		return mMapView.onTrackballEvent(event);
	}

	private void configureMapView() {
		// configure the MapView and activate the zoomLevel buttons
		mMapView.setClickable(true);
		// mMapView.setBuiltInZoomControls(true);
		mMapView.setFocusable(true);
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
		// if (requestCode == SELECT_MAP_FILE) {
		// if (resultCode == RESULT_OK) {
		//
		// mLocation.disableSnapToLocation(true);
		//
		// if (intent != null) {
		// if (intent.getStringExtra(FilePicker.SELECTED_FILE) != null) {
		// mMapView.setMapFile(intent
		// .getStringExtra(FilePicker.SELECTED_FILE));
		// }
		// }
		// } else if (resultCode == RESULT_CANCELED) {
		// startActivity(new Intent(this, EditPreferences.class));
		// }
		// } else
		if (requestCode == SELECT_RENDER_THEME_FILE && resultCode == RESULT_OK
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

	@TargetApi(11)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			mSpinnerAdapter = ArrayAdapter.createFromResource(this,
					R.array.view_sections,
					android.R.layout.simple_spinner_dropdown_item);
			ActionBar actionBar = getActionBar();
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

			// actionBar.setListNavigationCallbacks(mSpinnerAdapter, this);
			actionBar.setDisplayShowTitleEnabled(false);
		}

		// set up the layout views
		setContentView(R.layout.activity_tilemap);

		// getActionBar().setDisplayOptions(ActionBar.NAVIGATION_MODE_TABS);

		mMapView = (MapView) findViewById(R.id.mapView);

		configureMapView();

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
							mLocation.disableSnapToLocation(true);

							// set the map center and zoom level
							EditText latitudeView = (EditText) view
									.findViewById(R.id.latitude);
							EditText longitudeView = (EditText) view
									.findViewById(R.id.longitude);
							double latitude = Double.parseDouble(latitudeView.getText()
									.toString());
							double longitude = Double.parseDouble(longitudeView.getText()
									.toString());

							SeekBar zoomLevelView = (SeekBar) view
									.findViewById(R.id.zoomLevel);

							byte zoom = (byte) (zoomLevelView.getProgress());

							MapPosition mapPosition = new MapPosition(latitude,
									longitude, zoom, 1, 0);

							TileMap.this.mMapView.setMapCenter(mapPosition);
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
			zoomlevel.setMax(20); // FIXME mMapView.getMapGenerator().getZoomLevelMax());
			zoomlevel.setProgress(mMapView.getMapPosition().getZoomLevel());

			final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
			textView.setText(String.valueOf(zoomlevel.getProgress()));
			zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
			// } else if (id == DIALOG_INFO_MAP_FILE) {
			// MapInfo mapInfo = mMapView.getMapDatabase().getMapInfo();
			//
			// TextView textView = (TextView) dialog.findViewById(R.id.infoMapFileViewName);
			// textView.setText(mMapView.getMapFile());
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewSize);
			// textView.setText(FileUtils.formatFileSize(mapInfo.fileSize,
			// getResources()));
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewVersion);
			// textView.setText(String.valueOf(mapInfo.fileVersion));
			//
			// // textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDebug);
			// // if (mapFileInfo.debugFile) {
			// // textView.setText(R.string.info_map_file_debug_yes);
			// // } else {
			// // textView.setText(R.string.info_map_file_debug_no);
			// // }
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewDate);
			// Date date = new Date(mapInfo.mapDate);
			// textView.setText(DateFormat.getDateTimeInstance().format(date));
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewArea);
			// BoundingBox boundingBox = mapInfo.boundingBox;
			// textView.setText(boundingBox.getMinLatitude() + ", "
			// + boundingBox.getMinLongitude() + " - \n"
			// + boundingBox.getMaxLatitude() + ", " + boundingBox.getMaxLongitude());
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartPosition);
			// GeoPoint startPosition = mapInfo.startPosition;
			// if (startPosition == null) {
			// textView.setText(null);
			// } else {
			// textView.setText(startPosition.getLatitude() + ", "
			// + startPosition.getLongitude());
			// }
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewStartZoomLevel);
			// Byte startZoomLevel = mapInfo.startZoomLevel;
			// if (startZoomLevel == null) {
			// textView.setText(null);
			// } else {
			// textView.setText(startZoomLevel.toString());
			// }
			//
			// textView = (TextView) dialog
			// .findViewById(R.id.infoMapFileViewLanguagePreference);
			// textView.setText(mapInfo.languagePreference);
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewComment);
			// textView.setText(mapInfo.comment);
			//
			// textView = (TextView) dialog.findViewById(R.id.infoMapFileViewCreatedBy);
			// textView.setText(mapInfo.createdBy);
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
					MapDatabases.PBMAP_READER.name());

			MapDatabases mapDatabaseNew;

			try {
				mapDatabaseNew = MapDatabases.valueOf(name);
			} catch (IllegalArgumentException e) {
				mapDatabaseNew = MapDatabases.PBMAP_READER;
			}

			Log.d("VectorTileMap", "set map database " + mapDatabaseNew);

			if (mapDatabaseNew != mMapDatabase) {
				mMapView.setMapDatabase(mapDatabaseNew);
				mMapDatabase = mapDatabaseNew;
			}
		}

		// try {
		// String textScaleDefault = getString(R.string.preferences_text_scale_default);
		// mMapView.setTextScale(Float.parseFloat(preferences.getString("textScale",
		// textScaleDefault)));
		// } catch (NumberFormatException e) {
		// mMapView.setTextScale(1);
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

		DebugSettings debugSettings = new DebugSettings(drawTileCoordinates,
				drawTileFrames, disablePolygons, drawUnmatchedWays);

		mMapView.setDebugSettings(debugSettings);

		// if (mMapDatabase == MapDatabases.MAP_READER) {
		// if (mMapView.getMapFile() == null)
		// startMapFilePicker();
		// } else {
		// mMapView.setMapFile(mMapView.getMapFile());
		// }

		if (Build.VERSION.SDK_INT >= 11) {
			VersionHelper.refreshActionBarMenu(this);
		}

	}

	static class VersionHelper {
		@TargetApi(11)
		static void refreshActionBarMenu(Activity activity) {
			activity.invalidateOptionsMenu();
		}
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

	// @Override
	// public boolean onNavigationItemSelected(int arg0, long arg1) {
	// // TODO Auto-generated method stub
	// return false;
	// }

	class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
		private final TextView textView;

		SeekBarChangeListener(TextView textView) {
			this.textView = textView;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			this.textView.setText(String.valueOf(progress));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// do nothing
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// do nothing
		}
	}

}
