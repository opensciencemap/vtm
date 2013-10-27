package org.oscim.android.test;

import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.InternalRenderTheme;

import android.os.Bundle;

public class SimpleMapActivity extends BaseMapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMap.getLayers().add(new BuildingLayer(mMap, mBaseLayer.getTileLayer()));
		mMap.getLayers().add(new LabelLayer(mMap, mBaseLayer.getTileLayer()));

		//mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));

		mMap.setTheme(InternalRenderTheme.DEFAULT);
		//mMap.setTheme(InternalRenderTheme.TRONRENDER);
		//mMap.setTheme(InternalRenderTheme.OSMARENDER);

		mMap.setMapPosition(53.08, 8.83, Math.pow(2, 14));
	}
}
