package org.oscim.layers;

import java.util.HashMap;

import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.map.CartoCSS;
import org.jeo.map.RGB;
import org.jeo.map.Rule;
import org.jeo.map.Style;
import org.oscim.backend.canvas.Color;
import org.oscim.jeo.JeoUtils;
import org.oscim.map.Map;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.renderer.elements.TextItem;
import org.oscim.renderer.elements.TextLayer;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.theme.styles.Text;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class OSMIndoorLayer extends JeoVectorLayer {

	protected TextLayer mTextLayer;
	protected Text mText = Text.createText(16, 2.2f, Color.BLACK, Color.WHITE, true);

	public OSMIndoorLayer(Map map, VectorDataset data, Style style) {
		super(map, data, style);
	}

	public boolean[] activeLevels = new boolean[10];

	@Override
	protected void processFeatures(Task t, Envelope b) {
		mTextLayer = t.layers.addTextLayer(new TextLayer());

		super.processFeatures(t, b);

		//render TextItems to a bitmap and prepare vertex buffer data.
		mTextLayer.prepare();
		mTextLayer.clearLabels();
	}

	protected void addLine(Task t, Feature f, Rule rule, Geometry g) {

		if (((LineString) g).isClosed()) {
			addPolygon(t, f, rule, g);
			return;
		}

		int level = getLevel(f);

		LineLayer ll = t.layers.getLineLayer(level * 3 + 2);
		if (ll.line == null) {
			RGB color = rule.color(f, CartoCSS.LINE_COLOR, RGB.black);
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			ll.line = new Line(0, JeoUtils.color(color), width);
			ll.heightOffset = level * 4;
			ll.setDropDistance(0);
		}

		addLine(t, g, ll);
	}

	protected void addPolygon(Task t, Feature f, Rule rule, Geometry g) {
		int level = getLevel(f);

		LineLayer ll = t.layers.getLineLayer(level * 3 + 1);

		boolean active = activeLevels[level + 1];

		if (ll.line == null) {
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			int color = Color.rainbow((level + 1) / 10f);

			if (level > -2 && !active)
				color = Color.fade(color, 0.1f);

			ll.line = new Line(0, color, width);
			ll.heightOffset = level * 4;
			ll.setDropDistance(0);
		}

		MeshLayer mesh = t.layers.getMeshLayer(level * 3);
		if (mesh.area == null) {
			int color = JeoUtils.color(rule.color(f, CartoCSS.POLYGON_FILL, RGB.red));
			if (level > -2 && !active)
				color = Color.fade(color, 0.1f);

			mesh.area = new Area(color);
			//mesh.area = new Area(Color.fade(Color.DKGRAY, 0.1f));
			mesh.heightOffset = level * 4f;
		}

		addPolygon(t, g, mesh, ll);

		if (active) {
			Object o = f.get("name");
			if (o instanceof String) {
				float x = 0;
				float y = 0;
				int n = mGeom.index[0];
				for (int i = 0; i < n;) {
					x += mGeom.points[i++];
					y += mGeom.points[i++];
				}

				TextItem ti = TextItem.pool.get();
				ti.set(x / (n / 2) / 8, y / (n / 2) / 8, (String) o, mText);

				mTextLayer.addText(ti);
			}
		}
	}

	@Override
	protected void addPoint(Task t, Feature f, Rule rule, Geometry g) {

	}

	private int getLevel(Feature f) {
		/* not sure if one could match these geojson properties with cartocss */
		Object o = f.get("@relations");
		if (o instanceof HashMap) {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> tags = (HashMap<String, Object>) o;
			@SuppressWarnings("unchecked")
			HashMap<String, Object> reltags = (HashMap<String, Object>) tags.get("reltags");

			if (reltags != null) {
				o = reltags.get("level");
				if (o instanceof String) {
					//log.debug("got level {}", o);
					return Integer.parseInt((String) o);
				}
			}
		}
		return 0;
	}

}
