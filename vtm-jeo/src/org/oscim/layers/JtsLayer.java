package org.oscim.layers;

import org.jeo.geom.CoordinatePath;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.layers.vector.AbstractVectorLayer;
import org.oscim.map.Map;
import org.oscim.renderer.bucket.LineBucket;
import org.oscim.renderer.bucket.MeshBucket;
import org.oscim.utils.geom.SimplifyDP;
import org.oscim.utils.geom.SimplifyVW;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public abstract class JtsLayer extends AbstractVectorLayer<Geometry> {

	public JtsLayer(Map map) {
		super(map);
	}

	@Override
	protected void processFeatures(Task t, BoundingBox bbox) {
		processFeatures(t, new Envelope(bbox.getMinLongitude(), bbox.getMaxLongitude(),
		                                bbox.getMinLatitude(), bbox.getMaxLatitude()));

	}

	protected abstract void processFeatures(Task t, Envelope e);

	protected int transformPath(MapPosition pos, GeometryBuffer g, CoordinatePath path) {

		double scale = pos.scale * Tile.SIZE / UNSCALE_COORD;
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

	SimplifyDP mSimpDP = new SimplifyDP();
	SimplifyVW mSimpVW = new SimplifyVW();

	protected void addPolygon(Task t, Geometry g, MeshBucket ml, LineBucket ll) {
		mGeom.clear();
		mGeom.startPolygon();

		CoordinatePath p = CoordinatePath.create(g);
		if (mMinX > 0 || mMinY > 0)
			p.generalize(mMinX, mMinY);

		if (transformPath(t.position, mGeom, p) < 3)
			return;

		if (!mClipper.clip(mGeom))
			return;

		mSimpVW.simplify(mGeom, 0.1f);
		mSimpDP.simplify(mGeom, 0.5f);

		ll.addLine(mGeom);
		ml.addMesh(mGeom);
	}

	protected void addLine(Task t, Geometry g, LineBucket ll) {
		mGeom.clear();
		mGeom.startLine();

		CoordinatePath p = CoordinatePath.create(g);
		transformPath(t.position, mGeom, p);

		ll.addLine(mGeom);
	}
}
