package org.oscim.map;

import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.utils.FastMath;

public class ViewController extends Viewport {

	ViewController(Map map) {
		super(map);
	}

	public synchronized void setScreenSize(int width, int height) {
		mHeight = height;
		mWidth = width;

		/* setup projection matrix:
		 * 0. scale to window coordinates
		 * 1. translate to VIEW_DISTANCE
		 * 2. apply projection
		 * setup inverse projection:
		 * 0. invert projection
		 * 1. invert translate to VIEW_DISTANCE */

		float ratio = (mHeight / mWidth) * VIEW_SCALE;
		float[] tmp = new float[16];

		GLMatrix.frustumM(tmp, 0, -VIEW_SCALE, VIEW_SCALE,
		                  ratio, -ratio, VIEW_NEAR, VIEW_FAR);

		mProjMatrix.set(tmp);
		mTmpMatrix.setTranslation(0, 0, -VIEW_DISTANCE);
		mProjMatrix.multiplyRhs(mTmpMatrix);

		/* set inverse projection matrix (without scaling) */
		mProjMatrix.get(tmp);
		GLMatrix.invertM(tmp, 0, tmp, 0);
		mProjMatrixI.set(tmp);

		mProjMatrixUnscaled.copy(mProjMatrix);

		/* scale to window coordinates */
		mTmpMatrix.setScale(1 / mWidth, 1 / mWidth, 1 / mWidth);
		mProjMatrix.multiplyRhs(mTmpMatrix);

		updateMatrix();
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
	void moveTo(double x, double y) {
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

		float x = (float) (pivotX - pivotX * rcos + pivotY * rsin);
		float y = (float) (pivotY - pivotX * rsin - pivotY * rcos);

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

	private void updateMatrix() {
		/* - view matrix:
		 * 0. apply rotate
		 * 1. apply tilt */

		mRotMatrix.setRotation(mPos.angle, 0, 0, 1);
		mTmpMatrix.setRotation(mPos.tilt, 1, 0, 0);

		/* apply first rotation, then tilt */
		mRotMatrix.multiplyLhs(mTmpMatrix);

		mViewMatrix.copy(mRotMatrix);

		mVPMatrix.multiplyMM(mProjMatrix, mViewMatrix);

		/* inverse projection matrix: */
		/* invert scale */
		mUnprojMatrix.setScale(mWidth, mWidth, 1);

		/* invert rotation and tilt */
		mTmpMatrix.transposeM(mRotMatrix);

		/* (AB)^-1 = B^-1*A^-1, invert scale, tilt and rotation */
		mTmpMatrix.multiplyLhs(mUnprojMatrix);

		/* (AB)^-1 = B^-1*A^-1, invert projection */
		mUnprojMatrix.multiplyMM(mTmpMatrix, mProjMatrixI);
	}
}
