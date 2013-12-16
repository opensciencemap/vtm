package org.oscim.android.test;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.android.cache.TileCache;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;
import android.view.Menu;

public class BaseMapActivity extends MapActivity {

	MapView mMapView;
	VectorTileLayer mBaseLayer;
	TileSource mTileSource;

	private TileCache mCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);

		mTileSource = new OSciMap4TileSource();
		mTileSource.setOption("url", "http://opensciencemap.org/tiles/vtm");


		mCache = new TileCache(this, "cachedir", "testdb");
		mCache.setCacheSize(512 * (1 << 10));
		mTileSource.setCache(mCache);

		mBaseLayer = mMap.setBaseMap(mTileSource);
	}

	@Override
	protected void onDestroy() {
	    super.onDestroy();
	    mCache.dispose();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
