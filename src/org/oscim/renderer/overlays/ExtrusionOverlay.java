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

	private static int[] extrusionProgram = new int[2];
	private static int[] hExtrusionVertexPosition = new int[2];
	private static int[] hExtrusionLightPosition = new int[2];
	private static int[] hExtrusionMatrix = new int[2];
	private static int[] hExtrusionColor = new int[2];
	private static int[] hExtrusionAlpha = new int[2];
	private static int[] hExtrusionMode = new int[2];

	private boolean initialized = false;

	// FIXME sum up size used while filling layer only up to:
	private int BUFFERSIZE = 65536 * 2;
	private TileSet mTileSet;
	private ShortBuffer mShortBuffer;
	private MapTile[] mTiles;
	private int mTileCnt;

	@Override
	public void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		if (!initialized) {
			initialized = true;

			for (int i = 1; i < 2; i++) {
				// Set up the program for rendering extrusions
				extrusionProgram[i] = GlUtils.createProgram(extrusionVertexShader[i],
						extrusionFragmentShader);
				if (extrusionProgram[i] == 0) {
					Log.e(TAG, "Could not create extrusion shader program. " + i);
					return;
				}
				hExtrusionMatrix[i] = GLES20.glGetUniformLocation(extrusionProgram[i], "u_mvp");
				hExtrusionColor[i] = GLES20.glGetUniformLocation(extrusionProgram[i], "u_color");
				hExtrusionAlpha[i] = GLES20.glGetUniformLocation(extrusionProgram[i], "u_alpha");
				hExtrusionMode[i] = GLES20.glGetUniformLocation(extrusionProgram[i], "u_mode");
				hExtrusionVertexPosition[i] = GLES20.glGetAttribLocation(extrusionProgram[i],
						"a_pos");
				hExtrusionLightPosition[i] = GLES20.glGetAttribLocation(extrusionProgram[i],
						"a_light");
			}

			ByteBuffer buf = ByteBuffer.allocateDirect(BUFFERSIZE)
					.order(ByteOrder.nativeOrder());

			mShortBuffer = buf.asShortBuffer();
		}

		int ready = 0;
		mTileSet = mMapView.getTileManager().getActiveTiles(mTileSet);
		MapTile[] tiles = mTileSet.tiles;
		// FIXME just release tiles in this case
		if (mAlpha == 0 || curPos.zoomLevel < 16) {
			isReady = false;
			return;
		}

		// keep a list of tiles available for rendering
		if (mTiles == null || mTiles.length < mTileSet.cnt * 4)
			mTiles = new MapTile[mTileSet.cnt * 4];

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
	public void render(MapPosition pos, float[] mv, float[] proj) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster.

		Matrix.multiplyMM(mVPMatrix, 0, proj, 0, pos.viewMatrix, 0);
		proj = mVPMatrix;

		MapTile[] tiles = mTiles;

		float div = FastMath.pow(tiles[0].zoomLevel - pos.zoomLevel);

		int shaderMode = 1;
		int uExtAlpha = hExtrusionAlpha[shaderMode];
		int uExtColor = hExtrusionColor[shaderMode];
		int uExtVertexPosition = hExtrusionVertexPosition[shaderMode];
		int uExtLightPosition = hExtrusionLightPosition[shaderMode];
		int uExtMatrix = hExtrusionMatrix[shaderMode];
		int uExtMode = hExtrusionMode[shaderMode];

		if (debug) {
			GLState.useProgram(extrusionProgram[shaderMode]);

			GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
			GLES20.glUniform1i(uExtMode, 0);
			GLES20.glUniform4fv(uExtColor, 4, mColor, 0);

			GLState.test(false, false);

			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

				setMatrix(pos, mv, proj, tiles[i], div, 0);
				GLES20.glUniformMatrix4fv(uExtMatrix, 1, false, mv, 0);

				GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

				GLES20.glVertexAttribPointer(uExtVertexPosition, 3,
						GLES20.GL_SHORT, false, 8, 0);

				GLES20.glVertexAttribPointer(uExtLightPosition, 2,
						GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

				GLES20.glDrawElements(GLES20.GL_TRIANGLES,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
						GLES20.GL_UNSIGNED_SHORT, 0);

				GLES20.glDrawElements(GLES20.GL_LINES, el.mIndiceCnt[3],
						GLES20.GL_UNSIGNED_SHORT,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

				// just a temporary reference!
				tiles[i] = null;
			}
			return;
		}

		GLES20.glDepthMask(true);
		//GLES20.glStencilMask(0xff);
		//GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_STENCIL_BUFFER_BIT);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		GLState.useProgram(extrusionProgram[shaderMode]);
		GLState.enableVertexArrays(uExtVertexPosition, -1);
		if (pos.scale < 2) {
			// chances are high that one moves through a building
			// with scale > 2 also draw back sides in this case.
			GLES20.glEnable(GLES20.GL_CULL_FACE);
			GLES20.glCullFace(GLES20.GL_FRONT);
		}
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glColorMask(false, false, false, false);
		GLES20.glUniform1i(uExtMode, 0);
		GLES20.glUniform4fv(uExtColor, 4, mColor, 0);
		GLES20.glUniform1f(uExtAlpha, mAlpha);

		// draw to depth buffer
		for (int i = 0; i < mTileCnt; i++) {
			MapTile t = tiles[i];
			ExtrusionLayer el = (ExtrusionLayer) t.layers.extrusionLayers;
			int d = GLRenderer.depthOffset(t) * 10;
			setMatrix(pos, mv, proj, t, div, d);
			GLES20.glUniformMatrix4fv(uExtMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(uExtVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
					GLES20.GL_UNSIGNED_SHORT, 0);
		}

		// enable color buffer, use depth mask
		GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
		GLES20.glColorMask(true, true, true, true);
		GLES20.glDepthMask(false);
		GLState.blend(true);

		for (int i = 0; i < mTileCnt; i++) {
			MapTile t = tiles[i];
			ExtrusionLayer el = (ExtrusionLayer) t.layers.extrusionLayers;

			GLES20.glDepthFunc(GLES20.GL_EQUAL);
			int d = GLRenderer.depthOffset(t) * 10;
			setMatrix(pos, mv, proj, t, div, d);
			GLES20.glUniformMatrix4fv(uExtMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(uExtVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glVertexAttribPointer(uExtLightPosition, 2,
					GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

			// draw roof
			GLES20.glUniform1i(uExtMode, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[2],
					GLES20.GL_UNSIGNED_SHORT, (el.mIndiceCnt[0] + el.mIndiceCnt[1]) * 2);

			// draw sides 1
			GLES20.glUniform1i(uExtMode, 1);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[0],
					GLES20.GL_UNSIGNED_SHORT, 0);

			// draw sides 2
			GLES20.glUniform1i(uExtMode, 2);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[1],
					GLES20.GL_UNSIGNED_SHORT, el.mIndiceCnt[0] * 2);

			// drawing gl_lines with the same coordinates does not result in
			// same depth values as polygons, so add offset and draw gl_lequal:
			GLES20.glDepthFunc(GLES20.GL_LEQUAL);
			GlUtils.addOffsetM(mv, 100);
			GLES20.glUniformMatrix4fv(uExtMatrix, 1, false, mv, 0);

			GLES20.glUniform1i(uExtMode, 3);
			GLES20.glDrawElements(GLES20.GL_LINES, el.mIndiceCnt[3],
					GLES20.GL_UNSIGNED_SHORT,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

			// just a temporary reference!
			tiles[i] = null;
		}

		if (pos.scale < 2)
			GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private static void setMatrix(MapPosition mapPosition, float[] matrix, float[] proj,
			MapTile tile, float div, int delta) {

		float x = (float) (tile.pixelX - mapPosition.x * div);
		float y = (float) (tile.pixelY - mapPosition.y * div);
		float scale = mapPosition.scale / div;

		GlUtils.setTileMatrix(matrix, x, y, scale);
		// scale height
		matrix[10] = scale / (1000f * GLRenderer.COORD_MULTIPLIER);

		Matrix.multiplyMM(matrix, 0, proj, 0, matrix, 0);

		GlUtils.addOffsetM(matrix, delta);
	}

	private final float _a = 0.86f;
	private final float _r = 0xe9;
	private final float _g = 0xe8;
	private final float _b = 0xe6;
	private final float _o = 55;
	private final float _s = 20;
	private final float _l = 14;
	private float mAlpha = 1;
	private final float[] mColor = {
			// roof color
			_a * ((_r + _l + 1) / 255),
			_a * ((_g + _l + 1) / 255),
			_a * ((_b + _l) / 255),
			_a,
			// sligthly differ adjacent side
			// faces to improve contrast
			_a * ((_r - _s) / 255 + 0.01f),
			_a * ((_g - _s) / 255 + 0.01f),
			_a * ((_b - _s) / 255),
			_a,
			_a * ((_r - _s) / 255),
			_a * ((_g - _s) / 255),
			_a * ((_b - _s) / 255),
			_a,
			// roof outline
			(_r - _o) / 255,
			(_g - _o) / 255,
			(_b - _o) / 255,
			//			1, 0, 0,
			1.0f,
	};

	final static String[] extrusionVertexShader = {
			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
					+ "uniform vec4 u_color[4];"
					+ "uniform int u_mode;"
					+ "uniform float u_alpha;"
					+ "attribute vec4 a_pos;"
					+ "attribute vec2 a_light;"
					+ "varying vec4 color;"
					+ "const float ff = 255.0;"
					+ "float c_alpha = 0.8;"
					+ "void main() {"
					+ "  gl_Position = u_mvp * a_pos;"
					+ "  if (u_mode == 0)"
					//     roof / depth pass
					+ "    color = u_color[0];"
					+ "  else {"
					//    decrease contrast with distance
					+ "   float z = (0.96 + gl_Position.z * 0.04);"
					+ "   if (u_mode == 1){"
					//     sides 1 - use 0xff00
					//     scale direction to -0.5<>0.5
					+ "    float dir = abs(a_light.y / ff - 0.5);"
					+ "    color = u_color[1] * z;"
					+ "    color.rgb *= (0.7 + dir * 0.4);"
					+ "  } else if (u_mode == 2){"
					//     sides 2 - use 0x00ff
					+ "    float dir = abs(a_light.x / ff - 0.5);"
					+ "    color = u_color[2] * z;"
					+ "    color.rgb *= (0.7 + dir * 0.4);"
					+ "  } else"
					//     outline
					+ "    color = u_color[3] * z;"
					+ "}}",

			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
					+ "uniform vec4 u_color[4];"
					+ "uniform int u_mode;"
					+ "uniform float u_alpha;"
					+ "attribute vec4 a_pos;"
					+ "attribute vec2 a_light;"
					+ "varying vec4 color;"
					//+ "varying float z;"
					+ "const float ff = 255.0;"
					+ "void main() {"
					//   change height by u_alpha
					+ "  gl_Position = u_mvp * vec4(a_pos.xy, a_pos.z * u_alpha, 1.0);"
					//+ "  z = gl_Position.z;"
					+ "  if (u_mode == 0)"
					//     roof / depth pass
					+ "    color = u_color[0];"
					+ "  else {"
					//    decrease contrast with distance
					+ "   if (u_mode == 1){"
					//     sides 1 - use 0xff00
					//     scale direction to -0.5<>0.5
					//+ "    float dir = abs(a_light.y / ff - 0.5);"
					+ "    float dir = a_light.y / ff;"
					+ "    float z = (0.98 + gl_Position.z * 0.02);"
					+ "    color = u_color[1];"
					+ "    color.rgb *= (0.88 + dir * 0.12) * z;"
					+ "  } else if (u_mode == 2){"
					//     sides 2 - use 0x00ff
					//+ "    float dir = abs(a_light.x / ff - 0.5);"
					+ "    float dir = a_light.x / ff;"
					+ "    float z = (0.98 + gl_Position.z * 0.02);"
					+ "    color = u_color[2] * z;"
					+ "    color.rgb *= (0.88 + dir * 0.12) * z;"
					+ "  } else {"
					//     outline
					+ "    float z = (0.8 - gl_Position.z * 0.2);"
					+ "    color = u_color[3] * z;"
					+ "}}}"
	};

	final static String extrusionFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";

	final static String extrusionFragmentShaderZ = ""
			+ "precision highp float;"
			+ "uniform vec4 u_color;"
			+ "varying float z;"
			+ "void main() {"
			+ "if (z < 0.0)"
			+ "  gl_FragColor = vec4(z * -1.0, 0.0, 0.0, 1.0);"
			+ "else"
			+ "  gl_FragColor = vec4(0.0, 0.0, z, 1.0);"
			+ "}";

	public void setAlpha(float a) {
		mAlpha = a;
	}

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}
}
