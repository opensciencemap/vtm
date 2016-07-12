package org.oscim.gdx.client;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;

import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.map.Map;

import java.util.HashMap;

public class MapUrl extends Timer {
    private int curLon, curLat, curZoom, curTilt, curRot;
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
                tilt);

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
                    .setHash(mParams
                            + "scale=" + pos.zoomLevel
                            + "&rot=" + curRot
                            + "&tilt=" + curTilt
                            + "&lat=" + (curLat / 1000f)
                            + "&lon=" + (curLon / 1000f))
                    .buildString();
            Window.Location.replace(newURL);
        }
    }
}
