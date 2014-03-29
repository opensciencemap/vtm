package org.oscim.test.gdx.poi3d;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class Gdx3DTest extends GdxMap {

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff888888);

		mMap.setMapPosition(53.1, 8.8, 1 << 15);

		TileSource ts = new OSciMap4TileSource();
		// initDefaultLayers(ts, false, false, false);

		VectorTileLayer mMapLayer = mMap.setBaseMap(ts);
		mMap.setTheme(VtmThemes.DEFAULT);
		// mMap.setTheme(VtmThemes.TRONRENDER);

		mMap.layers().add(new BuildingLayer(mMap, mMapLayer));

		// mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));

		// ts = new OSciMap4TileSource("http://opensciencemap.org/tiles/s3db");
		// VectorTileLayer tl = new VectorTileLayer(mMap, 16, 16, 20);
		// tl.setTileSource(ts);
		// tl.setRenderTheme(ThemeLoader.load(VtmThemes.DEFAULT));
		// mMap.getLayers().add(tl);
		// mMap.getLayers().add(new BuildingLayer(mMap, tl.getTileLayer()));

		mMap.layers().add(new Poi3DLayer(mMap, mMapLayer));

		mMap.layers().add(new LabelLayer(mMap, mMapLayer));
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new Gdx3DTest(), null, 320);
	}
}
