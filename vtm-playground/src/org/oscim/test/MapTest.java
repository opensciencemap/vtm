/*
 * Copyright 2016 devemux86
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
package org.oscim.test;

import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Map;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;

public class MapTest extends GdxMapApp {

    @Override
    public void createLayers() {
        Map map = getMap();

        VectorTileLayer l = map.setBaseMap(new OSciMap4TileSource());

        GroupLayer groupLayer = new GroupLayer(mMap);
        groupLayer.layers.add(new BuildingLayer(map, l));
        groupLayer.layers.add(new LabelLayer(map, l));
        map.layers().add(groupLayer);

        map.setTheme(VtmThemes.DEFAULT);
        map.setMapPosition(53.075, 8.808, 1 << 17);
    }

    public static void main(String[] args) {
        GdxMapApp.init();
        GdxMapApp.run(new MapTest(), null, 400);
    }
}
