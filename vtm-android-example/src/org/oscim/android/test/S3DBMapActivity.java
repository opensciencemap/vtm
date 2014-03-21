package org.oscim.android.test;

import org.oscim.layers.tile.s3db.S3DBLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

import android.os.Bundle;

public class S3DBMapActivity extends BaseMapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMap.setTheme(VtmThemes.DEFAULT);
		//mMap.setTheme(VtmThemes.TRONRENDER);
		//mMap.setTheme(VtmThemes.OSMARENDER);

		TileSource ts = new OSciMap4TileSource("http://opensciencemap.org/tiles/s3db");
		mMap.layers().add(new S3DBLayer(mMap, ts));

		mMap.layers().add(new LabelLayer(mMap, mBaseLayer));
		mMap.setMapPosition(53.08, 8.83, Math.pow(2, 14));
	}
}
