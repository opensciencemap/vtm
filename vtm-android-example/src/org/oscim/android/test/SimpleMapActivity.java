/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */package org.oscim.android.test;

import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.theme.VtmThemes;

import android.os.Bundle;

public class SimpleMapActivity extends BaseMapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Layers layers = mMap.layers();
		layers.add(new BuildingLayer(mMap, mBaseLayer));
		layers.add(new LabelLayer(mMap, mBaseLayer));

		//layers.add(new TileGridLayer(mMap));

		mMap.setTheme(VtmThemes.DEFAULT);
		//mMap.setTheme(VtmThemes.TRONRENDER);
		//mMap.setTheme(VtmThemes.TRON2);
		//mMap.setTheme(VtmThemes.OSMARENDER);

		mMap.setMapPosition(53.08, 8.83, Math.pow(2, 10));
	}

	// Stress testing
	void loooop(final int i) {
		mMapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				VtmThemes t;
				if (i == 0)
					t = VtmThemes.DEFAULT;
				else if (i == 1)
					t = VtmThemes.TRONRENDER;
				else
					t = VtmThemes.OSMARENDER;

				mMapView.getMap().setTheme(t);

				loooop((i + 1) % 3);
			}
		}, 300 + (int) (Math.random() * 200));
	}
}
