package org.oscim.test;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.s3db.S3DBLayer;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class MeshTest extends GdxMapApp {

	@Override
	public void createLayers() {
		//MapRenderer.setBackgroundColor(0xf0f0f0);

		//VectorTileLayer l = mMap.setBaseMap(new OSciMap4TileSource());
		//mMap.setTheme(VtmThemes.DEFAULT);

		mMap.setBackgroundMap(new BitmapTileLayer(mMap, new DefaultSources.StamenToner()));

		TileSource ts = new OSciMap4TileSource("http://opensciencemap.org/tiles/s3db");
		S3DBLayer tl = new S3DBLayer(mMap, ts);

		//BuildingLayer tl = new BuildingLayer(mMap, l);

		//OffscreenRenderer or = new OffscreenRenderer(mMap.getWidth(),
		//                                            mMap.getHeight());
		//or.setRenderer(tl.getRenderer());

		mMap.layers().add(tl);

		//mMap.layers().add(new GenericLayer(mMap, or));

		//mMap.layers().add(new LabelLayer(mMap, l));

		//mMap.setMapPosition(7.707, 81.689, 1 << 17);

		mMap.setMapPosition(53.08, 8.82, 1 << 17);

	}

	public static void main(String[] args) {
		init();
		run(new MeshTest(), null, 400);
	}
}
