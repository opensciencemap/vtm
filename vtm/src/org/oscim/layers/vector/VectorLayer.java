package org.oscim.layers.vector;

import static org.oscim.core.MercatorProjection.latitudeToY;
import static org.oscim.core.MercatorProjection.longitudeToX;

import java.util.ArrayList;
import java.util.List;

import org.oscim.backend.canvas.Color;
import org.oscim.core.Box;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.layers.vector.geometries.Drawable;
import org.oscim.layers.vector.geometries.LineDrawable;
import org.oscim.layers.vector.geometries.PointDrawable;
import org.oscim.layers.vector.geometries.Style;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.oscim.utils.FastMath;
import org.oscim.utils.QuadTree;
import org.oscim.utils.SpatialIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

/* TODO keep bounding box of geometries - only try to render when bbox intersects viewport */

/**
 * Use this layer to draw predefined geometries from layers.vector.geometries
 * package and
 * JTS geometries together with a GeometryStyle
 * 
 */
public class VectorLayer extends AbstractVectorLayer<Drawable> {

	public static final Logger log = LoggerFactory.getLogger(VectorLayer.class);

	//private final SpatialIndex<Drawable> mDrawables = new RTree<Drawable>();
	protected final SpatialIndex<Drawable> mDrawables = new QuadTree<Drawable>(1 << 30, 18);

	protected final List<Drawable> tmpDrawables = new ArrayList<Drawable>(128);

	protected final JtsConverter mConverter;
	protected double mMinX;
	protected double mMinY;

	private static class GeometryWithStyle implements Drawable {
		final Geometry geometry;
		final Style style;

		GeometryWithStyle(Geometry g, Style s) {
			geometry = g;
			style = s;
		}

		@Override
		public Style getStyle() {
			return style;
		}

		@Override
		public Geometry getGeometry() {
			return geometry;
		}
	}

	protected Polygon mEnvelope;

	public VectorLayer(Map map, SpatialIndex<Drawable> index) {
		this(map);
	}

	public VectorLayer(Map map) {
		super(map);
		mConverter = new JtsConverter(Tile.SIZE / UNSCALE_COORD);
	}

	private static Box bbox(Geometry geometry, Style style) {
		Envelope e = geometry.getEnvelopeInternal();
		Box bbox = new Box(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		//if ("Point".equals(geometry.getGeometryType())){
		//	bbox.
		//}

		bbox.scale(1E6);
		return bbox;
	}

	/**
	 * Adds a drawable to a list of geometries that have to be drawn in the next
	 * map update.
	 * 
	 * @param drawable
	 */
	public void add(Drawable drawable) {
		mDrawables.insert(bbox(drawable.getGeometry(), drawable.getStyle()), drawable);
	}

	/**
	 * Adds a JTS geometry and a style to a list of geometries that have to be
	 * drawn in the next map update.
	 * 
	 * @param geometry
	 * @param style
	 */
	public synchronized void add(Geometry geometry, Style style) {
		mDrawables.insert(bbox(geometry, style), new GeometryWithStyle(geometry, style));
	}

	/**
	 * Removes the drawable from the list of drawn geometries.
	 * 
	 * @param drawable
	 */
	public synchronized void remove(Drawable drawable) {
		mDrawables.remove(bbox(drawable.getGeometry(), drawable.getStyle()), drawable);
	}

	/**
	 * removes the JTS geometry and its style from the list of drawn geometries.
	 * 
	 * @param geometry
	 */
	public synchronized void remove(Geometry geometry) {
		Drawable toRemove = null;
		Box bbox = bbox(geometry, null);

		synchronized (this) {
			tmpDrawables.clear();
			mDrawables.search(bbox, tmpDrawables);
			for (Drawable d : tmpDrawables) {
				if (d.getGeometry() == geometry)
					toRemove = d;
			}
		}

		if (toRemove == null) {
			log.error("Can't find geometry to remove.");
			return;
		}

		mDrawables.remove(bbox, toRemove);
		//mMap.render();
	}

	@Override
	protected void processFeatures(Task t, Box bbox) {
		//log.debug("bbox {}", bbox);
		if (Double.isNaN(bbox.xmin))
			return;

		//	mEnvelope = new GeomBuilder()
		//	    .point(bbox.xmin, bbox.ymin)
		//	    .point(bbox.xmin, bbox.ymax)
		//	    .point(bbox.xmax, bbox.ymax)
		//	    .point(bbox.xmax, bbox.ymin)
		//	    .point(bbox.xmin, bbox.ymin)
		//	    .toPolygon();

		/* reduce lines points min distance */
		mMinX = ((bbox.xmax - bbox.xmin) / mMap.getWidth());
		mMinY = ((bbox.ymax - bbox.ymin) / mMap.getHeight());

		mConverter.setPosition(t.position.x, t.position.y, t.position.scale);

		bbox.scale(1E6);

		int level = 0;
		Style lastStyle = null;

		/* go through features, find the matching style and draw */
		synchronized (this) {
			tmpDrawables.clear();
			mDrawables.search(bbox, tmpDrawables);
			// TODO sort by some order...

			for (Drawable d : tmpDrawables) {
				Style style = d.getStyle();
				draw(t, level, d, style);

				if (style != lastStyle)
					level += 2;

				lastStyle = style;
			}
		}
	}

	protected void draw(Task task, int level, Drawable d, Style style) {
		Geometry geom = d.getGeometry();

		if (d instanceof LineDrawable) {
			drawLine(task, level, geom, style);
		} else if (d instanceof PointDrawable) {
			drawPoint(task, level, geom, style);
		} else {
			drawPolygon(task, level, geom, style);
		}
	}

	protected void drawPoint(Task t, int level, Geometry points, Style style) {

		MeshBucket mesh = t.buckets.getMeshBucket(level);
		if (mesh.area == null) {
			mesh.area = new AreaStyle(Color.fade(style.fillColor,
			                                     style.fillAlpha));
		}

		LineBucket ll = t.buckets.getLineBucket(level + 1);
		if (ll.line == null) {
			ll.line = new LineStyle(2, style.strokeColor, style.strokeWidth);
		}

		for (int i = 0; i < points.getNumGeometries(); i++) {
			Point p = (Point) points.getGeometryN(i);
			addCircle(mGeom.clear(), t.position, p.getX(), p.getY(), style);

			if (!mClipper.clip(mGeom))
				continue;

			mesh.addConvexMesh(mGeom);
			ll.addLine(mGeom);
		}
	}

	protected void drawLine(Task t, int level, Geometry line, Style style) {

		LineBucket ll = t.buckets.getLineBucket(level);
		if (ll.line == null) {
			ll.line = new LineStyle(0, style.strokeColor, style.strokeWidth);
		}

		if (style.generalization != Style.GENERALIZATION_NONE) {
			line = DouglasPeuckerSimplifier.simplify(line, mMinX * style.generalization);
		}

		//line = line.intersection(mEnvelope);

		for (int i = 0; i < line.getNumGeometries(); i++) {
			mConverter.transformLineString(mGeom.clear(), (LineString) line.getGeometryN(i));
			if (!mClipper.clip(mGeom))
				continue;

			ll.addLine(mGeom);
		}
	}

	protected void drawPolygon(Task t, int level, Geometry polygon, Style style) {

		MeshBucket mesh = t.buckets.getMeshBucket(level);
		if (mesh.area == null) {
			mesh.area = new AreaStyle(Color.fade(style.fillColor,
			                                     style.fillAlpha));
		}

		LineBucket ll = t.buckets.getLineBucket(level + 1);
		if (ll.line == null) {
			ll.line = new LineStyle(2, style.strokeColor, style.strokeWidth);
		}

		if (style.generalization != Style.GENERALIZATION_NONE) {
			polygon = DouglasPeuckerSimplifier.simplify(polygon, mMinX * style.generalization);
		}

		// if (polygon.isRectangle())

		for (int i = 0; i < polygon.getNumGeometries(); i++) {
			mConverter.transformPolygon(mGeom.clear(), (Polygon) polygon.getGeometryN(i));

			if (mGeom.getNumPoints() < 3)
				continue;

			if (!mClipper.clip(mGeom))
				continue;

			mesh.addMesh(mGeom);
			ll.addLine(mGeom);
		}
	}

	protected void addCircle(GeometryBuffer g, MapPosition pos,
	        double px, double py, Style style) {

		double scale = pos.scale * Tile.SIZE / UNSCALE_COORD;
		double x = (longitudeToX(px) - pos.x) * scale;
		double y = (latitudeToY(py) - pos.y) * scale;

		/* TODO in the next line I was only able to interpolate a function
		 * that makes up for the zoom level. The circle should not grow, it
		 * should stickto the map. 0.01 / (1 << startLvl) makes it retain
		 * its size. Correction? */
		int zoomScale = (1 << style.scalingZoomLevel);

		/* Keep the circle's size constant in relation to the underlying map */
		double radius = style.buffer;

		if (pos.scale > zoomScale)
			radius = (radius * 0.01) / zoomScale * (scale - zoomScale);

		int quality = (int) (Math.sqrt(radius) * 8);
		quality = FastMath.clamp(quality, 4, 32);

		double step = 2.0 * Math.PI / quality;

		g.startPolygon();
		for (int i = 0; i < quality; i++) {
			g.addPoint((float) (x + radius * Math.cos(i * step)),
			           (float) (y + radius * Math.sin(i * step)));
		}
	}
}
