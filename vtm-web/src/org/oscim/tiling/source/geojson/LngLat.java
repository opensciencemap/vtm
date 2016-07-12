package org.oscim.tiling.source.geojson;

import com.google.gwt.core.client.JavaScriptObject;

public class LngLat extends JavaScriptObject {

    protected LngLat() {

    }

    public final native double getLongitude() /*-{
        return this[0];
    }-*/;

    public final native double getLatitude() /*-{
        return this[1];
    }-*/;
}
