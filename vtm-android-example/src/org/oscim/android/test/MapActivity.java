package org.oscim.android.test;

import org.oscim.android.MapView;
import org.oscim.core.MapPosition;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tiling.source.TileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

import android.os.Bundle;
import android.os.Environment;
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
		//tileSource.setOption("url", "http://opensciencemap.org/osci/map-live");

		//TileSource tileSource = new OSciMap4TileSource();
		//tileSource.setOption("url", "http://opensciencemap.org/tiles/vtm");

		TileSource tileSource = new MapFileTileSource();
		tileSource.setOption("file", Environment.getExternalStorageDirectory() + "/germany.map");

		//GarminImgTileSource tileSource = new GarminImgTileSource();
		//tileSource.setMapFile(Environment.getExternalStorageDirectory() + "/62760103.img");

		VectorTileLayer l = mMap.setBaseMap(tileSource);

		//mMap.getLayers().add(new BuildingLayer(mMap, l.getTileLayer()));
		mMap.getLayers().add(new LabelLayer(mMap, l.getTileLayer()));

		//mMap.setTheme(InternalRenderTheme.DEFAULT);
		//mMap.setTheme(InternalRenderTheme.TRONRENDER);
		mMap.setTheme(InternalRenderTheme.OSMARENDER);

		//	try {
		//		IRenderTheme theme = ThemeLoader.load("freizeitkarte/theme.xml");
		//		l.setRenderTheme(theme);
		//		MapRenderer.setBackgroundColor(theme.getMapBackground());
		//	} catch (FileNotFoundException e) {
		//		e.printStackTrace();
		//	}

		//mMap.getLayers().add(new BitmapTileLayer(mMap, HillShadeTiles.INSTANCE));

		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, StamenWaterTiles.INSTANCE));
		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, MapQuestAerial.INSTANCE));

		//mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));

		//mMap.getLayers().add(new JeoMapLayer(mMap));

		MapPosition p = new MapPosition();
		p.setZoomLevel(14);
		p.setPosition(53.08, 8.83);
		mMap.setMapPosition(p);

		mMapView.setClickable(true);
		mMapView.setFocusable(true);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}
}
