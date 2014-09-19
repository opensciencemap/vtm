package org.oscim.test;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class MapTest extends GdxMapApp {

	@Override
	public void createLayers() {
		Map map = getMap();

		VectorTileLayer l = map.setBaseMap(new OSciMap4TileSource());

		map.layers().add(new BuildingLayer(map, l));
		map.layers().add(new LabelLayer(map, l));

		map.setTheme(VtmThemes.DEFAULT);
		map.setMapPosition(53.075, 8.808, 1 << 17);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new MapTest(), null, 400);
	}
}
