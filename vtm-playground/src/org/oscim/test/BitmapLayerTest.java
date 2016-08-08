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

import com.badlogic.gdx.Input;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.renderer.MapRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.oscim.tiling.source.bitmap.DefaultSources.HIKEBIKE_HILLSHADE;
import static org.oscim.tiling.source.bitmap.DefaultSources.OPENSTREETMAP;
import static org.oscim.tiling.source.bitmap.DefaultSources.STAMEN_TONER;
import static org.oscim.tiling.source.bitmap.DefaultSources.STAMEN_WATERCOLOR;

public class BitmapLayerTest extends GdxMap {

    final Logger log = LoggerFactory.getLogger(BitmapTileLayer.class);

    BitmapTileLayer mLayer = null;
    BitmapTileLayer mShaded = null;

    @Override
    protected boolean onKeyDown(int keycode) {
        if (keycode == Input.Keys.NUM_1) {
            mMap.layers().remove(mLayer);
            mLayer = new BitmapTileLayer(mMap, OPENSTREETMAP.build());
            mMap.layers().add(mLayer);
            return true;
        } else if (keycode == Input.Keys.NUM_2) {
            mMap.layers().remove(mLayer);
            mLayer = new BitmapTileLayer(mMap, STAMEN_WATERCOLOR.build());
            mMap.layers().add(mLayer);
            return true;
        } else if (keycode == Input.Keys.NUM_3) {
            if (mShaded != null) {
                mMap.layers().remove(mShaded);
                mShaded = null;
            } else {
                mShaded = new BitmapTileLayer(mMap, HIKEBIKE_HILLSHADE.build());
                mMap.layers().add(mShaded);
            }
            return true;
        }

        return false;
    }

    @Override
    public void createLayers() {
        MapRenderer.setBackgroundColor(0xff888888);

        mLayer = new BitmapTileLayer(mMap, STAMEN_TONER.build());
        mMap.layers().add(mLayer);

    }

    public static void main(String[] args) {

        GdxMapApp.init();
        GdxMapApp.run(new BitmapLayerTest(), null, 256);
    }
}
