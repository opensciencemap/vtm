/*
 * Copyright 2014 Hannes Janetzek
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
 */
package org.oscim.android.test;

import java.util.ArrayList;
import java.util.List;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;

import android.os.Bundle;

public class PathOverlayActivity extends MapActivity {

	MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);

		for (double lon = -180; lon < 180; lon += 5) {
			List<GeoPoint> pts = new ArrayList<GeoPoint>();

			for (double lat = -90; lat <= 90; lat += 5)
				//pts.add(new GeoPoint(lat, lon));
				pts.add(new GeoPoint(lat, Math.sin((lat + 180) / 360 * Math.PI) * lon));

			PathLayer pathLayer = new PathLayer(mMap,
			                                    Color.rainbow((float) (lon + 180) / 360),
			                                    3);
			pathLayer.setPoints(pts);

			mMap.getLayers().add(pathLayer);
		}

		for (double lat = -90; lat <= 90; lat += 5) {
			List<GeoPoint> pts = new ArrayList<GeoPoint>();

			for (double lon = -180; lon <= 180; lon += 1)
				//pts.add(new GeoPoint(lat, lon));
				pts.add(new GeoPoint(Math.sin(lon/6 * Math.PI) * 3 + lat, lon));

			PathLayer pathLayer = new PathLayer(mMap,
			                                    Color.rainbow((float) (lat + 90) / 180),
			                                    3);
			pathLayer.setPoints(pts);

			mMap.getLayers().add(pathLayer);
		}

		mMap.setMapPosition(0, 0, 1);
	}
}
