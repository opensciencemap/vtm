package org.oscim.android.test;

import org.oscim.android.AndroidMapView;
import org.oscim.layers.labeling.LabelLayer;
import org.oscim.layers.overlay.BuildingOverlay;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.bitmap.HillShadeTiles;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap2.OSciMap2TileSource;

import android.os.Bundle;
import android.view.Menu;

public class MapActivity extends org.oscim.android.MapActivity {

	private AndroidMapView mAndroidMapView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mAndroidMapView = (AndroidMapView) findViewById(R.id.mapView);

		//mMap = mMapView.getMap();
		TileSource tileSource = new OSciMap2TileSource();
		tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/map-live");

		//TileSource tileSource = new OSciMap4TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/testing");

		MapTileLayer l = mMapView.setBaseMap(tileSource);
		//mMapView.setDebugSettings(new DebugSettings(false, false, true, false, false));

		mMapView.getLayerManager().add(new BuildingOverlay(mMapView, l.getTileLayer()));
		mMapView.getLayerManager().add(new LabelLayer(mMapView, l.getTileLayer()));

		mMapView.setTheme(InternalRenderTheme.DEFAULT);
		//mMapView.setTheme(InternalRenderTheme.TRONRENDER);

		mMapView.getLayerManager().add(new BitmapTileLayer(mMapView, HillShadeTiles.INSTANCE));

		//mMapView.setBackgroundMap(new BitmapTileLayer(mMapView, StamenWaterTiles.INSTANCE));
		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, MapQuestAerial.INSTANCE));
		//mMapView.getLayerManager().add(new GenericOverlay(mMapView, new GridRenderLayer(mMapView)));

		mAndroidMapView.setClickable(true);
		mAndroidMapView.setFocusable(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
