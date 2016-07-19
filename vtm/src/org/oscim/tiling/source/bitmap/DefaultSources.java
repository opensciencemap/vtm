/*
 * Copyright 2016 Andrey Novikov
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
package org.oscim.tiling.source.bitmap;

import org.oscim.layers.tile.bitmap.BitmapTileLayer.FadeStep;
import org.oscim.tiling.source.bitmap.BitmapTileSource.Builder;

/**
 * Do not use in applications unless you read through and comply to
 * their terms of use! Only added here for testing puposes.
 */
public class DefaultSources {

    public static Builder<?> OPENSTREETMAP = BitmapTileSource.builder()
            .url("http://tile.openstreetmap.org")
            .zoomMax(18);

    public static Builder<?> OSM_TRANSPORT = BitmapTileSource.builder()
            .url("http://a.tile.thunderforest.com/transport")
            .zoomMax(18);

    public static Builder<?> STAMEN_TONER = BitmapTileSource.builder()
            .url("http://a.tile.stamen.com/toner")
            .zoomMax(18);

    public static Builder<?> STAMEN_WATERCOLOR = BitmapTileSource.builder()
            .url("http://a.tile.stamen.com/watercolor")
            .tilePath("/{Z}/{X}/{Y}.jpg")
            .zoomMax(18);

    public static Builder<?> IMAGICO_LANDCOVER = BitmapTileSource.builder()
            .url("http://www.imagico.de/map/tiles/landcover")
            .tilePath("/{Z}/{X}/{Y}.jpg")
            .zoomMax(6);

    final static FadeStep[] fadeSteps = new FadeStep[]{
            new FadeStep(0, 8 - 1, 1, 0.7f),
            // dont fade between zoom-min/max
            // fade above zoom max + 2, interpolate 1 to 0
            new FadeStep(8 - 1, 8 + 1, 0.7f, 0)
    };

    public static Builder<?> MAPQUEST_AERIAL = BitmapTileSource.builder()
            .url("http://otile1.mqcdn.com/tiles/1.0.0/sat")
            .tilePath("/{Z}/{X}/{Y}.jpg")
            .fadeSteps(fadeSteps)
            .zoomMax(8);

    public static Builder<?> NE_LANDCOVER = BitmapTileSource.builder()
            .url("http://opensciencemap.org/tiles/ne")
            .fadeSteps(fadeSteps)
            .zoomMax(8);

    public static Builder<?> ARCGIS_RELIEF = BitmapTileSource.builder()
            .url("http://server.arcgisonline.com/ArcGIS/rest/services" +
                    "/World_Shaded_Relief/MapServer/tile/")
            .tilePath("{Z}/{Y}/{X}")
            .zoomMax(13);

    public static Builder<?> HD_HILLSHADE = BitmapTileSource.builder()
            .url("http://korona.geog.uni-heidelberg.de/tiles/asterh/")
            .tilePath("?x={X}&y={Y}&z={Z}")
            .zoomMin(2)
            .zoomMax(16);

    public static Builder<?> HIKEBIKE = BitmapTileSource.builder()
            .url("http://tiles.wmflabs.org/hikebike")
            .tilePath("/{Z}/{X}/{Y}.png")
            .zoomMax(17);

    public static Builder<?> HIKEBIKE_HILLSHADE = BitmapTileSource.builder()
            .url("http://tiles.wmflabs.org/hillshading")
            .tilePath("/{Z}/{X}/{Y}.png")
            .zoomMax(14);

    /**
     * https://github.com/opensciencemap/vtm/issues/18
     * https://developers.google.com/maps/faq
     */

    public static Builder<?> GOOGLE_MAPS = BitmapTileSource.builder()
            .url("INSERT URL")
            .tilePath("/vt/x={X}&y={Y}&z={Z}&s=Galileo&scale=2")
            .zoomMin(1)
            .zoomMax(20);
}
