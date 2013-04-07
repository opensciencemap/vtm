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
import java.nio.ShortBuffer;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.generator.JobTile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.ExtrusionLayer;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class ExtrusionOverlay extends RenderOverlay {
	private final static String TAG = ExtrusionOverlay.class.getName();

	public ExtrusionOverlay(MapView mapView) {
		super(mapView);
	}

	private static int[] shaderProgram = new int[2];
	private static int[] hVertexPosition = new int[2];
	private static int[] hLightPosition = new int[2];
	private static int[] hMatrix = new int[2];
	private static int[] hColor = new int[2];
	private static int[] hAlpha = new int[2];
	private static int[] hMode = new int[2];

	private boolean initialized = false;

	// FIXME sum up size used while filling layer only up to:
	public int mBufferSize = 65536;
	private TileSet mTileSet;
	private ShortBuffer mShortBuffer;
	private MapTile[] mTiles;
	private int mTileCnt;

	@Override
	public void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged, Matrices matrices) {

		mMapView.getMapViewPosition().getMapPosition(mMapPosition);

		if (!initialized) {
			initialized = true;

			for (int i = 1; i < 2; i++) {
				// Set up the program for rendering extrusions
				shaderProgram[i] = GlUtils.createProgram(extrusionVertexShader,
						extrusionFragmentShader);
				if (shaderProgram[i] == 0) {
					Log.e(TAG, "Could not create extrusion shader program. " + i);
					return;
				}
				hMatrix[i] = GLES20.glGetUniformLocation(shaderProgram[i], "u_mvp");
				hColor[i] = GLES20.glGetUniformLocation(shaderProgram[i], "u_color");
				hAlpha[i] = GLES20.glGetUniformLocation(shaderProgram[i], "u_alpha");
				hMode[i] = GLES20.glGetUniformLocation(shaderProgram[i], "u_mode");
				hVertexPosition[i] = GLES20.glGetAttribLocation(shaderProgram[i], "a_pos");
				hLightPosition[i] = GLES20.glGetAttribLocation(shaderProgram[i], "a_light");
			}

			ByteBuffer buf = ByteBuffer.allocateDirect(mBufferSize)
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
					int verticesBytes = el.mNumVertices * 8 * 2;
					if (verticesBytes > mBufferSize) {
						mBufferSize = verticesBytes;
						Log.d(TAG, "realloc extrusion buffer " + verticesBytes);
						ByteBuffer buf = ByteBuffer.allocateDirect(verticesBytes)
								.order(ByteOrder.nativeOrder());

						mShortBuffer = buf.asShortBuffer();
					}
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

	private final boolean debug = false;

	//private final float[] mVPMatrix = new float[16];

	@Override
	public void render(MapPosition pos, Matrices m) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster.

		MapTile[] tiles = mTiles;

		//float div = FastMath.pow(tiles[0].zoomLevel - pos.zoomLevel);

		int shaderMode = 1;
		int uExtAlpha = hAlpha[shaderMode];
		int uExtColor = hColor[shaderMode];
		int uExtVertexPosition = hVertexPosition[shaderMode];
		int uExtLightPosition = hLightPosition[shaderMode];
		int uExtMatrix = hMatrix[shaderMode];
		int uExtMode = hMode[shaderMode];

		if (debug) {
			GLState.useProgram(shaderProgram[shaderMode]);

			GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
			GLES20.glUniform1i(uExtMode, 0);
			GLES20.glUniform4fv(uExtColor, 4, mColor, 0);

			GLState.test(false, false);

			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

				setMatrix(pos, m, tiles[i], 0);
				m.mvp.setAsUniform(uExtMatrix);

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

		GLState.useProgram(shaderProgram[shaderMode]);
		GLState.enableVertexArrays(uExtVertexPosition, -1);
		if (pos.scale < (1 << 18)) {
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
			setMatrix(pos, m, t, d);
			m.mvp.setAsUniform(uExtMatrix);

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
			setMatrix(pos, m, t, d);
			m.mvp.setAsUniform(uExtMatrix);

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

			m.mvp.addDepthOffset(100);
			m.mvp.setAsUniform(uExtMatrix);

			GLES20.glUniform1i(uExtMode, 3);
			GLES20.glDrawElements(GLES20.GL_LINES, el.mIndiceCnt[3],
					GLES20.GL_UNSIGNED_SHORT,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

			// just a temporary reference!
			tiles[i] = null;
		}

		if (pos.scale < (1 << 18))
			GLES20.glDisable(GLES20.GL_CULL_FACE);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private static void setMatrix(MapPosition pos, Matrices m,
			MapTile tile, int delta) {

		int z = tile.zoomLevel;
		double curScale = Tile.TILE_SIZE * pos.scale;
		float scale = (float)(pos.scale / (1 << z));

		float x = (float) ((tile.x - pos.x) * curScale);
		float y = (float) ((tile.y - pos.y) * curScale);
		m.mvp.setTransScale(x, y, scale / GLRenderer.COORD_SCALE);

//		float x = (float) (tile.pixelX - mapPosition.x * div);
//		float y = (float) (tile.pixelY - mapPosition.y * div);
//		float scale = mapPosition.scale / div;
//
//		m.mvp.setTransScale(x * scale, y * scale, scale / GLRenderer.COORD_SCALE);

		// scale height
		m.mvp.setValue(10, scale / 10);

		m.mvp.multiplyMM(m.viewproj, m.mvp);

		m.mvp.addDepthOffset(delta);
	}

	private final float _a = 0.86f;
	private final float _r = 0xe9;
	private final float _g = 0xe8;
	private final float _b = 0xe6;
	private final float _o = 55;
	private final float _s = 25;
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

	final static String extrusionVertexShader = ""
			+ "precision mediump float;"
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
			+ "    color.rgb *= (0.85 + dir * 0.15) * z;"
			+ "  } else if (u_mode == 2){"
			//     sides 2 - use 0x00ff
			//+ "    float dir = abs(a_light.x / ff - 0.5);"
			+ "    float dir = a_light.x / ff;"
			+ "    float z = (0.98 + gl_Position.z * 0.02);"
			+ "    color = u_color[2] * z;"
			+ "    color.rgb *= (0.85 + dir * 0.15) * z;"
			+ "  } else {"
			//     outline
			+ "    float z = (0.8 - gl_Position.z * 0.2);"
			+ "    color = u_color[3] * z;"
			+ "}}}";

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
