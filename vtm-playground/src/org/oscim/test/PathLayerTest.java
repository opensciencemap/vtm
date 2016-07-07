package org.oscim.test;

import java.util.ArrayList;
import java.util.List;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.JtsPathLayer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;

public class PathLayerTest extends GdxMapApp {

	@Override
	public void createLayers() {
		createLayers(1, true);

		mMap.setMapPosition(0, 0, 1 << 2);

		mMap.events.bind(new UpdateListener() {
			@Override
			public void onMapEvent(Event e, MapPosition mapPosition) {
				if (e == Map.UPDATE_EVENT) {
					long t = System.currentTimeMillis();
					float pos = t % 20000 / 10000f - 1f;
					createLayers(pos, false);
					mMap.updateMap(true);
				}
			}
		});
	}

	ArrayList<JtsPathLayer> mPathLayers = new ArrayList<JtsPathLayer>();

	void createLayers(float pos, boolean init) {

		int i = 0;

		for (double lat = -90; lat <= 90; lat += 5) {
			List<GeoPoint> pts = new ArrayList<GeoPoint>();

			for (double lon = -180; lon <= 180; lon += 2) {
				//pts.add(new GeoPoint(lat, lon));
				//				double longitude = lon + (pos * 180);
				//				if (longitude < -180)
				//					longitude += 360;
				//				if (longitude > 180)
				//					longitude -= 360;
				double longitude = lon;

				double latitude = lat + (pos * 90);
				if (latitude < -90)
					latitude += 180;
				if (latitude > 90)
					latitude -= 180;

				latitude += Math.sin((Math.abs(pos) * (lon / Math.PI)));

				pts.add(new GeoPoint(latitude, longitude));
			}
			JtsPathLayer pathLayer;
			if (init) {
				int c = Color.fade(Color.rainbow((float) (lat + 90) / 180), 0.5f);
				pathLayer = new JtsPathLayer(mMap, c, 6);
				mMap.layers().add(pathLayer);
				mPathLayers.add(pathLayer);
			} else {
				pathLayer = mPathLayers.get(i++);
			}

			pathLayer.setPoints(pts);
		}

	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new PathLayerTest(), null, 400);
	}
}
