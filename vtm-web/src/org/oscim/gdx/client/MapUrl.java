/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2018 devemux86
 * Copyright 2018 Izumi Kawashima
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
package org.oscim.gdx.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.map.Map;

import java.util.HashMap;

public class MapUrl extends Timer {
    private int curLon, curLat, curZoom, curTilt, curRot, curRoll;
    private MapPosition pos = new MapPosition();
    private final Map mMap;
    private String mParams = "";

    public MapUrl(Map map) {
        mMap = map;
    }

    public String getParam(String name) {
        return params.get(name);
    }

    public final HashMap<String, String> params = new HashMap<String, String>();

    public void parseUrl(MapPosition pos) {

        //String addOpts = "";
        if (Window.Location.getHash() == null)
            return;

        String hash = Window.Location.getHash();
        hash = hash.substring(1);
        String[] urlParams = null;
        urlParams = hash.split("&");
        if (urlParams.length == 1)
            urlParams = hash.split(",");
        double lat = pos.getLatitude(), lon = pos.getLongitude();
        float rotation = pos.bearing;
        float tilt = pos.tilt;
        float roll = pos.roll;

        //String themeName = "";
        //String mapName = "";

        int zoom = pos.zoomLevel;

        for (String p : urlParams) {
            try {
                if (p.startsWith("lat="))
                    lat = Double.parseDouble(p.substring(4));

                else if (p.startsWith("lon="))
                    lon = Double.parseDouble(p.substring(4));
                else if (p.startsWith("scale="))
                    zoom = Integer.parseInt(p.substring(6));
                else if (p.startsWith("rot="))
                    rotation = Float.parseFloat(p.substring(4));
                else if (p.startsWith("tilt="))
                    tilt = Float.parseFloat(p.substring(5));
                else if (p.startsWith("roll="))
                    roll = Float.parseFloat(p.substring(5));
                    //    else if (p.startsWith("theme="))
                    //        themeName = p.substring(6);
                    //    else if (p.startsWith("map="))
                    //        mapName = p.substring(4);
                else {
                    String[] opt = p.split("=");
                    if (opt.length > 1)
                        params.put(opt[0], opt[1]);
                    else
                        params.put(opt[0], null);

                    mParams += p + "&";

                }
            } catch (NumberFormatException e) {

            }
        }
        pos.setPosition(lat, lon);
        pos.setZoomLevel(zoom);
        pos.set(MercatorProjection.longitudeToX(lon),
                MercatorProjection.latitudeToY(lat),
                1 << zoom,
                rotation,
                tilt, roll);

    }

    @Override
    public void run() {
        mMap.viewport().getMapPosition(pos);
        int lat = (int) (pos.getLatitude() * 1000);
        int lon = (int) (pos.getLongitude() * 1000);
        int tilt = (int) (pos.tilt);
        int rot = (int) (pos.bearing);
        int roll = (int) (pos.roll);

        if (curZoom != pos.zoomLevel || curLat != lat || curLon != lon
                || curTilt != tilt || curRot != rot || curRoll != roll) {
            curLat = lat;
            curLon = lon;
            curZoom = pos.zoomLevel;
            curTilt = tilt;
            curRot = rot;
            curRoll = roll;

            String newURL = Window.Location
                    .createUrlBuilder()
                    .setHash(mParams
                            + "scale=" + pos.zoomLevel
                            + "&rot=" + curRot
                            + "&tilt=" + curTilt
                            + "&roll=" + curRoll
                            + "&lat=" + (curLat / 1000f)
                            + "&lon=" + (curLon / 1000f))
                    .buildString();
            Window.Location.replace(newURL);
        }
    }
}
