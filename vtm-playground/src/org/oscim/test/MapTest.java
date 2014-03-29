package org.oscim.test;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapTest extends GdxMap {

	final Logger log = LoggerFactory.getLogger(MeshTest.class);

	@Override
	public void createLayers() {

		VectorTileLayer l = mMap.setBaseMap(new OSciMap4TileSource());
		mMap.setTheme(VtmThemes.DEFAULT);

		mMap.layers().add(new LabelLayer(mMap, l));

		mMap.setMapPosition(53.08, 8.82, 1 << 17);

	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new MapTest(), null, 400);
	}
}
