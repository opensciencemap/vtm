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
package org.oscim.web.js;

import org.oscim.core.MapPosition;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.LayerRenderer;
import org.oscim.theme.IRenderTheme;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.source.geojson.OsmRoadLineJsonTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.timepedia.exporter.client.Export;
import org.timepedia.exporter.client.ExportOverlay;
import org.timepedia.exporter.client.ExportPackage;
import org.timepedia.exporter.client.Exportable;

@ExportPackage("")
public class JsOverlays implements Exportable {
    @ExportPackage("vtm")
    @Export("Layers")
    public interface XLayers extends ExportOverlay<Layers> {
        void add(Layer layer);
    }

    @ExportPackage("vtm")
    @Export("Map")
    public interface XMap extends ExportOverlay<Map> {
        public abstract Layers layers();

        public abstract void setMapPosition(MapPosition pos);

        public abstract MapPosition getMapPosition();

    }

    @ExportPackage("vtm")
    @Export("MapPosition")
    public static class XMapPosition implements ExportOverlay<MapPosition> {
        public XMapPosition(double latitude, double longitude, double scale) {
        }

        ;

        public XMapPosition() {
        }

        ;

        public void setPosition(double latitude, double longitude) {
        }

        public void setScale(double scale) {
        }
    }

    @ExportPackage("vtm")
    @Export("GenericLayer")
    public abstract class XGenericLayer implements ExportOverlay<GenericLayer> {
        public XGenericLayer(Map map, LayerRenderer renderer) {
        }
    }

    @ExportPackage("vtm")
    @Export("TileGridLayer")
    public static class XTileGridLayer implements ExportOverlay<TileGridLayer> {
        public XTileGridLayer(Map map) {
        }
    }

    @ExportPackage("vtm")
    @Export("OsmTileLayer")
    public static class XOsmTileLayer implements ExportOverlay<OsmTileLayer> {
        public XOsmTileLayer(Map map) {
        }

        public boolean setTileSource(TileSource tileSource) {
            return false;
        }

        public void setRenderTheme(IRenderTheme theme) {
        }
    }

    @ExportPackage("vtm")
    @Export("HighroadJsonTileSource")
    public static class XHighroadJsonTileSource implements
            ExportOverlay<OsmRoadLineJsonTileSource> {
        public XHighroadJsonTileSource() {
        }
    }

    @ExportPackage("vtm")
    @Export("OSciMap4TileSource")
    public static class XOSciMap4TileSource implements
            ExportOverlay<OSciMap4TileSource> {
        public XOSciMap4TileSource(String url) {
        }

        public XOSciMap4TileSource() {
        }
    }

    @ExportPackage("vtm")
    @Export("LabelLayer")
    public static class XLabelLayer implements
            ExportOverlay<LabelLayer> {
        public XLabelLayer(Map map, VectorTileLayer l) {
        }
    }

    @ExportPackage("vtm")
    @Export("BuildingLayer")
    public static class XBuildingLayer implements
            ExportOverlay<BuildingLayer> {
        public XBuildingLayer(Map map, VectorTileLayer l) {
        }
    }

    //    @ExportPackage("vtm")
    //    @Export("Viewport")
    //    public interface XViewport extends ExportOverlay
    //
}
