package org.oscim.test;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class S3DBLayerTest extends GdxMapApp {

	@Override
	public void createLayers() {

		//VectorTileLayer l = mMap.setBaseMap(new OSciMap4TileSource());
		//mMap.setTheme(VtmThemes.DEFAULT);

		mMap.setBaseMap(new BitmapTileLayer(mMap, DefaultSources.STAMEN_TONER.build()));

		TileSource ts = OSciMap4TileSource
		    .builder()
		    .url("http://opensciencemap.org/tiles/s3db")
		    .build();

		S3DBLayer tl = new S3DBLayer(mMap, ts);
		mMap.layers().add(tl);

		mMap.setMapPosition(53.08, 8.82, 1 << 17);

	}

	public static void main(String[] args) {
		init();
		run(new S3DBLayerTest(), null, 400);
	}
}
