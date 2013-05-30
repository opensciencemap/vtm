/*
 * Copyright 2012, osmdroid: Viesturs Zarins, Martin Pearman
 * Copyright 2012, Hannes Janetzek
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

package org.oscim.layers.overlay;

import java.util.ArrayList;
import java.util.List;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.graphics.Paint.Cap;
import org.oscim.layers.Layer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layers.BasicRenderLayer;
import org.oscim.renderer.sublayers.LineLayer;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.LineClipper;
import org.oscim.view.MapView;

/** This class draws a path line in given color. */
public class PathOverlay extends Layer {

	/** Stores points, converted to the map projection. */
	/* package */protected final ArrayList<GeoPoint> mPoints;
	/* package */boolean mUpdatePoints;

	/** Line style */
	/* package */Line mLineStyle;

	class RenderPath extends BasicRenderLayer {

		private static final int MIN_DIST = 4;

		// pre-projected points
		private double[] mPreprojected;

		// projected points
		private float[] mPPoints;
		private int mSize;
		private final LineClipper mClipper;

		// limit coords
		private final int max = 2048;

		public RenderPath(MapView mapView) {
			super(mapView);
			mClipper = new LineClipper(-max, -max, max, max, true);
			mPPoints = new float[1];
			layers.addLineLayer(0, mLineStyle);
		}

		private int mCurX = -1;
		private int mCurY = -1;
		private int mCurZ = -1;

		// note: this is called from GL-Thread. so check your syncs!
		// TODO use an Overlay-Thread to build up layers (like for Labeling)
		@Override
		public synchronized void update(MapPosition pos, boolean changed, Matrices m) {
			int tz = 1 << pos.zoomLevel;
			int tx = (int) (pos.x * tz);
			int ty = (int) (pos.y * tz);

			// update layers when map moved by at least one tile
			boolean tilesChanged = (tx != mCurX || ty != mCurY || tz != mCurZ);

			if (!tilesChanged && !mUpdatePoints)
				return;

			mCurX = tx;
			mCurY = ty;
			mCurZ = tz;


			if (mUpdatePoints) {
				synchronized (mPoints) {
					mUpdatePoints = false;

					ArrayList<GeoPoint> geopoints = mPoints;
					int size = geopoints.size();
					double[] points = mPreprojected;
					mSize = size * 2;

					if (mSize > points.length) {
						points = mPreprojected = new double[mSize];
						mPPoints = new float[mSize];
					}

					for (int i = 0; i < size; i++)
						MercatorProjection.project(geopoints.get(i), points, i);
				}
			}


			int size = mSize;
			if (size == 0) {
				if (layers.baseLayers != null) {
					layers.clear();
					newData = true;
				}
				return;
			}

			LineLayer ll = layers.getLineLayer(0);
			ll.clear();

			ll.line = mLineStyle;
			ll.width = ll.line.width;

			int z = pos.zoomLevel;

			double mx = pos.x;
			double my = pos.y;
			double scale = Tile.SIZE * (1 << z);


			// flip around dateline. complicated stuff..
			int flip = 0;
			int flipMax = Tile.SIZE << (z - 1);

			int x = (int) ((mPreprojected[0] - mx) * scale);
			int y = (int) ((mPreprojected[1] - my) * scale);

			if (x > flipMax) {
				x -= (flipMax * 2);
				flip = -1;
			} else if (x < -flipMax) {
				x += (flipMax * 2);
				flip = 1;
			}

			mClipper.clipStart(x, y);

			float[] projected = mPPoints;
			int i = addPoint(projected, 0, x, y);

			int prevX = x;
			int prevY = y;

			for (int j = 2; j < size; j += 2) {
				x = (int) ((mPreprojected[j + 0] - mx) * scale);
				y = (int) ((mPreprojected[j + 1] - my) * scale);

				int curFlip = 0;
				if (x > flipMax) {
					x -= flipMax * 2;
					curFlip = -1;
				} else if (x < -flipMax) {
					x += flipMax * 2;
					curFlip = 1;
				}

				if (flip != curFlip) {
					flip = curFlip;
					if (i > 2)
						ll.addLine(projected, i, false);

					mClipper.clipStart(x, y);
					i = addPoint(projected, 0, x, y);
					continue;
				}

				int clip = mClipper.clipNext(x, y);
				if (clip < 1) {
					if (i > 2)
						ll.addLine(projected, i, false);

					if (clip < 0) {
						// add line segment
						projected[0] = mClipper.out[0];
						projected[1] = mClipper.out[1];

						projected[2] = prevX = mClipper.out[2];
						projected[3] = prevY = mClipper.out[3];
						ll.addLine(projected, 4, false);
					}
					i = 0;
					continue;
				}

				int dx = x - prevX;
				int dy = y - prevY;
				if ((i == 0) || FastMath.absMaxCmp(dx, dy, MIN_DIST)) {
					projected[i++] = prevX = x;
					projected[i++] = prevY = y;
				}
			}
			if (i > 2)
				ll.addLine(projected, i, false);

			// keep position to render relative to current state
			mMapPosition.copy(pos);

			// items are placed relative to scale 1
			mMapPosition.scale = 1 << z;

			newData = true;
		}

		private int addPoint(float[] points, int i, int x, int y) {
			points[i++] = x;
			points[i++] = y;
			return i;
		}
	}

	public PathOverlay(MapView mapView, int lineColor, float lineWidth) {
		super(mapView);

		mLineStyle = new Line(lineColor, lineWidth, Cap.BUTT);

		this.mPoints = new ArrayList<GeoPoint>();

		mLayer = new RenderPath(mapView);
	}

	public PathOverlay(MapView mapView, int lineColor) {
		this(mapView, lineColor, 2);
	}

	/**
	 * Draw a great circle. Calculate a point for every 100km along the path.
	 *
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 */
	public void addGreatCircle(GeoPoint startPoint, GeoPoint endPoint) {
		synchronized (mPoints) {

			// get the great circle path length in meters
			final int greatCircleLength = startPoint.distanceTo(endPoint);

			// add one point for every 100kms of the great circle path
			final int numberOfPoints = greatCircleLength / 100000;

			addGreatCircle(startPoint, endPoint, numberOfPoints);
		}
	}

	/**
	 * Draw a great circle.
	 *
	 * @param startPoint
	 *            start point of the great circle
	 * @param endPoint
	 *            end point of the great circle
	 * @param numberOfPoints
	 *            number of points to calculate along the path
	 */
	public void addGreatCircle(GeoPoint startPoint, GeoPoint endPoint,
			final int numberOfPoints) {
		// adapted from page
		// http://compastic.blogspot.co.uk/2011/07/how-to-draw-great-circle-on-map-in.html
		// which was adapted from page http://maps.forum.nu/gm_flight_path.html

		// convert to radians
		final double lat1 = startPoint.getLatitude() * Math.PI / 180;
		final double lon1 = startPoint.getLongitude() * Math.PI / 180;
		final double lat2 = endPoint.getLatitude() * Math.PI / 180;
		final double lon2 = endPoint.getLongitude() * Math.PI / 180;

		final double d = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) / 2), 2)
				+ Math.cos(lat1) * Math.cos(lat2)
				* Math.pow(Math.sin((lon1 - lon2) / 2), 2)));
		double bearing = Math.atan2(
				Math.sin(lon1 - lon2) * Math.cos(lat2),
				Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2)
						* Math.cos(lon1 - lon2))
				/ -(Math.PI / 180);
		bearing = bearing < 0 ? 360 + bearing : bearing;

		for (int i = 0, j = numberOfPoints + 1; i < j; i++) {
			final double f = 1.0 / numberOfPoints * i;
			final double A = Math.sin((1 - f) * d) / Math.sin(d);
			final double B = Math.sin(f * d) / Math.sin(d);
			final double x = A * Math.cos(lat1) * Math.cos(lon1) + B * Math.cos(lat2)
					* Math.cos(lon2);
			final double y = A * Math.cos(lat1) * Math.sin(lon1) + B * Math.cos(lat2)
					* Math.sin(lon2);
			final double z = A * Math.sin(lat1) + B * Math.sin(lat2);

			final double latN = Math.atan2(z, Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2)));
			final double lonN = Math.atan2(y, x);
			addPoint((int) (latN / (Math.PI / 180) * 1E6), (int) (lonN / (Math.PI / 180) * 1E6));
		}
	}

	public void clearPath() {
		if (mPoints.isEmpty())
			return;

		synchronized (mPoints) {
			mPoints.clear();
			mUpdatePoints = true;
		}
	}

	public void setPoints(List<GeoPoint> pts) {
		synchronized (mPoints) {
			mPoints.clear();
			mPoints.addAll(pts);
			mUpdatePoints = true;
		}
	}

	public void addPoint(GeoPoint pt) {
		synchronized (mPoints) {
			mPoints.add(pt);
			mUpdatePoints = true;
		}
	}

	public void addPoint(int latitudeE6, int longitudeE6) {
		synchronized (mPoints) {
			mPoints.add(new GeoPoint(latitudeE6, longitudeE6));
			mUpdatePoints = true;
		}
	}

	public List<GeoPoint> getPoints() {
		return mPoints;
	}
}
