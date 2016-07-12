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
package org.oscim.tiling.source.geojson;

import java.util.HashMap;
import java.util.Map;

public class Feature extends GeoJsonObject {

    protected Feature() {

    }

    public final native Geometry<?> getGeometry() /*-{
        return this.geometry;
    }-*/;

    public final native String getId() /*-{
        return this.id;
    }-*/;

    public final native void setId(String id) /*-{
        this.id = id;
    }-*/;

    public final Map<String, Object> getProperties(HashMap<String, Object> map) {
        map.clear();
        fromJavascriptObject(map);

        return map;
    }

    public final native void fromJavascriptObject(HashMap<String, Object> s) /*-{
        for(var key in this.properties) {
             s.@java.util.HashMap::put(Ljava/lang/Object;Ljava/lang/Object;)(key, Object(this.properties[key]));
        }
    }-*/;
}
