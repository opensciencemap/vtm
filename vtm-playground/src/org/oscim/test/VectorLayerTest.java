package org.oscim.test;

import org.oscim.backend.canvas.Color;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.vector.VectorLayer;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.utils.ColorUtil;

public class VectorLayerTest extends GdxMapApp {

	@Override
	public void createLayers() {
		Map map = getMap();

		//VectorTileLayer tileLayer = map.setBaseMap(new OSciMap4TileSource());

		VectorLayer vectorLayer = new VectorLayer(map);

		//		vectorLayer.add(new PointDrawable(0, 180, Style.builder()
		//		    .setBuffer(10)
		//		    .setFillColor(Color.RED)
		//		    .setFillAlpha(0.5)
		//		    .build()));
		//
		//		Geometry g = new GeomBuilder()
		//		    .point(180, 0)
		//		    .point()
		//		    .buffer(6)
		//		    .get();
		//
		//		vectorLayer.add(new PolygonDrawable(g, defaultStyle()));
		//

		Style.Builder sb = Style.builder()
		    .buffer(0.4)
		    .fillColor(Color.RED)
		    .fillAlpha(0.2);

		Style style = sb.fillAlpha(0.2).build();

		//		int tileSize = 5;
		//		for (int x = -180; x < 200; x += tileSize) {
		//			for (int y = -90; y < 90; y += tileSize) {
		//				//	Style style = sb.setFillAlpha(FastMath.clamp(FastMath.length(x, y) / 180, 0.2, 1))
		//				//		    .build();
		//
		//				vectorLayer.add(new RectangleDrawable(FastMath.clamp(y, -85, 85), x,
		//				                                      FastMath.clamp(y + tileSize - 0.1, -85, 85),
		//				                                      x + tileSize - 0.1, style));
		//
		//			}
		//		}

		for (int i = 0; i < 1000; i++) {
			style = sb.buffer(Math.random() * 1)
			    .fillColor(ColorUtil.setHue(Color.RED,
			                                   Math.random()))
			    .fillAlpha(0.5)
			    .build();

			vectorLayer.add(new PointDrawable(Math.random() * 180 - 90,
			                                  Math.random() * 360 - 180,
			                                  style));

		}

		map.layers().add(vectorLayer);

		//map.layers().add(new LabelLayer(map, tileLayer));
		//map.setTheme(VtmThemes.DEFAULT);

		map.setMapPosition(0, 0, 1 << 2);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new VectorLayerTest(), null, 400);
	}
}
