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
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.LineTexLayer;
import org.oscim.theme.styles.LineStyle;

import com.badlogic.gdx.Input;

public class LineRenderTest extends GdxMap {

	GeometryBuffer mGeom = new GeometryBuffer(2, 1);
	GeometryBuffer mLine = new GeometryBuffer(2, 1);

	static boolean fixedLineWidth = true;
	LineTest l = new LineTest();

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff000000);

		// TileSource ts = new OSciMap4TileSource();
		// ts.setOption("url", "http://opensciencemap.org/tiles/vtm");
		// initDefaultLayers(ts, false, false, false);

		mMap.setMapPosition(0, 0, 1 << 4);

		GeometryBuffer g = mLine;
		g.startLine();
		g.addPoint(-100, 0);
		g.addPoint(100, 0);

		addLines(l, 0, true, false);

		mMap.layers().add(new GenericLayer(mMap, l));
	}

	void addLines(LineTest l, int layer, boolean addOutline, boolean fixed) {

		GeometryBuffer g = mLine;

		LineStyle line1, line2, line3, line4;

		if (fixed) {
			line1 = new LineStyle(Color.RED, 0.5f);
			line2 = new LineStyle(Color.GREEN, 1);
			line3 = new LineStyle(Color.BLUE, 2);
			line4 = new LineStyle(Color.LTGRAY, 3);

		} else {
			line1 = new LineStyle(0, null, Color.fade(Color.RED, 0.5f), 4.0f,
			                      Cap.BUTT, false, 0, 0, 0, 0, 1f, false);

			line2 = new LineStyle(0, null, Color.GREEN, 6.0f, Cap.BUTT, true, 0, 0,
			                      0, 0, 1f, false);

			line4 = new LineStyle(0, null, Color.LTGRAY, 2.0f, Cap.ROUND, false, 0,
			                      0, 0, 0, 1f, false);
		}

		line3 = new LineStyle(0, null, Color.BLUE, 2.0f, Cap.ROUND, true, 4,
		                      Color.CYAN, 1, 0, 0, false);

		LineStyle outline = new LineStyle(0, null, Color.BLUE, 2.0f, Cap.ROUND, false, 0,
		                                  0, 0, 0, 1f, true);

		LineStyle outline2 = new LineStyle(0, null, Color.RED, 2.0f, Cap.ROUND, false, 0,
		                                   0, 0, 0, 0, true);

		LineLayer ol = l.layers.addLineLayer(0, outline);
		LineLayer ol2 = l.layers.addLineLayer(5, outline2);

		LineLayer ll = l.layers.addLineLayer(10, line1);
		ll.addLine(g.translate(0, -20));
		ll.addLine(g.translate(0, 10.5f));
		addCircle(-200, -200, 100, ll);

		if (addOutline)
			ol.addOutline(ll);

		ll = l.layers.addLineLayer(20, line2);
		ll.addLine(g.translate(0, 10.5f));
		ll.addLine(g.translate(0, 10.5f));
		addCircle(200, -200, 100, ll);

		if (addOutline)
			ol.addOutline(ll);

		LineTexLayer lt = l.layers.getLineTexLayer(30);
		lt.line = line3;
		lt.addLine(g.translate(0, 10.5f));
		lt.addLine(g.translate(0, 10.5f));
		addCircle(200, 200, 100, lt);

		// if (addOutline)
		// ol2.addOutline(ll);

		ll = l.layers.addLineLayer(40, line4);
		ll.addLine(g.translate(0, 10.5f));
		ll.addLine(g.translate(0, 10.5f));
		addCircle(-200, 200, 100, ll);

		if (addOutline)
			ol2.addOutline(ll);
	}

	void addCircle(float cx, float cy, float radius, LineLayer ll) {
		GeometryBuffer g = mGeom;

		g.clear();
		g.startLine();
		g.addPoint(cx, cy);
		g.addPoint(cx, cy);

		for (int i = 0; i < 60; i++) {
			double d = Math.toRadians(i * 6);
			g.setPoint(1, cx + (float) Math.sin(d) * radius,
			           cy + (float) Math.cos(d) * radius);
			ll.addLine(g);
		}
	}

	void addCircle(float cx, float cy, float radius, LineTexLayer ll) {
		GeometryBuffer g = mGeom;

		g.clear();
		g.startLine();
		g.addPoint(cx, cy);
		g.addPoint(cx, cy);

		for (int i = 0; i < 60; i++) {
			double d = Math.toRadians(i * 6);
			g.setPoint(1, cx + (float) Math.sin(d) * radius,
			           cy + (float) Math.cos(d) * radius);
			ll.addLine(g);
		}
	}

	@Override
	protected boolean onKeyDown(int keycode) {
		if (keycode < Input.Keys.NUM_1 || keycode > Input.Keys.NUM_4)
			return false;

		synchronized (l) {
			l.clear();

			GeometryBuffer g = mLine;
			g.clear();
			g.startLine();
			g.addPoint(-100, 0);
			g.addPoint(100, 0);

			if (keycode == Input.Keys.NUM_1)
				addLines(l, 0, true, true);
			else if (keycode == Input.Keys.NUM_2)
				addLines(l, 0, true, false);
			else if (keycode == Input.Keys.NUM_3)
				addLines(l, 0, false, true);
			else if (keycode == Input.Keys.NUM_4)
				addLines(l, 0, false, false);
		}

		mMap.updateMap(true);

		return true;
	}

	class LineTest extends ElementRenderer {

		public LineTest() {
			mMapPosition.scale = 0;
		}

		public synchronized void clear() {
			layers.clear();
			setReady(false);
		}

		@Override
		protected synchronized void update(GLViewport v) {

			if (mMapPosition.scale == 0)
				mMapPosition.copy(v.pos);

			if (!isReady()) {
				compile();
			}
		}

		// @Override
		// protected void setMatrix(MapPosition pos, Matrices m, boolean
		// project) {
		// m.useScreenCoordinates(true, 8f);
		// }
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LineRenderTest(), null, 256);
	}
}
