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

import com.google.gwt.core.client.JavaScriptObject;

public abstract class GeoJsonObject extends JavaScriptObject {

    protected GeoJsonObject() {

    }

    public final native double[] getBbox()/*-{
        return bbox;
    }-*/;

    public final native void setBbox(double[] bbox) /*-{
        this.bbox = bbox;
    }-*/;
}
