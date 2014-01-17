/*
 * Copyright 2014 Hannes Janetzek
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
import org.oscim.core.GeoPoint;
import org.oscim.layers.PathLayer;

import android.graphics.Color;
import android.os.Bundle;

public class PathOverlayActivity extends MapActivity {

	MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);


		for (double lon = -180; lon <= 180; lon += 10) {
			PathLayer pathLayer = new PathLayer(mMap, Color.CYAN);
			pathLayer.addGreatCircle(new GeoPoint(-90, lon), new GeoPoint(90, lon));
			mMap.getLayers().add(pathLayer);

		}

		for (double lat = -90; lat <= 90; lat += 10) {
			List<GeoPoint> pts = new ArrayList<GeoPoint>();

			for (double lon = -180; lon <= 180; lon += 10)
				pts.add(new GeoPoint(lat, lon));

			PathLayer pathLayer = new PathLayer(mMap, Color.CYAN);
			pathLayer.setPoints(pts);

			mMap.getLayers().add(pathLayer);
		}


		for (double lat = -90; lat <= 90; lat += 10) {
			PathLayer pathLayer = new PathLayer(mMap, Color.LTGRAY, 4);
			pathLayer.addGreatCircle(new GeoPoint(lat, -90), new GeoPoint(lat, 0));
			mMap.getLayers().add(pathLayer);
		}

		for (double lat = -90; lat <= 90; lat += 10) {
			PathLayer pathLayer = new PathLayer(mMap, Color.LTGRAY, 4);
			mMap.getLayers().add(pathLayer);
		}
	}
}
