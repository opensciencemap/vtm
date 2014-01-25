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
import org.oscim.theme.InternalRenderTheme;
import org.oscim.tiling.TileRenderer;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

public class SimpleMapActivity extends BaseMapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		TileRenderer l = mBaseLayer.getTileRenderer();
		Layers layers = mMap.getLayers();
		layers.add(new BuildingLayer(mMap, l));
		layers.add(new LabelLayer(mMap, l));

		//layers.add(new TileGridLayer(mMap));

		mMap.setTheme(InternalRenderTheme.DEFAULT);
		//mMap.setTheme(InternalRenderTheme.TRONRENDER);
		//mMap.setTheme(InternalRenderTheme.OSMARENDER);

		mMap.setMapPosition(53.08, 8.83, Math.pow(2, 14));

		//loooop(0);
	}

	void loooop(final int i) {
		mMapView.postDelayed(new Runnable() {
			@Override
			public void run() {
				InternalRenderTheme t;
				if (i == 0)
					t = InternalRenderTheme.DEFAULT;
				else if (i == 1)
					t = InternalRenderTheme.TRONRENDER;
				else
					t = InternalRenderTheme.OSMARENDER;

				mMapView.getMap().setTheme(t);

				loooop((i + 1) % 3);
			}
		}, 300 + (int)(Math.random() * 200));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.theme_menu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
			case R.id.theme_default:
				mMap.setTheme(InternalRenderTheme.DEFAULT);
				item.setChecked(true);
				return true;

			case R.id.theme_tubes:
				mMap.setTheme(InternalRenderTheme.TRONRENDER);
				item.setChecked(true);
				return true;

			case R.id.theme_osmarender:
				mMap.setTheme(InternalRenderTheme.OSMARENDER);
				item.setChecked(true);
				return true;
		}

		return false;
	}
}
