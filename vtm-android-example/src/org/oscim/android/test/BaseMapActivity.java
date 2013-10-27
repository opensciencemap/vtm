package org.oscim.android.test;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;
import android.view.Menu;

public class BaseMapActivity extends MapActivity {

	MapView mMapView;
	VectorTileLayer mBaseLayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);

		TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", "http://opensciencemap.org/tiles/vtm");

		mBaseLayer = mMap.setBaseMap(tileSource);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
