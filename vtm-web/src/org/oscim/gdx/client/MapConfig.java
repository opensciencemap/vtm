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
 */
package org.oscim.gdx.client;

import com.google.gwt.core.client.JavaScriptObject;

public class MapConfig extends JavaScriptObject {
    protected MapConfig() {
    }

    public static native MapConfig get()/*-{
        return $wnd.mapconfig;
    }-*/;

    public final native double getLatitude() /*-{
        return this.latitude || 0;
    }-*/;

    public final native double getLongitude() /*-{
        return this.longitude || 0;
    }-*/;

    public final native int getZoom() /*-{
        return this.zoom || 2;
    }-*/;

    public final native String getTileSource() /*-{
        return this.tilesource;
    }-*/;

    public final native int getTileSize() /*-{
        return this.tileSize || 256;
    }-*/;

}
