package org.oscim.layers;

// FIXME
// Apache License 2.0

import java.io.IOException;

import org.jeo.data.Dataset;
import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.CoordinatePath;
import org.jeo.geom.Envelopes;
import org.jeo.geom.Geom;
import org.jeo.map.CartoCSS;
import org.jeo.map.Map;
import org.jeo.map.RGB;
import org.jeo.map.Rule;
import org.jeo.map.RuleList;
import org.jeo.map.View;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.RenderElement;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.PausableThread;
import org.oscim.utils.TileClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Does the work of actually rendering the map, outside of the ui thread.
 * 
 * @author Justin Deoliveira, OpenGeo
 * @author Hannes Janetzek, OpenScienceMap
 */

public class JeoMapLoader extends PausableThread {

	static final Logger log = LoggerFactory.getLogger(JeoMapLoader.class);

	private final JeoMapLayer mMapLayer;

	public JeoMapLoader(JeoMapLayer mapLayer) {
		mMapLayer = mapLayer;
	}

	private ElementLayers layers;
	private final GeometryBuffer mGeom = new GeometryBuffer(128, 4);

	private Task mCurrentTask;

	private double mMinX;
	private double mMinY;

	@Override
	protected void doWork() throws InterruptedException {
		log.debug("start");
		mWork = false;
		Envelope env = new Envelope();
		BoundingBox bbox = mMapLayer.mMap.getViewport().getViewBox();

		env.init(bbox.getMinLongitude(), bbox.getMaxLongitude(),
		         bbox.getMinLatitude(), bbox.getMaxLatitude());
		int w = mMapLayer.mMap.getWidth();
		int h = mMapLayer.mMap.getHeight();
		mMapLayer.view.setWidth(w);
		mMapLayer.view.setHeight(h);

		mClipper.setRect(-w, -h, w, h);

		mMapLayer.view.zoomto(env);

		Task task = new Task();
		task.view = mMapLayer.view.clone();

		mMapLayer.mMap.getMapPosition(task);

		mCurrentTask = task;
		layers = new ElementLayers();

		Envelope b = task.view.getBounds();

		// reduce lines points min distance
		mMinX = ((b.getMaxX() - b.getMinX()) / task.view.getWidth()) * 2;
		mMinY = ((b.getMaxY() - b.getMinY()) / task.view.getHeight()) * 2;

		Map map = mMapLayer.view.getMap();

		for (org.jeo.map.Layer l : map.getLayers()) {

			if (!l.isVisible())
				continue;

			Dataset data = l.getData();

			RuleList rules =
			        map.getStyle().getRules().selectById(l.getName(), true).flatten();

			log.debug("data {}", data);

			if (data instanceof VectorDataset) {
				for (RuleList ruleList : rules.zgroup()) {
					render(task.view, (VectorDataset) data, ruleList);
				}
			}
		}

		if (layers.baseLayers != null) {
			mCurrentTask.layers = layers.baseLayers;

			//layers.baseLayers = null;
			//layers.clear();

			mMapLayer.setLayers(mCurrentTask);
		}
		layers = null;
		mCurrentTask = null;

	}

	void render(View view, VectorDataset data, RuleList rules) {

		try {
			Query q = new Query().bounds(view.getBounds());
			log.debug("query {}", q);

			// reproject
			// if (data.getCRS() != null) {
			//	if (!Proj.equal(view.getCRS(), data.getCRS())) {
			//		q.reproject(view.getCRS());
			//	}
			//}
			//else {
			//	log.debug("Layer " + data.getName()
			//		+ " specifies no projection, assuming map projection");
			//}

			for (Feature f : data.cursor(q)) {

				RuleList rs = rules.match(f);
				if (rs.isEmpty()) {
					continue;
				}

				Rule r = rules.match(f).collapse();
				if (r == null)
					continue;

				draw(view, f, r);
			}
		} catch (IOException e) {
			log.error("Error querying layer " + data.getName() + e);
		}
	}

	Geometry clipGeometry(View view, Geometry g) {
		// TODO: doing a full intersection is sub-optimal,
		// look at a more efficient clipping
		// algorithm, like cohen-sutherland
		return g.intersection(Envelopes.toPolygon(view.getBounds()));
	}

	void draw(View view, Feature f, Rule rule) {
		Geometry g = f.geometry();
		if (g == null) {
			return;
		}

		//		g = clipGeometry(view, g);
		//		if (g.isEmpty()) {
		//			return;
		//		}

		switch (Geom.Type.from(g)) {
			case POINT:
			case MULTIPOINT:
				//log.debug("draw point");
				//drawPoint(f, rule);
				return;
			case LINESTRING:
			case MULTILINESTRING:
				//log.debug("draw line");
				drawLine(f, rule, g);
				return;
			case POLYGON:
				//Polygon p = (Polygon) g;
				//p.reverse();
				//log.debug("draw polygon");
				drawPolygon(f, rule, g);
				return;

			case MULTIPOLYGON:
				//log.debug("draw polygon");
				for (int i = 0, n = g.getNumGeometries(); i < n; i++)
					drawPolygon(f, rule, g.getGeometryN(i));
				return;
			default:
				throw new UnsupportedOperationException();
		}
	}

	private void drawLine(Feature f, Rule rule, Geometry g) {

		LineLayer ll = layers.getLineLayer(0);

		if (ll.line == null) {
			RGB color = rule.color(f, CartoCSS.LINE_COLOR, RGB.black);
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			ll.line = new Line(0, color(color), width);
			ll.width = width;
		}

		mGeom.clear();
		mGeom.startLine();

		CoordinatePath p = CoordinatePath.create(g);
		path(mGeom, p);

		//log.debug( ll.width + " add line " + mGeom.pointPos + " " + Arrays.toString(mGeom.points));

		ll.addLine(mGeom);
	}

	TileClipper mClipper = new TileClipper(0, 0, 0, 0);

	private void drawPolygon(Feature f, Rule rule, Geometry g) {

		LineLayer ll = layers.getLineLayer(3);

		if (ll.line == null) {
			RGB color = rule.color(f, CartoCSS.POLYGON_FILL, RGB.red);
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			ll.line = new Line(2, color(color), width);
			ll.width = width;
		}

		//PolygonLayer pl = layers.getPolygonLayer(1);
		//
		//if (pl.area == null) {
		//	RGB color = rule.color(f, CartoCSS.POLYGON_FILL, RGB.red);
		//	pl.area = new Area(1, color(color));
		//}

		MeshLayer mesh = layers.getMeshLayer(2);

		mGeom.clear();
		mGeom.startPolygon();
		//mGeom.startLine();

		CoordinatePath p = CoordinatePath.create(g).generalize(mMinX, mMinY);
		if (path(mGeom, p) < 3)
			return;

		if (!mClipper.clip(mGeom))
			return;

		//log.debug(ll.width + " add poly " + mGeom.pointPos + " " + Arrays.toString(mGeom.points));
		mesh.addMesh(mGeom);

		ll.addLine(mGeom);
		//pl.addPolygon(mGeom.points, mGeom.index);
	}

	public static int color(RGB rgb) {
		return rgb.getAlpha() << 24
		        | rgb.getRed() << 16
		        | rgb.getGreen() << 8
		        | rgb.getBlue();
	}

	private int path(GeometryBuffer g, CoordinatePath path) {

		MapPosition pos = mCurrentTask;
		double scale = pos.scale * Tile.SIZE;
		int cnt = 0;
		O: while (path.hasNext()) {
			Coordinate c = path.next();
			float x = (float) ((MercatorProjection.longitudeToX(c.x) - pos.x) * scale);
			float y = (float) ((MercatorProjection.latitudeToY(c.y) - pos.y) * scale);

			switch (path.getStep()) {
				case MOVE_TO:
					if (g.isPoly())
						g.startPolygon();
					else if (g.isLine())
						g.startLine();

					cnt++;
					g.addPoint(x, y);
					break;

				case LINE_TO:
					cnt++;
					g.addPoint(x, y);
					break;

				case CLOSE:
					//g.addPoint(x, y);

					//if (g.type == GeometryType.POLY)
					break;
				case STOP:
					break O;
			}
		}
		return cnt;
	}

	@Override
	protected String getThreadName() {
		return "JeoMapLayer";
	}

	@Override
	protected boolean hasWork() {
		return mWork;
	}

	boolean mWork;

	public void go() {
		if (hasWork())
			return;

		mWork = true;

		synchronized (this) {
			notifyAll();
		}
	}

	static class Task extends MapPosition {
		View view;
		RenderElement layers;
	}
}
