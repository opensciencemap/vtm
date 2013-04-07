/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer.overlays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.core.MapPosition;
import org.oscim.core.PointD;
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.LineRenderer;
import org.oscim.renderer.LineTexRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.PolygonRenderer;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.Layer;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.utils.Matrix4;
import org.oscim.view.MapView;

import android.opengl.GLES20;

public class TileOverlay extends RenderOverlay {

	private final static String TAG = TileOverlay.class.getName();

	private float mDownX = 400;
	private float mDownY = 400;

	private float mOverlayOffsetX;
	private float mOverlayOffsetY;

	private final float mOverlayScale = 1.8f;

	private final PointD mScreenPoint = new PointD();

	private TileSet mTileSet;

	private float mScreenWidth;
	private float mScreenHeight;

	private final Matrix4 mProjMatrix = new Matrix4();
	private final Matrix4 mViewProjMatrix = new Matrix4();

	public TileOverlay(MapView mapView) {
		super(mapView);
	}

	@Override
	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged,
			Matrices matrices) {

		if (!mInitialized) {
			if (!init())
				return;

			this.isReady = true;

			mProjMatrix.copy(matrices.proj);
			// discard z projection from tilt
			mProjMatrix.setValue(10, 0);
			mProjMatrix.setValue(14, 0);
		}
	}

	@Override
	public void compile() {
	}

	@Override
	public void render(MapPosition pos, Matrices m) {

		GLES20.glDepthMask(true);
		// set depth buffer to min depth -1
		GLES20.glClearDepthf(-1);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		// back to usual
		GLES20.glClearDepthf(1);

		GLES20.glDepthFunc(GLES20.GL_ALWAYS);

		GLState.useProgram(mShaderProgram);

		// set depth offset of overlay circle to be greater
		// than those used for tiles (so that their gl_less test
		// evaluates to true inside the circle)
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glPolygonOffset(1, 100);

		GLState.blend(true);
		GLState.test(true, false);

		// unbind previously bound VBOs
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		// Load the vertex data
		GLES20.glVertexAttribPointer(hVertexPosition, 4,
				GLES20.GL_FLOAT, false, 0, mVertices);

		GLState.enableVertexArrays(hVertexPosition, -1);

		mScreenWidth = mMapView.getWidth();
		mScreenHeight = mMapView.getHeight();

		mOverlayOffsetX = mDownX;
		mOverlayOffsetY = mDownY;

		m.mvp.setTranslation(mOverlayOffsetX - mScreenWidth / 2,
				mOverlayOffsetY - mScreenHeight / 2, 0);

		float ratio = 1f / mScreenWidth;
		tmpMatrix.setScale(ratio, ratio, ratio);

		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		m.mvp.multiplyMM(m.proj, m.mvp);
		m.mvp.setAsUniform(hMatrixPosition);

		// Draw the circle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// get current tiles
		mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		mDrawCnt = 0;
		GLES20.glDepthFunc(GLES20.GL_LESS);

		// scale position passed to Line/PolyRenderer
		// FIXME scale is used for both alpha fading
		// and line width...
		MapPosition scaledPos = mMapPosition;
		scaledPos.copy(pos);

		//FIXME scaledPos.scale *= mOverlayScale;

		// get translation vector from map at screen point to center
		// relative to current scale
		mMapView.getMapViewPosition().getScreenPointOnMap(
				mOverlayOffsetX, mOverlayOffsetY,
				mMapPosition.scale, mScreenPoint);

		mViewProjMatrix.setTranslation(
				-(float)mScreenPoint.x,
				-(float)mScreenPoint.y, 0);

		// rotate around center
		tmpMatrix.setRotation(pos.angle, 0, 0, 1);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		// translate to overlay circle in screen coordinates
		tmpMatrix.setTransScale(
				(mOverlayOffsetX - mScreenWidth / 2),
				(mOverlayOffsetY - mScreenHeight / 2),
				mOverlayScale);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		// normalize coordinates, i guess thats how it's called
		tmpMatrix.setScale(ratio, ratio, ratio);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		mViewProjMatrix.multiplyMM(mProjMatrix, mViewProjMatrix);

		// load texture for line caps
		LineRenderer.beginLines();

		for (int i = 0; i < mTileSet.cnt; i++) {
			MapTile t = mTileSet.tiles[i];
			drawTile(t, pos, m);
		}

		LineRenderer.endLines();

	}

	private final Matrix4 tmpMatrix = new Matrix4();

	private int mDrawCnt;

	private void drawTile(MapTile tile, MapPosition pos, Matrices m) {
		MapTile t = tile;

		//if (t.holder != null)
		//	t = t.holder;

		if (t.layers == null || t.layers.vbo == null) {
			//Log.d(TAG, "missing data " + (t.layers == null) + " " + (t.vbo == null));
			return;
		}

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, t.layers.vbo.id);

		// place tile relative to map position
		float div = FastMath.pow(tile.zoomLevel - pos.zoomLevel);
		int z = tile.zoomLevel;
		double curScale = Tile.TILE_SIZE * pos.scale;

		double scale = pos.scale / (1 << z);

		double x = (tile.x - pos.x) * curScale;
		double y = (tile.y - pos.y) * curScale;

		m.mvp.setTransScale((float) x, (float) y,
				(float) (scale / GLRenderer.COORD_SCALE));

		m.mvp.multiplyMM(mViewProjMatrix, m.mvp);

		// set depth offset (used for clipping to tile boundaries)
		GLES20.glPolygonOffset(1, mDrawCnt++);
		if (mDrawCnt == 100)
			mDrawCnt = 0;

		// simple line shader does not take forward shortening into account
		int simpleShader = 0; //= (pos.tilt < 1 ? 1 : 0);

		boolean clipped = false;

		MapPosition scaledPos = mMapPosition;

		for (Layer l = t.layers.baseLayers; l != null;) {
			switch (l.type) {
				case Layer.POLYGON:
					l = PolygonRenderer.draw(scaledPos, l, m, !clipped, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(scaledPos, null, m, true, true);
						clipped = true;
					}
					l = LineRenderer.draw(t.layers, l, scaledPos, m, div, simpleShader);
					break;

				case Layer.TEXLINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(scaledPos, null, m, true, true);
						clipped = true;
					}
					l = LineTexRenderer.draw(t.layers, l, scaledPos, m, div);
					break;

				default:
					// just in case
					l = l.next;
			}
		}

		// clear clip-region and could also draw 'fade-effect'
		PolygonRenderer.drawOver(m, false, 0);
	}

	private int mShaderProgram;
	private int hVertexPosition;
	private int hMatrixPosition;

	private FloatBuffer mVertices;
	private boolean mInitialized;

	private boolean init() {
		// Load the vertex/fragment shaders
		int programObject = GlUtils.createProgram(vShaderStr, fShaderStr);

		if (programObject == 0)
			return false;

		// Handle for vertex position in shader
		hVertexPosition = GLES20.glGetAttribLocation(programObject, "a_pos");
		hMatrixPosition = GLES20.glGetUniformLocation(programObject, "u_mvp");

		// Store the program object
		mShaderProgram = programObject;
		int halfRadius = 250;
		float[] vertices = {
				-halfRadius, -halfRadius, -1, -1,
				-halfRadius, halfRadius, -1, 1,
				halfRadius, -halfRadius, 1, -1,
				halfRadius, halfRadius, 1, 1
		};

		mVertices = ByteBuffer.allocateDirect(vertices.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();

		mVertices.put(vertices);
		mVertices.flip();

		mInitialized = true;

		return true;
	}

	private final static String vShaderStr =
			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
					+ "attribute vec4 a_pos;"
					+ "varying vec2 tex;"
					+ "void main()"
					+ "{"
					+ "   gl_Position = u_mvp * vec4(a_pos.xy, 0.0, 1.0);"
					+ "   tex = a_pos.zw;"
					+ "}";

	private final static String fShaderStr =
			"precision mediump float;"
					+ "varying vec2 tex;"
					+ "const vec4 color = vec4(0.95, 0.95, 0.95, 0.95);"
					+ "void main()"
					+ "{"
					+ " if (length(tex) < 1.0)"
					+ "   gl_FragColor = color;"
					+ " else"
					// dont write pixel (also discards writing to depth buffer)
					+ "    discard;"
					+ "}";

	public void setPointer(float x, float y) {
		mDownX = x;
		mDownY = y;
	}
}
