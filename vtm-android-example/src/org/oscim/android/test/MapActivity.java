package org.oscim.android.test;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public abstract class MapActivity extends ActionBarActivity {
	public static final Logger log = LoggerFactory.getLogger(MapActivity.class);
	MapView mMapView;
	Map mMap;
	MapPreferences mPrefs;

	protected final int mContentView;

	public MapActivity(int contentView) {
		mContentView = contentView;
	}

	public MapActivity() {
		this(R.layout.activity_map);
	}

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(mContentView);

		mMapView = (MapView) findViewById(R.id.mapView);
		mMap = mMapView.map();
		mPrefs = new MapPreferences(MapActivity.class.getName(), this);
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
