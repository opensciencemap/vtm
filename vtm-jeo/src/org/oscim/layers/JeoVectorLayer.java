package org.oscim.layers;

import java.io.IOException;

import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.Geom;
import org.jeo.map.CartoCSS;
import org.jeo.map.RGB;
import org.jeo.map.Rule;
import org.jeo.map.RuleList;
import org.jeo.map.Style;
import org.oscim.jeo.JeoUtils;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.theme.styles.AreaStyle;
import org.oscim.theme.styles.LineStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class JeoVectorLayer extends JtsLayer {

	public static final Logger log = LoggerFactory.getLogger(JeoVectorLayer.class);
	static final boolean dbg = false;

	private final VectorDataset mDataset;
	private final RuleList mRules;

	protected double mDropPointDistance = 0.01;

	public JeoVectorLayer(Map map, VectorDataset data, Style style) {
		super(map);
		mDataset = data;

		mRules = style.getRules().selectById(data.getName(), true).flatten();
		//mRules = style.getRules().selectById("way", true).flatten();
		log.debug(mRules.toString());

		mRenderer = new Renderer();
	}

	@Override
	protected void processFeatures(Task t, Envelope b) {
		if (mDropPointDistance > 0) {
			/* reduce lines points min distance */
			mMinX = ((b.getMaxX() - b.getMinX()) / mMap.getWidth());
			mMinY = ((b.getMaxY() - b.getMinY()) / mMap.getHeight());
			mMinX *= mDropPointDistance;
			mMinY *= mDropPointDistance;
		}

		try {
			Query q = new Query().bounds(b);
			if (dbg)
				log.debug("query {}", b);
			for (Feature f : mDataset.cursor(q)) {
				if (dbg)
					log.debug("feature {}", f);

				RuleList rs = mRules.match(f);
				if (rs.isEmpty())
					continue;

				Rule r = rs.collapse();
				if (r == null)
					continue;

				Geometry g = f.geometry();
				if (g == null)
					continue;

				switch (Geom.Type.from(g)) {
					case POINT:
						addPoint(t, f, r, g);
						break;
					case MULTIPOINT:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addPoint(t, f, r, g.getGeometryN(i));
						break;
					case LINESTRING:
						addLine(t, f, r, g);
						break;
					case MULTILINESTRING:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addLine(t, f, r, g.getGeometryN(i));
						break;
					case POLYGON:
						addPolygon(t, f, r, g);
						break;
					case MULTIPOLYGON:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addPolygon(t, f, r, g.getGeometryN(i));
						break;
					default:
						break;
				}
			}
		} catch (IOException e) {
			log.error("Error querying layer " + mDataset.getName() + e);
		}
	}

	protected void addLine(Task t, Feature f, Rule rule, Geometry g) {

		if (((LineString) g).isClosed()) {
			addPolygon(t, f, rule, g);
			return;
		}

		LineBucket ll = t.buckets.getLineBucket(2);
		if (ll.line == null) {
			RGB color = rule.color(f, CartoCSS.LINE_COLOR, RGB.black);
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			ll.line = new LineStyle(0, JeoUtils.color(color), width);
			ll.setDropDistance(0.5f);
		}

		addLine(t, g, ll);
	}

	protected void addPolygon(Task t, Feature f, Rule rule, Geometry g) {

		LineBucket ll = t.buckets.getLineBucket(1);

		if (ll.line == null) {
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			RGB color = rule.color(f, CartoCSS.LINE_COLOR, RGB.black);
			ll.line = new LineStyle(0, JeoUtils.color(color), width);
			ll.setDropDistance(0.5f);
		}

		MeshBucket mesh = t.buckets.getMeshBucket(0);
		if (mesh.area == null) {
			int color = JeoUtils.color(rule.color(f, CartoCSS.POLYGON_FILL, RGB.red));
			mesh.area = new AreaStyle(color);
		}

		addPolygon(t, g, mesh, ll);
	}

	protected void addPoint(Task t, Feature f, Rule rule, Geometry g) {

	}
}
