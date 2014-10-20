package org.oscim.android.start;

import org.oscim.android.MapPreferences;
import org.oscim.android.MapView;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

public class TestActivity extends ActionBarActivity {
	public static final Logger log = LoggerFactory.getLogger(TestActivity.class);
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
