package org.oscim.renderer.test;

import java.util.List;

import org.oscim.backend.canvas.Color;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.theme.styles.Line;
import org.oscim.utils.geom.BezierPath;

public class BezierPathLayer extends ElementRenderer {

	public BezierPathLayer() {
		mMapPosition.scale = 0;

		GeometryBuffer g = new GeometryBuffer(100, 1);
		g.startLine();

		Point[] pts = new Point[10];
		for (int i = 0; i < 10; i++) {
			pts[i] = new Point(i * 3, (i * i) % 3 * 4);
			pts[i].x *= 10;
			pts[i].y *= 10;
			// System.out.println(pts[i]);
			g.addPoint(pts[i]);
		}
		LineLayer ll = layers.addLineLayer(0, new Line(Color.BLUE, 2f));
		ll.addLine(g);

		List<Point> ctrl = BezierPath.cubicSplineControlPoints(pts, 0.1f);

		g.clear();
		g.startLine();
		Point p0 = pts[0];

		for (int j = 1, k = 0; j < pts.length; j++) {
			Point p1 = ctrl.get(k++);
			Point p2 = ctrl.get(k++);
			Point p3 = pts[j];
			System.out.println(">>> " + p1 + " " + p2);
			for (int i = 0; i < 10; i++) {
				double mu = (i / 10f);
				Point p = BezierPath.cubicBezier(p0, p1, p2, p3, mu);
				g.addPoint(p);
				System.out.println(mu + " " + p);
			}
			p0 = p3;
		}
		ll = layers.addLineLayer(1, new Line(Color.CYAN, 2f));
		ll.addLine(g);

	}

	public synchronized void clear() {
		layers.clear();
		setReady(false);
	}

	@Override
	protected synchronized void update(MapPosition pos, boolean changed, Matrices m) {

		if (mMapPosition.scale == 0)
			mMapPosition.copy(pos);

		if (!isReady()) {
			compile();
		}
	}
}
