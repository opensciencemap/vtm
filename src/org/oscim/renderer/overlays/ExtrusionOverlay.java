/*
 * Copyright 2012 OpenScienceMap
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
import java.nio.ShortBuffer;

import org.oscim.core.MapPosition;
import org.oscim.generator.JobTile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileManager;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.ExtrusionLayer;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class ExtrusionOverlay extends RenderOverlay {
	private final static String TAG = ExtrusionOverlay.class.getName();

	public ExtrusionOverlay(MapView mapView) {
		super(mapView);
	}

	private static int extrusionProgram;
	private static int hExtrusionVertexPosition;
	private static int hExtrusionLightPosition;
	private static int hExtrusionMatrix;
	private static int hExtrusionColor;
	private static int hExtrusionMode;

	private boolean initialized = false;

	// TODO sum up size used while filling layer only up to:
	private int BUFFERSIZE = 65536 * 2;
	private TileSet mTileSet;
	private ShortBuffer mShortBuffer;
	private MapTile[] mTiles;
	private int mTileCnt;

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		if (!initialized) {
			initialized = true;

			// Set up the program for rendering extrusions
			extrusionProgram = GlUtils.createProgram(extrusionVertexShader,
					extrusionFragmentShader);
			if (extrusionProgram == 0) {
				Log.e(TAG, "Could not create extrusion shader program.");
				return;
			}
			hExtrusionMatrix = GLES20.glGetUniformLocation(extrusionProgram, "u_mvp");
			hExtrusionColor = GLES20.glGetUniformLocation(extrusionProgram, "u_color");
			hExtrusionMode = GLES20.glGetUniformLocation(extrusionProgram, "u_mode");
			hExtrusionVertexPosition = GLES20.glGetAttribLocation(extrusionProgram, "a_position");
			hExtrusionLightPosition = GLES20.glGetAttribLocation(extrusionProgram, "a_light");

			ByteBuffer buf = ByteBuffer.allocateDirect(BUFFERSIZE)
					.order(ByteOrder.nativeOrder());

			mShortBuffer = buf.asShortBuffer();
		}

		int ready = 0;
		//if (curPos.zoomLevel < 17)
		mTileSet = TileManager.getActiveTiles(mTileSet);
		MapTile[] tiles = mTileSet.tiles;

		// keep a list of tiles available for rendering
		if (mTiles == null || mTiles.length != tiles.length)
			mTiles = new MapTile[tiles.length];
		ExtrusionLayer el;
		if (curPos.zoomLevel >= 17) {
			for (int i = 0; i < mTileSet.cnt; i++) {
				if (!tiles[i].isVisible)
					continue;

				el = getLayer(tiles[i]);
				if (el == null)
					continue;

				if (!el.compiled) {
					el.compile(mShortBuffer);
					GlUtils.checkGlError("...");
				}

				if (el.compiled)
					mTiles[ready++] = tiles[i];
			}
		} else if (curPos.zoomLevel == 16) {
			for (int i = 0; i < mTileSet.cnt; i++) {
				if (!tiles[i].isVisible)
					continue;
				MapTile t = tiles[i];

				for (byte j = 0; j < 4; j++) {
					if ((t.proxies & (1 << j)) != 0) {
						MapTile c = t.rel.child[j].tile;
						el = getLayer(c);

						if (el == null || !el.compiled)
							continue;

						// TODO check overflow, even if very unlikely...
						mTiles[ready++] = c;
					}
				}
			}
		}

		mTileCnt = ready;
		isReady = ready > 0;
	}

	private static ExtrusionLayer getLayer(MapTile t) {
		if (t.layers != null && t.layers.extrusionLayers != null
				&& t.state == JobTile.STATE_READY)
			return (ExtrusionLayer) t.layers.extrusionLayers;
		return null;
	}

	private boolean debug = false;
	private final float[] mVPMatrix = new float[16];

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {

		Matrix.multiplyMM(mVPMatrix, 0, proj, 0, pos.viewMatrix, 0);
		proj = mVPMatrix;

		MapTile[] tiles = mTiles;

		float div = FastMath.pow(tiles[0].zoomLevel - pos.zoomLevel);

		int depthScale = 1;

		if (debug) {
			GLES20.glUseProgram(extrusionProgram);

			GLState.enableVertexArrays(hExtrusionVertexPosition, hExtrusionLightPosition);
			GLES20.glUniform1i(hExtrusionMode, 0);
			GLES20.glUniform4fv(hExtrusionColor, 4, mColor, 0);

			GLState.test(false, false);

			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

				setMatrix(pos, mv, proj, tiles[i], div);
				GLES20.glUniformMatrix4fv(hExtrusionMatrix, 1, false, mv, 0);

				GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

				GLES20.glVertexAttribPointer(hExtrusionVertexPosition, 3,
						GLES20.GL_SHORT, false, 8, 0);

				GLES20.glVertexAttribPointer(hExtrusionLightPosition, 2,
						GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

				GLES20.glUniform4f(hExtrusionColor, 0.6f, 0.6f, 0.6f, 0.8f);
				GLES20.glDrawElements(GLES20.GL_TRIANGLES,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
						GLES20.GL_UNSIGNED_SHORT, 0);

				GLES20.glUniform4f(hExtrusionColor, 1.0f, 0.5f, 0.5f, 0.9f);

				GLES20.glDrawElements(GLES20.GL_LINES, el.mIndiceCnt[3],
						GLES20.GL_UNSIGNED_SHORT,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

				// just a temporary reference!
				tiles[i] = null;
			}
			return;
		}
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);
		GLES20.glUseProgram(extrusionProgram);
		GLState.enableVertexArrays(hExtrusionVertexPosition, -1);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glCullFace(GLES20.GL_FRONT);
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glDepthMask(true);
		GLES20.glColorMask(false, false, false, false);
		GLES20.glUniform1i(hExtrusionMode, 0);
		GLES20.glUniform4fv(hExtrusionColor, 4, mColor, 0);

		// draw to depth buffer
		for (int i = 0; i < mTileCnt; i++) {
			ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

			GLES20.glPolygonOffset(depthScale, GLRenderer.depthOffset(tiles[i]));

			setMatrix(pos, mv, proj, tiles[i], div);
			GLES20.glUniformMatrix4fv(hExtrusionMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(hExtrusionVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
					GLES20.GL_UNSIGNED_SHORT, 0);
		}

		// enable color buffer, use depth mask
		GLState.enableVertexArrays(hExtrusionVertexPosition, hExtrusionLightPosition);
		GLES20.glColorMask(true, true, true, true);
		GLES20.glDepthMask(false);
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);

		for (int i = 0; i < mTileCnt; i++) {
			ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

			GLES20.glPolygonOffset(depthScale, GLRenderer.depthOffset(tiles[i]));

			setMatrix(pos, mv, proj, tiles[i], div);
			GLES20.glUniformMatrix4fv(hExtrusionMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(hExtrusionVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glVertexAttribPointer(hExtrusionLightPosition, 2,
					GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

			// draw roof
			GLES20.glUniform1i(hExtrusionMode, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[2],
					GLES20.GL_UNSIGNED_SHORT, (el.mIndiceCnt[0] + el.mIndiceCnt[1]) * 2);

			// draw sides 1
			GLES20.glUniform1i(hExtrusionMode, 1);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[0],
					GLES20.GL_UNSIGNED_SHORT, 0);

			// draw sides 2
			GLES20.glUniform1i(hExtrusionMode, 2);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[1],
					GLES20.GL_UNSIGNED_SHORT, el.mIndiceCnt[0] * 2);

			GLES20.glUniform1i(hExtrusionMode, 3);
			GLES20.glDrawElements(GLES20.GL_LINES, el.mIndiceCnt[3],
					GLES20.GL_UNSIGNED_SHORT,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

			// just a temporary reference!
			tiles[i] = null;
		}

		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

	}

	private static void setMatrix(MapPosition mapPosition, float[] matrix, float[] proj,
			MapTile tile, float div) {

		float x = (float) (tile.pixelX - mapPosition.x * div);
		float y = (float) (tile.pixelY - mapPosition.y * div);
		float scale = mapPosition.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;

		// scale to tile to world coordinates
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;
		matrix[10] = scale / 1000f;

		Matrix.multiplyMM(matrix, 0, proj, 0, matrix, 0);
	}

	private final float _a = 0.8f;

	private final float[] mColor = {
			// roof color
			236 / 255f * _a, 235 / 255f * _a, 234 / 255f * _a, _a,
			// sligthly differ adjacent side faces to improve contrast
			201 / 255f, 200 / 255f, 198 / 255f, _a,
			200 / 255f, 200 / 255f, 196 / 255f, _a,
			// roof outline
			0.75f, 0.75f, 0.75f, 1.0f
	};

	final static String extrusionVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color[4];"
			+ "uniform int u_mode;"
			+ "attribute vec4 a_position;"
			+ "attribute vec2 a_light;"
			+ "varying vec4 color;"
			+ "const float ff = 255.0;"
			+ "const float a = 0.8;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_position;"
			+ "  if (u_mode == 0)"
			//     roof / depth pass
			+ "    color = u_color[0];"
			+ "  else if (u_mode == 1)"
			//     sides 1 - use 0xff00
			+ "    color = vec4(u_color[1].rgb * ((0.90 + (0.5 - (a_light.y / ff)) * 0.2) * a), a);"
			+ "  else if (u_mode == 2)"
			//     sides 2 - use 0x00ff
			+ "    color = vec4(u_color[2].rgb * ((0.90 + (0.5 - (a_light.x / ff)) * 0.2) * a), a);"
			+ "  else"
			//     outline - decrease contrast with distance
			+ "    color = u_color[3] * (0.98 + gl_Position.z * 0.02);"
			+ "}";

	//	final static String extrusionVertexAnimShader = ""
	//			+ "precision mediump float;"
	//			+ "uniform mat4 u_mvp;"
	//			+ "uniform vec4 u_color;"
	//			+ "uniform int u_mode;"
	//			+ "uniform float u_adv;"
	//			+ "attribute vec4 a_pos;"
	//			+ "attribute vec2 a_light;"
	//			+ "varying vec4 color;"
	//			+ "const float ff = 255.0;"
	//			+ "void main() {"
	//			+ "  gl_Position = u_mvp * vec4(a_pos.xy, a_pos.z * adv, 1.0);"
	//			+ "  if (u_mode == 0)"
	//			//     roof / depth pass
	//			+ "    color = u_color;"
	//			+ "  else if (u_mode == 1)"
	//			//     sides 1 - use 0xff00
	//			+ "    color = vec4(u_color.rgb * (a_light.y / ff), 0.8);"
	//			+ "  else"
	//			//     sides 2 - use 0x00ff
	//			+ "    color = vec4(u_color.rgb * (a_light.x / ff), 0.8);"
	//			+ "}";

	final static String extrusionFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";
}
