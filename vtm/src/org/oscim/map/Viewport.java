/*
 * Copyright 2012 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
package org.oscim.map;

import org.oscim.core.BoundingBox;
import org.oscim.core.Box;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.utils.FastMath;

/**
 * The Viewport class contains a MapPosition and the projection matrices.
 * It provides functions to modify the MapPosition and translate between
 * map and screen coordinates.
 * <p>
 * Public methods are thread safe.
 */
public class Viewport {
	//static final Logger log = LoggerFactory.getLogger(Viewport.class);

	public final static int MAX_ZOOMLEVEL = 20;
	public final static int MIN_ZOOMLEVEL = 2;

	public final static double MAX_SCALE = (1 << MAX_ZOOMLEVEL);
	public final static double MIN_SCALE = (1 << MIN_ZOOMLEVEL);

	public final static float MAX_TILT = 65;

	protected final MapPosition mPos = new MapPosition();

	protected final GLMatrix mProjMatrix = new GLMatrix();
	protected final GLMatrix mProjMatrixUnscaled = new GLMatrix();
	protected final GLMatrix mProjMatrixInverse = new GLMatrix();
	protected final GLMatrix mRotationMatrix = new GLMatrix();
	protected final GLMatrix mViewMatrix = new GLMatrix();
	protected final GLMatrix mViewProjMatrix = new GLMatrix();
	protected final GLMatrix mUnprojMatrix = new GLMatrix();
	protected final GLMatrix mTmpMatrix = new GLMatrix();

	/* temporary vars: only use in synchronized functions! */
	protected final Point mMovePoint = new Point();
	protected final float[] mv = new float[4];
	protected final float[] mu = new float[4];
	protected final float[] mViewCoords = new float[8];

	protected final Box mMapBBox = new Box();

	protected float mHeight, mWidth;

	public final static float VIEW_DISTANCE = 3.0f;
	public final static float VIEW_NEAR = 1;
	public final static float VIEW_FAR = 8;
	/** scale map plane at VIEW_DISTANCE to near plane */
	public final static float VIEW_SCALE = (VIEW_NEAR / VIEW_DISTANCE) * 0.5f;

	protected Viewport() {
		mPos.scale = MIN_SCALE;
		mPos.x = 0.5;
		mPos.y = 0.5;
		mPos.bearing = 0;
		mPos.tilt = 0;
	}

	/**
	 * Get the current MapPosition.
	 * 
	 * @param pos MapPosition to be updated.
	 * 
	 * @return true iff current position is different from
	 *         passed position.
	 */
	public synchronized boolean getMapPosition(MapPosition pos) {

		boolean changed = (pos.scale != mPos.scale
		        || pos.x != mPos.x
		        || pos.y != mPos.y
		        || pos.bearing != mPos.bearing
		        || pos.tilt != mPos.tilt);

		pos.bearing = mPos.bearing;
		pos.tilt = mPos.tilt;

		pos.x = mPos.x;
		pos.y = mPos.y;
		pos.scale = mPos.scale;
		pos.zoomLevel = FastMath.log2((int) mPos.scale);

		return changed;
	}

	/**
	 * Get the inverse projection of the viewport, i.e. the
	 * coordinates with z==0 that will be projected exactly
	 * to screen corners by current view-projection-matrix.
	 * 
	 * @param box float[8] will be set.
	 * @param add increase extents of box
	 */
	public synchronized void getMapExtents(float[] box, float add) {
		float t = getDepth(1);
		float t2 = getDepth(-1);

		// top-right
		unproject(1, -1, t, box, 0);
		// top-left
		unproject(-1, -1, t, box, 2);
		// bottom-left
		unproject(-1, 1, t2, box, 4);
		// bottom-right
		unproject(1, 1, t2, box, 6);

		if (add == 0)
			return;

		for (int i = 0; i < 8; i += 2) {
			float x = box[i];
			float y = box[i + 1];
			float len = (float) Math.sqrt(x * x + y * y);
			box[i + 0] += x / len * add;
			box[i + 1] += y / len * add;
		}
	}

	/**
	 * Get Z-value of the map-plane for a point on screen -
	 * calculate the intersection of a ray from camera origin
	 * and the map plane
	 */
	protected float getDepth(float y) {
		// origin is moved by VIEW_DISTANCE
		double cx = VIEW_DISTANCE;
		// 'height' of the ray
		double ry = y * (mHeight / mWidth) * 0.5f;

		double ua;

		if (y == 0)
			ua = 1;
		else {
			// tilt of the plane (center is kept on x = 0)
			double t = Math.toRadians(mPos.tilt);
			double px = y * Math.sin(t);
			double py = y * Math.cos(t);
			ua = 1 + (px * ry) / (py * cx);
		}

		mv[0] = 0;
		mv[1] = (float) (ry / ua);
		mv[2] = (float) (cx - cx / ua);

		mProjMatrixUnscaled.prj(mv);

		return mv[2];
	}

	protected void unproject(float x, float y, float z, float[] coords, int position) {
		mv[0] = x;
		mv[1] = y;
		mv[2] = z;

		mUnprojMatrix.prj(mv);

		coords[position + 0] = mv[0];
		coords[position + 1] = mv[1];
	}

	/**
	 * Get the minimal axis-aligned BoundingBox that encloses
	 * the visible part of the map.
	 * 
	 * @return BoundingBox containing view
	 */
	public synchronized BoundingBox getBBox(int expand) {
		getBBox(mMapBBox, expand);

		/* scale map-pixel coordinates at current scale to
		 * absolute coordinates and apply mercator projection. */
		double minLon = MercatorProjection.toLongitude(mMapBBox.xmin);
		double maxLon = MercatorProjection.toLongitude(mMapBBox.xmax);
		double minLat = MercatorProjection.toLatitude(mMapBBox.ymax);
		double maxLat = MercatorProjection.toLatitude(mMapBBox.ymin);

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	public synchronized BoundingBox getBBox() {
		return getBBox(0);
	}

	/**
	 * Get the minimal axis-aligned BoundingBox that encloses
	 * the visible part of the map. Sets box to map coordinates:
	 * xmin,ymin,ymax,ymax
	 */
	public synchronized void getBBox(Box box, int expand) {
		float[] coords = mViewCoords;
		getMapExtents(coords, expand);

		box.xmin = coords[0];
		box.xmax = coords[0];
		box.ymin = coords[1];
		box.ymax = coords[1];

		for (int i = 2; i < 8; i += 2) {
			box.xmin = Math.min(box.xmin, coords[i]);
			box.xmax = Math.max(box.xmax, coords[i]);
			box.ymin = Math.min(box.ymin, coords[i + 1]);
			box.ymax = Math.max(box.ymax, coords[i + 1]);
		}

		//updatePosition();
		double cs = mPos.scale * Tile.SIZE;
		double cx = mPos.x * cs;
		double cy = mPos.y * cs;

		box.xmin = (cx + box.xmin) / cs;
		box.xmax = (cx + box.xmax) / cs;
		box.ymin = (cy + box.ymin) / cs;
		box.ymax = (cy + box.ymax) / cs;
	}

	/**
	 * Get the GeoPoint for x,y in screen coordinates.
	 * 
	 * @param x screen coordinate
	 * @param y screen coordinate
	 * @return the corresponding GeoPoint
	 */
	public synchronized GeoPoint fromScreenPoint(float x, float y) {
		fromScreenPoint(x, y, mMovePoint);
		return new GeoPoint(
		                    MercatorProjection.toLatitude(mMovePoint.y),
		                    MercatorProjection.toLongitude(mMovePoint.x));
	}

	/**
	 * Get the map position for x,y in screen coordinates.
	 * 
	 * @param x screen coordinate
	 * @param y screen coordinate
	 */
	public synchronized void fromScreenPoint(double x, double y, Point out) {
		// scale to -1..1
		float mx = (float) (1 - (x / mWidth * 2));
		float my = (float) (1 - (y / mHeight * 2));

		unproject(-mx, my, getDepth(-my), mu, 0);

		double cs = mPos.scale * Tile.SIZE;
		double cx = mPos.x * cs;
		double cy = mPos.y * cs;

		double dx = cx + mu[0];
		double dy = cy + mu[1];

		dx /= cs;
		dy /= cs;

		while (dx > 1)
			dx -= 1;
		while (dx < 0)
			dx += 1;

		if (dy > 1)
			dy = 1;
		else if (dy < 0)
			dy = 0;

		out.x = dx;
		out.y = dy;
	}

	/**
	 * Get the screen pixel for a GeoPoint
	 * 
	 * @param geoPoint the GeoPoint
	 * @param out Point projected to screen pixel relative to center
	 */
	public synchronized void toScreenPoint(GeoPoint geoPoint, Point out) {
		MercatorProjection.project(geoPoint, out);
		toScreenPoint(out.x, out.y, out);
	}

	/**
	 * Get the screen pixel for map coordinates
	 * 
	 * @param out Point projected to screen coordinate
	 */
	public synchronized void toScreenPoint(double x, double y, Point out) {

		double cs = mPos.scale * Tile.SIZE;
		double cx = mPos.x * cs;
		double cy = mPos.y * cs;

		mv[0] = (float) (x * cs - cx);
		mv[1] = (float) (y * cs - cy);

		mv[2] = 0;
		mv[3] = 1;

		mViewProjMatrix.prj(mv);

		out.x = (mv[0] * (mWidth / 2));
		out.y = -(mv[1] * (mHeight / 2));
	}

	public synchronized boolean copy(Viewport viewport) {
		mUnprojMatrix.copy(viewport.mUnprojMatrix);
		mRotationMatrix.copy(viewport.mRotationMatrix);
		mViewMatrix.copy(viewport.mViewMatrix);
		mViewProjMatrix.copy(viewport.mViewProjMatrix);
		return viewport.getMapPosition(mPos);
	}

	public synchronized void initFrom(Viewport viewport) {
		mProjMatrix.copy(viewport.mProjMatrix);
		mProjMatrixUnscaled.copy(viewport.mProjMatrixUnscaled);
		mProjMatrixInverse.copy(viewport.mProjMatrixInverse);

		mHeight = viewport.mHeight;
		mWidth = viewport.mWidth;
	}
}
