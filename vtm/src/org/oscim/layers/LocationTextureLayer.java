/*
 * Copyright 2017-2018 Longri
 * Copyright 2018 devemux86
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
package org.oscim.layers;

import org.oscim.core.MercatorProjection;
import org.oscim.map.Map;
import org.oscim.renderer.LocationTextureRenderer;
import org.oscim.renderer.atlas.TextureRegion;

public class LocationTextureLayer extends Layer {
    public final LocationTextureRenderer locationRenderer;

    public LocationTextureLayer(Map map, TextureRegion textureRegion) {
        super(map);

        mRenderer = locationRenderer = new LocationTextureRenderer(map);
        locationRenderer.setTextureRegion(textureRegion);
    }

    public void setPosition(double latitude, double longitude, float bearing, float accuracy) {
        double x = MercatorProjection.longitudeToX(longitude);
        double y = MercatorProjection.latitudeToY(latitude);
        bearing = -bearing;
        while (bearing < 0)
            bearing += 360;
        double radius = accuracy / MercatorProjection.groundResolutionWithScale(latitude, 1);
        locationRenderer.setLocation(x, y, bearing, radius);
    }
}
