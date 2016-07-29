/*
 * Copyright 2014 Hannes Janetzek
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

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.PathLayer;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.renderer.bucket.TextureItem;
import org.oscim.theme.styles.LineStyle;
import org.oscim.tiling.source.bitmap.DefaultSources;

import java.util.ArrayList;
import java.util.List;

public class LineTexTest extends GdxMap {

    TextureItem tex;

    @Override
    protected void createLayers() {
        BitmapTileLayer bitmapLayer = new BitmapTileLayer(mMap, DefaultSources.STAMEN_TONER.build());
        bitmapLayer.tileRenderer().setBitmapAlpha(0.5f);
        mMap.setBaseMap(bitmapLayer);

        mMap.setMapPosition(0, 0, 1 << 2);

        tex = new TextureItem(CanvasAdapter.getBitmapAsset("", "patterns/pike.png"));
        tex.mipmap = true;

        //    LineStyle style = LineStyle.builder()
        //        .stippleColor(Color.BLACK)
        //        .stipple(64)
        //        .stippleWidth(1)
        //        .strokeWidth(8)
        //        .strokeColor(Color.RED)
        //        .fixed(true)
        //        .texture(tex)
        //        .build();

        //PathLayer pl = new PathLayer(mMap, style);
        //PathLayer pl = new PathLayer(mMap, Color.RED);

        //pl.addGreatCircle(new GeoPoint(53.1, -85.), new GeoPoint(-40.0, 85.0));

        //mMap.layers().add(pl);

        createLayers(1, true);

        /*mMap.events.bind(new Map.UpdateListener() {
            @Override
            public void onMapEvent(Event e, MapPosition mapPosition) {
                //if (e == Map.UPDATE_EVENT) {
                long t = System.currentTimeMillis();
                float pos = t % 20000 / 10000f - 1f;
                createLayers(pos, false);
                mMap.updateMap(true);
                //}
            }
        });*/
    }

    ArrayList<PathLayer> mPathLayers = new ArrayList<>();

    void createLayers(float pos, boolean init) {

        int i = 0;

        for (double lat = -90; lat <= 90; lat += 5) {
            List<GeoPoint> pts = new ArrayList<>();

            for (double lon = -180; lon <= 180; lon += 2) {
                //pts.add(new GeoPoint(lat, lon));
                double longitude = lon + (pos * 180);
                if (longitude < -180)
                    longitude += 360;
                if (longitude > 180)
                    longitude -= 360;

                double latitude = lat + (pos * 90);
                if (latitude < -90)
                    latitude += 180;
                if (latitude > 90)
                    latitude -= 180;

                latitude += Math.sin((Math.abs(pos) * (lon / Math.PI)));

                pts.add(new GeoPoint(latitude, longitude));
            }
            PathLayer pathLayer;
            if (init) {
                int c = Color.fade(Color.rainbow((float) (lat + 90) / 180), 0.9f);

                LineStyle style = LineStyle.builder()
                        .stippleColor(c)
                        .stipple(24)
                        .stippleWidth(1)
                        .strokeWidth(12)
                        .strokeColor(c)
                        .fixed(true)
                        .texture(tex)
                        .build();

                pathLayer = new PathLayer(mMap, style);

                mMap.layers().add(pathLayer);
                mPathLayers.add(pathLayer);
            } else {
                pathLayer = mPathLayers.get(i++);
            }

            pathLayer.setPoints(pts);
        }

    }

    //    mMap.layers().add(new GenericLayer(mMap, new BucketRenderer() {
    //        boolean init;
    //
    //        GeometryBuffer g = new GeometryBuffer(10, 1);
    //
    //        LineBucket lb = buckets.addLineBucket(0,
    //                                              new LineStyle(Color.fade(Color.CYAN, 0.5f), 2.5f));
    //
    //        LineTexBucket ll;
    //
    //        @Override
    //        public boolean setup() {
    //
    //            //lb.next = ll;
    //            ll = buckets.getLineTexBucket(1);
    //
    //            TextureItem tex = new TextureItem(CanvasAdapter.getBitmapAsset("patterns/dot.png"));
    //            tex.mipmap = true;
    //
    //            ll.line = LineStyle.builder()
    //                .stippleColor(Color.BLACK)
    //                .stipple(16)
    //                .stippleWidth(1)
    //                .strokeWidth(8)
    //                .strokeColor(Color.RED)
    //                .fixed(true)
    //                .texture(tex)
    //                .build();
    //
    //            //ll.width = 8;
    //
    //            return super.setup();
    //        }
    //
    //        @Override
    //        public void update(GLViewport v) {
    //            if (!init) {
    //                mMapPosition.copy(v.pos);
    //                init = true;
    //            }
    //
    //            buckets.clear();
    //            buckets.set(lb);
    //GeometryBuffer.makeCircle(g, 0, 0, 600, 40);
    //
    //            //    g.clear();
    //            //    g.startLine();
    //            //    g.addPoint(-1, 0);
    //            //    g.addPoint(1, 0);
    //            //    g.addPoint(1, -1);
    //            //    g.addPoint(-1, -1);
    //            //    g.scale(100, 100);
    //
    //            for (int i = 0; i < 15; i++) {
    //                lb.addLine(g);
    //                ll.addLine(g);
    //                g.scale(0.8f, 0.8f);
    //            }
    //            compile();
    //
    //        }
    //    }));

    public static void main(String[] args) {
        GdxMapApp.init();
        GdxMapApp.run(new LineTexTest(), null, 256);
    }
}
