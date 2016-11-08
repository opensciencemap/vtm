/*
 * Copyright 2016 devemux86
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

import com.badlogic.gdx.Input;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.StreamRenderTheme;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MapInfo;

import java.io.File;
import java.util.Set;

public class MapsforgeStyleTest extends GdxMap {

    private static File mapFile;
    private String style;

    @Override
    public void createLayers() {
        MapFileTileSource tileSource = new MapFileTileSource();
        tileSource.setMapFile(mapFile.getAbsolutePath());
        tileSource.setPreferredLanguage("en");

        VectorTileLayer l = mMap.setBaseMap(tileSource);
        loadTheme();

        mMap.layers().add(new BuildingLayer(mMap, l));
        mMap.layers().add(new LabelLayer(mMap, l));

        MapInfo info = tileSource.getMapInfo();
        MapPosition pos = new MapPosition();
        pos.setByBoundingBox(info.boundingBox, Tile.SIZE * 4, Tile.SIZE * 4);
        mMap.setMapPosition(pos);
    }

    private static File getMapFile(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("missing argument: <mapFile>");
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            throw new IllegalArgumentException("file does not exist: " + file);
        } else if (!file.isFile()) {
            throw new IllegalArgumentException("not a file: " + file);
        } else if (!file.canRead()) {
            throw new IllegalArgumentException("cannot read file: " + file);
        }
        return file;
    }

    private void loadTheme() {
        mMap.setTheme(new StreamRenderTheme("", getClass().getResourceAsStream("/assets/styles/style.xml"), new XmlRenderThemeMenuCallback() {
            @Override
            public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
                if (style == null)
                    style = renderThemeStyleMenu.getDefaultValue();
                XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
                if (renderThemeStyleLayer == null) {
                    System.err.println("Invalid style " + style);
                    return null;
                }
                Set<String> categories = renderThemeStyleLayer.getCategories();
                // Add the categories from overlays that are enabled
                for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                    if (overlay.isEnabled())
                        categories.addAll(overlay.getCategories());
                }
                return categories;
            }
        }));
    }

    @Override
    protected boolean onKeyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.NUM_1:
                System.out.println("Sea with land");
                style = "1";
                loadTheme();
                mMap.clearMap();
                return true;
            case Input.Keys.NUM_2:
                System.out.println("Sea without land");
                style = "2";
                loadTheme();
                mMap.clearMap();
                return true;
        }

        return super.onKeyDown(keycode);
    }

    public static void main(String[] args) {
        mapFile = getMapFile(args);

        GdxMapApp.init();
        GdxMapApp.run(new MapsforgeStyleTest());
    }
}
