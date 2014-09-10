package org.oscim.test;

import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.core.GeometryBuffer;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.GenericLayer;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.MapRenderer;
import org.oscim.renderer.elements.HairLineLayer;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.PolygonLayer;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.LineStyle.LineBuilder;

public class HairLineTest extends GdxMap {

	static GeometryBuffer createLine(float r) {
		GeometryBuffer in = new GeometryBuffer(100, 2);

		for (int j = 0; j <= 12; j += 3) {
			in.startLine();
			for (int i = 0; i <= 120; i++) {
				double rad = Math.toRadians(i * 3);
				in.addPoint((float) (Math.cos(rad) * (r + j)), (float) (Math.sin(rad) * (r + j)));
			}
		}
		return in;
	}

	static class Renderer extends ElementRenderer {
		boolean init;
		LineBuilder l = new LineStyle.LineBuilder()
		    .color(Color.WHITE)
		    .width(1.5f)
		    .cap(Cap.ROUND);

		HairLineLayer ll = layers.addHairLineLayer(1, l.build());

		//LineLayer ll = layers.addLineLayer(1, new LineStyle(Color.fade(Color.CYAN, 0.6f), 2.5f));
		LineStyle style = new LineStyle(Color.fade(Color.MAGENTA, 0.6f), 2.5f);

		HairLineLayer l1 = layers.addHairLineLayer(2, style);

		//style = new LineStyle(Color.fade(Color.LTGRAY, 0.8f), 1.5f);
		LineLayer l2 = layers.addLineLayer(3, style);

		PolygonLayer pl = layers.addPolygonLayer(4, new AreaStyle.AreaBuilder()
		    .color(Color.BLUE)
		    //.outline(Color.CYAN, 1)
		    .build());

		@Override
		protected boolean setup() {
			//ll.roundCap = true;
			return super.setup();
		}

		@Override
		protected void update(GLViewport v) {
			if (!init) {
				mMapPosition.copy(v.pos);
				init = true;
				GeometryBuffer g;

				for (int i = 105; i < 160; i += 30) {

					g = createLine(i);

					ll.addLine(g);

					//g.translate(10, 10);
					//l1.addLine(g);

					//		int o = 0;
					//		for (int k = 0; k < g.index.length && g.index[k] >= 0; k++) {
					//	
					//			for (int j = 0; j < g.index[k];)
					//				ll.addPoint(g.points[o + j++], g.points[o + j++]);
					//	
					//			o += g.index[k];
					//		}
				}
				g = new GeometryBuffer(4, 2);
				g.clear();
				g.startPolygon();
				g.addPoint(-100, -100);
				g.addPoint(100, -100);
				g.addPoint(100, 100);
				g.addPoint(-100, 100);
				g.translate(100, 100);
				l2.addLine(g);
				pl.addPolygon(g);

				compile();
			}
		}
	}

	@Override
	protected void createLayers() {
		MapRenderer.setBackgroundColor(Color.BLACK);
		mMap.layers().add(new GenericLayer(mMap, new Renderer()));

	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new HairLineTest(), null, 400);

	}
}
