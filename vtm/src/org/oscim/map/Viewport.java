/*
 * Copyright 2012 Hannes Janetzek
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
import org.oscim.utils.FastMath;
import org.oscim.utils.Matrix4;

/**
 * The Viewport class contains a MapPosition and the projection matrices.
 * It provides functions to modify the MapPosition and translate between
 * map and screen coordinates.
 * <p>
 * Public methods are thread safe.
 */
public class Viewport {
	//private static final String TAG = Viewport.class.getName();

	public final static int MAX_ZOOMLEVEL = 20;
	public final static int MIN_ZOOMLEVEL = 2;

	public final static double MAX_SCALE = (1 << MAX_ZOOMLEVEL);
	public final static double MIN_SCALE = (1 << MIN_ZOOMLEVEL);

	private final static float MAX_TILT = 65;

	private final MapPosition mPos = new MapPosition();

	private final Matrix4 mProjMatrix = new Matrix4();
	private final Matrix4 mProjMatrixI = new Matrix4();
	private final Matrix4 mRotMatrix = new Matrix4();
	private final Matrix4 mViewMatrix = new Matrix4();
	private final Matrix4 mVPMatrix = new Matrix4();
	private final Matrix4 mUnprojMatrix = new Matrix4();
	private final Matrix4 mTmpMatrix = new Matrix4();

	// temporary vars: only use in synchronized functions!
	private final Point mMovePoint = new Point();
	private final float[] mv = new float[4];
	private final float[] mu = new float[4];
	private final float[] mViewCoords = new float[8];

	private final Box mMapBBox = new Box();

	private float mHeight, mWidth;

	public final static float VIEW_DISTANCE = 3.0f;
	public final static float VIEW_NEAR = 1;
	public final static float VIEW_FAR = 8;
	// scale map plane at VIEW_DISTANCE to near plane
	public final static float VIEW_SCALE = (VIEW_NEAR / VIEW_DISTANCE) * 0.5f;

	Viewport(Map map) {
		mPos.scale = MIN_SCALE;
		mPos.x = 0.5;
		mPos.y = 0.5;
		mPos.angle = 0;
		mPos.tilt = 0;
	}

	public synchronized void setViewport(int width, int height) {
		float s = VIEW_SCALE;
		float aspect = height / (float) width;
		float[] tmp = new float[16];

		Matrix4.frustumM(tmp, 0, -s, s,
		                 aspect * s, -aspect * s, VIEW_NEAR, VIEW_FAR);

		mProjMatrix.set(tmp);
		mTmpMatrix.setTranslation(0, 0, -VIEW_DISTANCE);
		mProjMatrix.multiplyRhs(mTmpMatrix);
		mProjMatrix.get(tmp);

		Matrix4.invertM(tmp, 0, tmp, 0);
		mProjMatrixI.set(tmp);

		mHeight = height;
		mWidth = width;

		updateMatrix();
	}

	/**
	 * Get the current MapPosition.
	 * 
	 * @param pos MapPosition to be updated.
	 * @return true if current position is different from pos.
	 */
	public synchronized boolean getMapPosition(MapPosition pos) {

		boolean changed = (pos.scale != mPos.scale
		                   || pos.x != mPos.x
		                   || pos.y != mPos.y
		                   || pos.angle != mPos.angle
		                   || pos.tilt != mPos.tilt);

		pos.angle = mPos.angle;
		pos.tilt = mPos.tilt;

		pos.x = mPos.x;
		pos.y = mPos.y;
		pos.scale = mPos.scale;
		pos.zoomLevel = FastMath.log2((int) mPos.scale);

		return changed;
	}

	/**
	 * Get a copy of current matrices
	 * 
	 * @param view view Matrix
	 * @param proj projection Matrix
	 * @param vp view and projection
	 */
	public synchronized void getMatrix(Matrix4 view, Matrix4 proj, Matrix4 vp) {
		if (view != null)
			view.copy(mViewMatrix);

		if (proj != null)
			proj.copy(mProjMatrix);

		if (vp != null)
			vp.copy(mVPMatrix);
	}

	/**
	 * Get the inverse projection of the viewport, i.e. the
	 * coordinates with z==0 that will be projected exactly
	 * to screen corners by current view-projection-matrix.
	 * 
	 * @param box float[8] will be set.
	 */
	public synchronized void getMapViewProjection(float[] box) {
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
	}

	/*
	 * Get Z-value of the map-plane for a point on screen -
	 * calculate the intersection of a ray from camera origin
	 * and the map plane
	 */
	private float getDepth(float y) {
		if (y == 0)
			return 0;

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

		mProjMatrix.prj(mv);

		return mv[2];
	}

	private void unproject(float x, float y, float z, float[] coords, int position) {
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
	public synchronized BoundingBox getViewBox() {
		getViewBox(mMapBBox);

		// scale map-pixel coordinates at current scale to
		// absolute coordinates and apply mercator projection.
		double minLon = MercatorProjection.toLongitude(mMapBBox.minX);
		double maxLon = MercatorProjection.toLongitude(mMapBBox.maxX);
		// sic(k)
		double minLat = MercatorProjection.toLatitude(mMapBBox.maxY);
		double maxLat = MercatorProjection.toLatitude(mMapBBox.minY);

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	/**
	 * Get the minimal axis-aligned BoundingBox that encloses
	 * the visible part of the map. Sets box to map coordinates:
	 * minX,minY,maxY,maxY
	 */
	public synchronized void getViewBox(Box box) {
		float[] coords = mViewCoords;
		getMapViewProjection(coords);

		box.minX = coords[0];
		box.maxX = coords[0];
		box.minY = coords[1];
		box.maxY = coords[1];

		for (int i = 2; i < 8; i += 2) {
			box.minX = Math.min(box.minX, coords[i]);
			box.maxX = Math.max(box.maxX, coords[i]);
			box.minY = Math.min(box.minY, coords[i + 1]);
			box.maxY = Math.max(box.maxY, coords[i + 1]);
		}

		//updatePosition();
		double cs = mPos.scale * Tile.SIZE;
		double cx = mPos.x * cs;
		double cy = mPos.y * cs;

		box.minX = (cx + box.minX) / cs;
		box.maxX = (cx + box.maxX) / cs;
		box.minY = (cy + box.minY) / cs;
		box.maxY = (cy + box.maxY) / cs;
	}

	//	/**
	//	 * For x, y in screen coordinates set Point to map-tile
	//	 * coordinates at returned scale.
	//	 *
	//	 * @param x screen coordinate
	//	 * @param y screen coordinate
	//	 * @param out Point coords will be set
	//	 */
	//	public synchronized void getScreenPointOnMap(float x, float y, double scale, Point out) {
	//
	//		// scale to -1..1
	//		float mx = 1 - (x / mWidth * 2);
	//		float my = 1 - (y / mHeight * 2);
	//
	//		unproject(-mx, my, getDepth(-my), mu, 0);
	//
	//		out.x = mu[0];
	//		out.y = mu[1];
	//
	//		if (scale != 0) {
	//			out.x *= scale / mPos.scale;
	//			out.y *= scale / mPos.scale;
	//		}
	//	}

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

		if (dx > 1) {
			while (dx > 1)
				dx -= 1;
		} else {
			while (dx < 0)
				dx += 1;
		}

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

		mVPMatrix.prj(mv);

		out.x = (mv[0] * (mWidth / 2));
		out.y = -(mv[1] * (mHeight / 2));
	}

	private void updateMatrix() {
		// - view matrix
		// 1. scale to window coordinates
		// 2. apply rotate
		// 3. apply tilt

		// - projection matrix
		// 4. translate to VIEW_DISTANCE
		// 5. apply projection

		mRotMatrix.setRotation(mPos.angle, 0, 0, 1);

		// tilt map
		mTmpMatrix.setRotation(mPos.tilt, 1, 0, 0);

		// apply first rotation, then tilt
		mRotMatrix.multiplyMM(mTmpMatrix, mRotMatrix);

		// scale to window coordinates
		mTmpMatrix.setScale(1 / mWidth, 1 / mWidth, 1 / mWidth);

		mViewMatrix.multiplyMM(mRotMatrix, mTmpMatrix);

		mVPMatrix.multiplyMM(mProjMatrix, mViewMatrix);

		//--- unproject matrix:

		// inverse scale
		mUnprojMatrix.setScale(mWidth, mWidth, 1);

		// inverse rotation and tilt
		mTmpMatrix.transposeM(mRotMatrix);

		// (AB)^-1 = B^-1*A^-1, unapply scale, tilt and rotation
		mTmpMatrix.multiplyMM(mUnprojMatrix, mTmpMatrix);

		// (AB)^-1 = B^-1*A^-1, unapply projection
		mUnprojMatrix.multiplyMM(mTmpMatrix, mProjMatrixI);
	}

	/**
	 * Moves this Viewport by the given amount of pixels.
	 * 
	 * @param mx the amount of pixels to move the map horizontally.
	 * @param my the amount of pixels to move the map vertically.
	 */
	public synchronized void moveMap(float mx, float my) {
		Point p = applyRotation(mx, my);
		double tileScale = mPos.scale * Tile.SIZE;

		moveTo(mPos.x - p.x / tileScale, mPos.y - p.y / tileScale);
	}

	/* used by MapAnimator */
	void moveInternal(double mx, double my) {
		Point p = applyRotation(mx, my);
		moveTo(p.x, p.y);
	}

	private void moveTo(double x, double y) {
		mPos.x = x;
		mPos.y = y;

		// clamp latitude
		mPos.y = FastMath.clamp(mPos.y, 0, 1);

		// wrap longitude
		while (mPos.x > 1)
			mPos.x -= 1;
		while (mPos.x < 0)
			mPos.x += 1;
	}

	private Point applyRotation(double mx, double my) {
		if (mPos.angle == 0) {
			mMovePoint.x = mx;
			mMovePoint.y = my;
		} else {
			double rad = Math.toRadians(mPos.angle);
			double rcos = Math.cos(rad);
			double rsin = Math.sin(rad);
			mMovePoint.x = mx * rcos + my * rsin;
			mMovePoint.y = mx * -rsin + my * rcos;
		}
		return mMovePoint;
	}

	/**
	 * Scale map by scale width center at pivot in pixel relative to
	 * screen center. Map scale is clamp to MIN_SCALE and MAX_SCALE.
	 * 
	 * @param scale
	 * @param pivotX
	 * @param pivotY
	 * @return true if scale was changed
	 */
	public synchronized boolean scaleMap(float scale, float pivotX, float pivotY) {
		// just sanitize input
		//scale = FastMath.clamp(scale, 0.5f, 2);
		if (scale < 0.000001)
			return false;

		double newScale = mPos.scale * scale;

		newScale = FastMath.clamp(newScale, MIN_SCALE, MAX_SCALE);

		if (newScale == mPos.scale)
			return false;

		scale = (float) (newScale / mPos.scale);

		mPos.scale = newScale;

		if (pivotX != 0 || pivotY != 0)
			moveMap(pivotX * (1.0f - scale),
			        pivotY * (1.0f - scale));

		return true;
	}

	/**
	 * Rotate map by radians around pivot. Pivot is in pixel relative
	 * to screen center.
	 * 
	 * @param radians
	 * @param pivotX
	 * @param pivotY
	 */
	public synchronized void rotateMap(double radians, float pivotX, float pivotY) {

		double rsin = Math.sin(radians);
		double rcos = Math.cos(radians);

		float x = (float) (pivotX * rcos + pivotY * -rsin - pivotX);
		float y = (float) (pivotX * rsin + pivotY * rcos - pivotY);

		moveMap(x, y);

		setRotation(mPos.angle + Math.toDegrees(radians));
	}

	public synchronized void setRotation(double degree) {
		while (degree > 360)
			degree -= 360;
		while (degree < 0)
			degree += 360;

		mPos.angle = (float) degree;
		updateMatrix();
	}

	public synchronized boolean tiltMap(float move) {
		return setTilt(mPos.tilt + move);
	}

	public synchronized boolean setTilt(float tilt) {
		tilt = FastMath.clamp(tilt, 0, MAX_TILT);
		if (tilt == mPos.tilt)
			return false;
		mPos.tilt = tilt;
		updateMatrix();
		return true;
	}

	public synchronized void setMapPosition(MapPosition mapPosition) {
		mPos.scale = FastMath.clamp(mapPosition.scale, MIN_SCALE, MAX_SCALE);
		mPos.x = mapPosition.x;
		mPos.y = mapPosition.y;
		mPos.tilt = mapPosition.tilt;
		mPos.angle = mapPosition.angle;
		updateMatrix();
	}

	synchronized void setPos(double x, double y) {
		mPos.x = x;
		mPos.y = y;
	}
}
