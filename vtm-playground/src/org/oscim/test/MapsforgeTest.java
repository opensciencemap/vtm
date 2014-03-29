package org.oscim.test;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.mapfile.MapFileTileSource;

public class MapsforgeTest extends GdxMap {

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff888888);

		mMap.setMapPosition(53.072, 8.80, 1 << 15);
		// mMap.setMapPosition(52.5, 13.3, 1 << 15);

		MapFileTileSource tileSource = new MapFileTileSource();
		tileSource.setMapFile("/home/jeff/germany.map");

		VectorTileLayer l = mMap.setBaseMap(tileSource);

		// mMap.getLayers().add(new BuildingLayer(mMap, l.getTileLayer()));
		mMap.layers().add(new LabelLayer(mMap, l));

		// mMap.setTheme(VtmThemes.DEFAULT);
		// mMap.setTheme(VtmThemes.TRONRENDER);
		mMap.setTheme(VtmThemes.OSMARENDER);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new MapsforgeTest(), null, 400);
	}
}
