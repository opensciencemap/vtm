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

package org.oscim.overlay;

import java.util.ArrayList;
import java.util.List;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.PointD;
import org.oscim.core.Tile;
import org.oscim.graphics.Paint.Cap;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.layer.LineLayer;
import org.oscim.renderer.overlays.BasicOverlay;
import org.oscim.theme.renderinstruction.Line;
import org.oscim.utils.FastMath;
import org.oscim.utils.LineClipper;
import org.oscim.view.MapView;

/** This class draws a path line in given color. */
public class PathOverlay extends Overlay {

	/** Stores points, converted to the map projection. */
	/* package */protected final ArrayList<GeoPoint> mPoints;
	/* package */boolean mUpdatePoints;

	/** Line style */
	/* package */Line mLineStyle;

	class RenderPath extends BasicOverlay {

		private static final byte MAX_ZOOM = 20;
		private final double MAX_SCALE;
		private static final int MIN_DIST = 2;

		// pre-projected points to zoomlovel 20
		private int[] mPreprojected;
		private final PointD mMapPoint;

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
			mMapPoint = new PointD();
			MAX_SCALE = (1 << MAX_ZOOM) * Tile.SIZE;
		}

		// note: this is called from GL-Thread. so check your syncs!
		// TODO use an Overlay-Thread to build up layers (like for Labeling)
		@Override
		public synchronized void update(MapPosition curPos,
				boolean positionChanged,
				boolean tilesChanged, Matrices matrices) {

			if (!tilesChanged && !mUpdatePoints)
				return;

			float[] projected = mPPoints;

			if (mUpdatePoints) {
				// pre-project point on zoomlelvel 20
				synchronized (mPoints) {
					mUpdatePoints = false;

					ArrayList<GeoPoint> geopoints = mPoints;
					int size = geopoints.size();
					int[] points = mPreprojected;
					mSize = size * 2;

					if (mSize > projected.length) {
						points = mPreprojected = new int[mSize];
						projected = mPPoints = new float[mSize];
					}

					for (int i = 0, j = 0; i < size; i++, j += 2) {
						GeoPoint p = geopoints.get(i);
						MercatorProjection.project(p, mMapPoint);
						points[j + 0] = (int) (mMapPoint.x * MAX_SCALE);
						points[j + 1] = (int) (mMapPoint.y * MAX_SCALE);
					}
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
			ll.line = mLineStyle;
			ll.width = ll.line.width;

			// Hack: reset verticesCnt to reuse layer
			ll.verticesCnt = 0;

			int x, y, prevX, prevY;

			int z = curPos.zoomLevel;
			float div = FastMath.pow(z - MAX_ZOOM);

			int mx = (int) (curPos.x * (Tile.SIZE << z));
			int my = (int) (curPos.y * (Tile.SIZE << z));

			int j = 0;

			// flip around dateline. complicated stuff..
			int flip = 0;
			int flipMax = Tile.SIZE << (z - 1);

			x = (int)(mPreprojected[j++] * div) - mx;
			y = (int)(mPreprojected[j++] * div) - my;

			if (x > flipMax) {
				x -= (flipMax * 2);
				flip = -1;
			} else if (x < -flipMax) {
				x += (flipMax * 2);
				flip = 1;
			}

			mClipper.clipStart(x, y);

			int i = addPoint(projected, 0, x, y);

			prevX = x;
			prevY = y;

			for (; j < size; j += 2) {
				x = (int)(mPreprojected[j + 0] * div) - mx;
				y = (int)(mPreprojected[j + 1] * div) - my;

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
			mMapPosition.copy(curPos);

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
