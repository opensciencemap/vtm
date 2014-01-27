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
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.core.GeoPoint;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.marker.ItemizedIconLayer;
import org.oscim.layers.marker.ItemizedIconLayer.OnItemGestureListener;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerItem.HotspotPlace;
import org.oscim.layers.marker.MarkerSymbol;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

public class MarkerOverlayActivity extends MapActivity implements OnItemGestureListener<MarkerItem> {
	MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);

		mMapView = (MapView) findViewById(R.id.mapView);
		registerMapView(mMapView);

		Drawable d = getResources().getDrawable(R.drawable.marker_poi);
		MarkerSymbol symbol = AndroidGraphics.makeMarker(d, HotspotPlace.CENTER);

		ItemizedIconLayer<MarkerItem> markerLayer =
		        new ItemizedIconLayer<MarkerItem>(mMap, new ArrayList<MarkerItem>(),
		                                          symbol, this);

		mMap.getLayers().add(markerLayer);

		List<MarkerItem> pts = new ArrayList<MarkerItem>();

		for (double lat = -90; lat <= 90; lat += 5) {
			for (double lon = -180; lon <= 180; lon += 5)
				pts.add(new MarkerItem(lat + "/" + lon, "",
				                       new GeoPoint(lat, lon)));
		}

		markerLayer.addItems(pts);

		mMap.getLayers().add(new TileGridLayer(mMap));
		mMap.setMapPosition(0, 0, 1);
	}

	@Override
	public boolean onItemSingleTapUp(int index, MarkerItem item) {
		Toast toast = Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT);
		toast.show();
		return true;
	}

	@Override
	public boolean onItemLongPress(int index, MarkerItem item) {
		return false;
	}
}
