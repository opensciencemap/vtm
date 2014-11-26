package org.oscim.map;

import static org.oscim.utils.FastMath.clamp;

import org.oscim.core.MapPosition;
import org.oscim.core.Point;
import org.oscim.core.Tile;
import org.oscim.renderer.GLMatrix;
import org.oscim.utils.FastMath;
import org.oscim.utils.ThreadUtils;

public class ViewController extends Viewport {

	protected float mPivotY = 0.0f;

	private final float[] mat = new float[16];

	public void setScreenSize(int width, int height) {
		ThreadUtils.assertMainThread();

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

		GLMatrix.frustumM(mat, 0, -VIEW_SCALE, VIEW_SCALE,
		                  ratio, -ratio, VIEW_NEAR, VIEW_FAR);

		mProjMatrix.set(mat);

		mTmpMatrix.setTranslation(0, 0, -VIEW_DISTANCE);
		mProjMatrix.multiplyRhs(mTmpMatrix);

		/* set inverse projection matrix (without scaling) */
		mProjMatrix.get(mat);
		GLMatrix.invertM(mat, 0, mat, 0);
		mProjMatrixInverse.set(mat);

		mProjMatrixUnscaled.copy(mProjMatrix);

		/* scale to window coordinates */
		mTmpMatrix.setScale(1 / mWidth, 1 / mWidth, 1 / mWidth);
		mProjMatrix.multiplyRhs(mTmpMatrix);

		updateMatrices();
	}

	/**
	 * Set pivot height relative to screen center. E.g. 0.5 is usually preferred
	 * for navigation, moving the center to 25% of the screen height.
	 * Range is [-1, 1].
	 */
	public void setMapScreenCenter(float pivotY) {
		mPivotY = FastMath.clamp(pivotY, -1, 1) * 0.5f;
	}

	/**
	 * Moves this Viewport by the given amount of pixels.
	 * 
	 * @param mx the amount of pixels to move the map horizontally.
	 * @param my the amount of pixels to move the map vertically.
	 */
	public void moveMap(float mx, float my) {
		ThreadUtils.assertMainThread();

		Point p = applyRotation(mx, my);
		double tileScale = mPos.scale * Tile.SIZE;
		moveTo(mPos.x - p.x / tileScale, mPos.y - p.y / tileScale);
	}

	/* used by MapAnimator */
	void moveTo(double x, double y) {
		mPos.x = x;
		mPos.y = y;

		/* clamp latitude */
		mPos.y = FastMath.clamp(mPos.y, 0, 1);

		/* wrap longitude */
		while (mPos.x > 1)
			mPos.x -= 1;
		while (mPos.x < 0)
			mPos.x += 1;
	}

	private Point applyRotation(double mx, double my) {
		if (mPos.bearing == 0) {
			mMovePoint.x = mx;
			mMovePoint.y = my;
		} else {
			double rad = Math.toRadians(mPos.bearing);
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
	public boolean scaleMap(float scale, float pivotX, float pivotY) {
		ThreadUtils.assertMainThread();

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

		if (pivotX != 0 || pivotY != 0) {
			pivotY -= mHeight * mPivotY;

			moveMap(pivotX * (1.0f - scale),
			        pivotY * (1.0f - scale));
		}
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
	public void rotateMap(double radians, float pivotX, float pivotY) {
		ThreadUtils.assertMainThread();

		double rsin = Math.sin(radians);
		double rcos = Math.cos(radians);

		pivotY -= mHeight * mPivotY;

		float x = (float) (pivotX - pivotX * rcos + pivotY * rsin);
		float y = (float) (pivotY - pivotX * rsin - pivotY * rcos);

		moveMap(x, y);

		setRotation(mPos.bearing + Math.toDegrees(radians));
	}

	public void setRotation(double degree) {
		ThreadUtils.assertMainThread();

		while (degree > 180)
			degree -= 360;
		while (degree < -180)
			degree += 360;

		mPos.bearing = (float) degree;
		updateMatrices();
	}

	public boolean tiltMap(float move) {
		ThreadUtils.assertMainThread();

		return setTilt(mPos.tilt + move);
	}

	public boolean setTilt(float tilt) {
		ThreadUtils.assertMainThread();

		tilt = FastMath.clamp(tilt, 0, MAX_TILT);
		if (tilt == mPos.tilt)
			return false;
		mPos.tilt = tilt;
		updateMatrices();
		return true;
	}

	public void setMapPosition(MapPosition mapPosition) {
		ThreadUtils.assertMainThread();

		mPos.scale = clamp(mapPosition.scale, MIN_SCALE, MAX_SCALE);
		mPos.x = mapPosition.x;
		mPos.y = mapPosition.y;
		mPos.tilt = clamp(mapPosition.tilt, 0, MAX_TILT);
		mPos.bearing = mapPosition.bearing;
		updateMatrices();
	}

	private void updateMatrices() {
		/* - view matrix:
		 * 0. apply rotate
		 * 1. apply tilt */

		mRotationMatrix.setRotation(mPos.bearing, 0, 0, 1);
		mTmpMatrix.setRotation(mPos.tilt, 1, 0, 0);

		/* apply first rotation, then tilt */
		mRotationMatrix.multiplyLhs(mTmpMatrix);

		mViewMatrix.copy(mRotationMatrix);

		mTmpMatrix.setTranslation(0, mPivotY * mHeight, 0);
		mViewMatrix.multiplyLhs(mTmpMatrix);

		mViewProjMatrix.multiplyMM(mProjMatrix, mViewMatrix);

		mViewProjMatrix.get(mat);
		GLMatrix.invertM(mat, 0, mat, 0);
		mUnprojMatrix.set(mat);
	}

	public final Viewport mNextFrame = new Viewport();

	/** synchronize on this object when doing multiple calls on it */
	public final Viewport getSyncViewport() {
		return mNextFrame;
	}

	void syncViewport() {
		synchronized (mNextFrame) {
			mNextFrame.copy(this);
		}
	}

	public boolean getSyncViewport(Viewport v) {
		synchronized (mNextFrame) {
			return v.copy(mNextFrame);
		}
	}

	public boolean getSyncMapPosition(MapPosition mapPosition) {
		synchronized (mNextFrame) {
			return mNextFrame.getMapPosition(mapPosition);
		}
	}

}
