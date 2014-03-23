package org.oscim.android.start;

import org.oscim.android.MapActivity;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;

public class TestActivity extends MapActivity {
	public static final Logger log = LoggerFactory.getLogger(TestActivity.class);

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		Map map = this.map();

		VectorTileLayer baseLayer = map.setBaseMap(new OSciMap4TileSource());
		map.layers().add(new BuildingLayer(map, baseLayer));
		map.layers().add(new LabelLayer(map, baseLayer));
		map.setTheme(VtmThemes.DEFAULT);

		//mMap.setMapPosition(49.417, 8.673, 1 << 17);
		map.setMapPosition(53.5620092, 9.9866457, 1 << 16);

		//	mMap.layers().add(new TileGridLayer(mMap));
	}
}
