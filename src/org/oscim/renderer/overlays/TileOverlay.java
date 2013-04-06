/*
 * Copyright 2013 ...
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

	private final int mOverlayOffsetX = 0;
	private final int mOverlayOffsetY = 0;
	private final float mOverlayScale = 1.5f;


	private TileSet mTileSet;

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

			// fix current MapPosition
			//mMapPosition.copy(curPos);
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
		// than used for tiles (so that their gl_less test
		// evaluates to true inside the circle)
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glPolygonOffset(1, 100);

		//GLState.blend(true);
		// TODO check, depth test needs to be enabled to write?
		GLState.test(true, false);

		// unbind previously bound VBOs
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		// Load the vertex data
		GLES20.glVertexAttribPointer(hVertexPosition, 4,
				GLES20.GL_FLOAT, false, 0, mVertices);

		GLState.enableVertexArrays(hVertexPosition, -1);


		m.mvp.setTranslation(mOverlayOffsetX, mOverlayOffsetY, 0);

		float ratio = 1f / mMapView.getWidth();
		tmpMatrix.setScale(ratio, ratio, ratio);
		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		m.mvp.multiplyMM(m.proj, m.mvp);
		m.mvp.setAsUniform(hMatrixPosition);

		// Draw the triangle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);



		// get current tiles
		mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		mDrawCnt = 0;
		GLES20.glDepthFunc(GLES20.GL_LESS);

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
		float x = (float) (tile.pixelX - pos.x * div);
		float y = (float) (tile.pixelY - pos.y * div);
		float scale = pos.scale / div;

		x -= mOverlayOffsetX / scale;
		y -= mOverlayOffsetY / scale;

		m.mvp.setTransScale(x * scale, y * scale, scale / GLRenderer.COORD_SCALE);
		//m.mvp.multiplyMM(m.viewproj, m.mvp);

		tmpMatrix.setRotation(pos.angle, 0, 0, 1);
		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		float ratio = mOverlayScale / mMapView.getWidth();
		tmpMatrix.setScale(ratio, ratio, ratio);
		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		m.mvp.multiplyMM(m.proj, m.mvp);

		//tmpMatrix.setTransScale(500/0.5f, 500/0.5f, 0.5f);

		//m.mvp.multiplyMM(tmpMatrix, m.mvp);

		//		tmpMatrix.setTransScale(-500, -500, 1);
		//		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		// set depth offset (used for clipping to tile boundaries)
		GLES20.glPolygonOffset(1, mDrawCnt++);
		if (mDrawCnt > 20)
			mDrawCnt = 0;

		// simple line shader does not take forward shortening into account
		int simpleShader = 0; //= (pos.tilt < 1 ? 1 : 0);

		boolean clipped = false;

		for (Layer l = t.layers.baseLayers; l != null;) {
			switch (l.type) {
				case Layer.POLYGON:
					l = PolygonRenderer.draw(pos, l, m, !clipped, true);
					clipped = true;
					break;

				case Layer.LINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, true);
						clipped = true;
					}
					l = LineRenderer.draw(t.layers, l, pos, m, div, simpleShader);
					break;

				case Layer.TEXLINE:
					if (!clipped) {
						// draw stencil buffer clip region
						PolygonRenderer.draw(pos, null, m, true, true);
						clipped = true;
					}
					l = LineTexRenderer.draw(t.layers, l, pos, m, div);
					break;

				default:
					// just in case
					l = l.next;
			}
		}

		// clear clip-region and could also draw 'fade-effect'
		PolygonRenderer.drawOver(m);
	}

	private int mShaderProgram;
	private int hVertexPosition;
	private int hMatrixPosition;

	private FloatBuffer mVertices;
	private final float[] mVerticesData = {
			-200, -200, -1, -1,
			-200, 200, -1, 1,
			200, -200, 1, -1,
			200, 200, 1, 1
	};
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

		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();

		mVertices.put(mVerticesData);
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
					+ "void main()"
					+ "{"
					+ " if (length(tex) < 1.0)"
					+ "   gl_FragColor = vec4 (0.65, 0.65, 0.65, 0.7);"
					+ " else"
					// dont write pixel (also discards writing to depth buffer)
					+"    discard;"
					//+ "   float a = 1.0 - step(1.0, length(tex));"
					//+ "   float a = 1.0 - smoothstep(0.5, 1.0, length(tex));"
					//+ "  gl_FragColor = vec4 (a, 0.0, 0.0, a);"
					+ "}";
}
