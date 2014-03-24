package org.oscim.android.test;

import org.oscim.android.cache.TileCache;
import org.oscim.layers.tile.s3db.S3DBLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;

public class S3DBMapActivity extends BaseMapActivity {

	TileCache mS3dbCache;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMap.setTheme(VtmThemes.DEFAULT);
		//mMap.setTheme(VtmThemes.TRONRENDER);
		//mMap.setTheme(VtmThemes.OSMARENDER);

		TileSource ts = new OSciMap4TileSource("http://opensciencemap.org/tiles/s3db");

		if (USE_CACHE) {
			mS3dbCache = new TileCache(this, null, "s3db.db");
			mS3dbCache.setCacheSize(512 * (1 << 10));
			ts.setCache(mS3dbCache);
		}

		mMap.layers().add(new S3DBLayer(mMap, ts));
		mMap.layers().add(new LabelLayer(mMap, mBaseLayer));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mS3dbCache != null)
			mS3dbCache.dispose();
	}

}
