package org.oscim.gdx.client;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.map.Map;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

public class UrlUpdater extends Timer {
	private int curLon, curLat, curZoom, curTilt, curRot;
	private MapPosition pos = new MapPosition();
	private final Map mMap;
	private String mParams = "";

	public UrlUpdater(Map map) {
		mMap = map;
	}

	public void setParams(String params) {
		mParams = params;
	}

	@Override
	public void run() {
		mMap.viewport().getMapPosition(pos);
		int lat = (int) (MercatorProjection.toLatitude(pos.y) * 1000);
		int lon = (int) (MercatorProjection.toLongitude(pos.x) * 1000);
		int rot = (int) (pos.bearing);
		rot = (int) (pos.bearing) % 360;
		//rot = rot < 0 ? -rot : rot;

		if (curZoom != pos.zoomLevel || curLat != lat || curLon != lon
		        || curTilt != rot || curRot != (int) (pos.bearing)) {

			curLat = lat;
			curLon = lon;
			curZoom = pos.zoomLevel;
			curTilt = (int) pos.tilt;
			curRot = rot;

			String newURL = Window.Location
			    .createUrlBuilder()
			    .setHash(mParams + "scale=" + pos.zoomLevel + ",rot=" + curRot
			            + ",tilt=" + curTilt + ",lat=" + (curLat / 1000f)
			            + ",lon=" + (curLon / 1000f))
			    .buildString();
			Window.Location.replace(newURL);
		}
	}
}
