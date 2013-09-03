package org.oscim.android.test;

import org.oscim.android.MapView;
import org.oscim.layers.labeling.LabelLayer;
import org.oscim.layers.overlay.BuildingOverlay;
import org.oscim.layers.tile.vector.MapTileLayer;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tilesource.TileSource;
import org.oscim.tilesource.oscimap4.OSciMap4TileSource;

import android.os.Bundle;
import android.view.Menu;

public class MapActivity extends org.oscim.android.MapActivity {

	private MapView mMapView;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);

		//mMap = mMap.getMap();
		//TileSource tileSource = new OSciMap2TileSource();
		//tileSource.setOption("url", "http://city.informatik.uni-bremen.de/osci/map-live");

		TileSource tileSource = new OSciMap4TileSource();
		tileSource.setOption("url", "http://city.informatik.uni-bremen.de/tiles/vtm");

		MapTileLayer l = mMap.setBaseMap(tileSource);
		//mMap.setDebugSettings(new DebugSettings(false, false, true, false, false));

		mMap.getLayers().add(new BuildingOverlay(mMap, l.getTileLayer()));
		mMap.getLayers().add(new LabelLayer(mMap, l.getTileLayer()));

		mMap.setTheme(InternalRenderTheme.DEFAULT);
		//mMap.setTheme(InternalRenderTheme.TRONRENDER);

		//mMap.getLayers().add(new BitmapTileLayer(mMap, HillShadeTiles.INSTANCE));

		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, StamenWaterTiles.INSTANCE));
		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, MapQuestAerial.INSTANCE));
		//mMap.getLayers().add(new GenericOverlay(mMap, new GridRenderLayer(mMap)));

		mMapView.setClickable(true);
		mMapView.setFocusable(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
