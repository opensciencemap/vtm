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
