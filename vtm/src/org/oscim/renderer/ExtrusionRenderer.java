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
package org.oscim.renderer;

import org.oscim.backend.GL20;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.Log;
import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileRenderer;
import org.oscim.tiling.TileSet;
import org.oscim.utils.GlUtils;

// TODO move MapTile part to BuildingLayer and make
// this class work on ExtrusionLayers

public class ExtrusionRenderer extends LayerRenderer {
	private final static String TAG = ExtrusionRenderer.class.getName();

	private static final GL20 GL = GLAdapter.get();

	private final TileRenderer mTileLayer;

	protected float mAlpha = 1;

	public ExtrusionRenderer(TileRenderer tileRenderLayer) {
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
	}

	private static int[] shaderProgram = new int[2];
	private static int[] hVertexPosition = new int[2];
	private static int[] hLightPosition = new int[2];
	private static int[] hMatrix = new int[2];
	private static int[] hColor = new int[2];
	private static int[] hAlpha = new int[2];
	private static int[] hMode = new int[2];

	private boolean initialized = false;

	private final TileSet mTileSet;
	private MapTile[] mTiles;
	private int mTileCnt;

	private final static int SHADER = 0;

	private boolean initShader() {
		initialized = true;

		for (int i = 0; i <= SHADER; i++) {
			if (i == 0) {
				shaderProgram[i] = GlUtils.createProgram(extrusionVertexShader,
						extrusionFragmentShader);
			} else {
				shaderProgram[i] = GlUtils.createProgram(extrusionVertexShader,
						extrusionFragmentShaderZ);
			}

			if (shaderProgram[i] == 0) {
				Log.e(TAG, "Could not create extrusion shader program. " + i);
				return false;
			}

			hMatrix[i] = GL.glGetUniformLocation(shaderProgram[i], "u_mvp");
			hColor[i] = GL.glGetUniformLocation(shaderProgram[i], "u_color");
			hAlpha[i] = GL.glGetUniformLocation(shaderProgram[i], "u_alpha");
			hMode[i] = GL.glGetUniformLocation(shaderProgram[i], "u_mode");
			hVertexPosition[i] = GL.glGetAttribLocation(shaderProgram[i], "a_pos");
			hLightPosition[i] = GL.glGetAttribLocation(shaderProgram[i], "a_light");
		}

		return true;
	}

	@Override
	protected void update(MapPosition pos, boolean changed, Matrices matrices) {
		mMapPosition.copy(pos);

		if (!initialized && !initShader())
			return;

		if (shaderProgram[0] == 0)
			return;

		if (mAlpha == 0 || pos.zoomLevel < 16) {
			setReady(false);
			return;
		}

		int activeTiles = 0;
		mTileLayer.getVisibleTiles(mTileSet);
		MapTile[] tiles = mTileSet.tiles;

		if (mTileSet.cnt == 0) {
			mTileLayer.releaseTiles(mTileSet);
			setReady(false);
			return;
		}

		// keep a list of tiles available for rendering
		if (mTiles == null || mTiles.length < mTileSet.cnt * 4)
			mTiles = new MapTile[mTileSet.cnt * 4];

		int zoom = tiles[0].zoomLevel;

		ExtrusionLayer el;
		if (zoom == 17) {
			for (int i = 0; i < mTileSet.cnt; i++) {
				el = getLayer(tiles[i]);
				if (el == null)
					continue;

				if (!el.compiled) {
					int numShorts = el.mNumVertices * 8;
					el.compile(MapRenderer.getShortBuffer(numShorts));
					GlUtils.checkGlError("...");
				}

				if (el.compiled)
					mTiles[activeTiles++] = tiles[i];
			}
		} else if (zoom == 16) {
			// check if proxy children are ready
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i];
				for (byte j = 0; j < 4; j++) {
					if ((t.proxies & (1 << j)) == 0)
						continue;

					MapTile c = t.rel.get(j);
					el = getLayer(c);

					if (el == null || !el.compiled)
						continue;

					mTiles[activeTiles++] = c;
				}
			}
		}

		mTileCnt = activeTiles;

		if (activeTiles > 0)
			setReady(true);
		else
			mTileLayer.releaseTiles(mTileSet);
	}

	@Override
	protected void compile() {

	}

	private static ExtrusionLayer getLayer(MapTile t) {
		if (t.layers != null && t.layers.extrusionLayers != null
				&& t.state == MapTile.STATE_READY)
			return (ExtrusionLayer) t.layers.extrusionLayers;
		return null;
	}

	private final boolean debug = false;

	//private final float[] mVPMatrix = new float[16];

	@Override
	protected void render(MapPosition pos, Matrices m) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster and would allow postprocessing outlines.

		MapTile[] tiles = mTiles;

		int uExtAlpha = hAlpha[SHADER];
		int uExtColor = hColor[SHADER];
		int uExtVertexPosition = hVertexPosition[SHADER];
		int uExtLightPosition = hLightPosition[SHADER];
		int uExtMatrix = hMatrix[SHADER];
		int uExtMode = hMode[SHADER];

		if (debug) {
			GLState.useProgram(shaderProgram[SHADER]);

			GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
			GL.glUniform1i(uExtMode, 0);
			GlUtils.glUniform4fv(uExtColor, 4, mColor);

			GLState.test(false, false);

			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

				setMatrix(pos, m, tiles[i], 0);
				m.mvp.setAsUniform(uExtMatrix);

				el.vboIndices.bind();
				el.vboVertices.bind();

				GL.glVertexAttribPointer(uExtVertexPosition, 3,
						GL20.GL_SHORT, false, 8, 0);

				GL.glVertexAttribPointer(uExtLightPosition, 2,
						GL20.GL_UNSIGNED_BYTE, false, 8, 6);

				GL.glDrawElements(GL20.GL_TRIANGLES,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
						GL20.GL_UNSIGNED_SHORT, 0);

				GL.glDrawElements(GL20.GL_LINES, el.mIndiceCnt[3],
						GL20.GL_UNSIGNED_SHORT,
						(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

				// just a temporary reference!
				tiles[i] = null;
			}
			return;
		}

		GL.glDepthMask(true);
		//GL.glStencilMask(0xff);
		//GL.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		GLState.useProgram(shaderProgram[SHADER]);
		GLState.enableVertexArrays(uExtVertexPosition, -1);
		if (pos.scale < (1 << 18)) {
			// chances are high that one moves through a building
			// with scale > 2 also draw back sides in this case.
			GL.glEnable(GL20.GL_CULL_FACE);
			GL.glCullFace(GL20.GL_FRONT);
		}
		GL.glDepthFunc(GL20.GL_LESS);
		GL.glColorMask(false, false, false, false);
		GL.glUniform1i(uExtMode, 0);
		GlUtils.glUniform4fv(uExtColor, 4, mColor);
		GL.glUniform1f(uExtAlpha, mAlpha);

		// draw to depth buffer
		for (int i = 0; i < mTileCnt; i++) {
			MapTile t = tiles[i];
			ExtrusionLayer el = (ExtrusionLayer) t.layers.extrusionLayers;
			int d = MapRenderer.depthOffset(t) * 10;
			setMatrix(pos, m, t, d);
			m.mvp.setAsUniform(uExtMatrix);

			el.vboIndices.bind();
			el.vboVertices.bind();

			GL.glVertexAttribPointer(uExtVertexPosition, 3,
					GL20.GL_SHORT, false, 8, 0);

			GL.glDrawElements(GL20.GL_TRIANGLES,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]),
					GL20.GL_UNSIGNED_SHORT, 0);
		}

		// enable color buffer, use depth mask
		GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
		GL.glColorMask(true, true, true, true);
		GL.glDepthMask(false);
		GLState.blend(true);

		for (int i = 0; i < mTileCnt; i++) {
			MapTile t = tiles[i];
			ExtrusionLayer el = (ExtrusionLayer) t.layers.extrusionLayers;

			GL.glDepthFunc(GL20.GL_EQUAL);
			int d = MapRenderer.depthOffset(t) * 10;
			setMatrix(pos, m, t, d);
			m.mvp.setAsUniform(uExtMatrix);

			el.vboIndices.bind();
			el.vboVertices.bind();

			GL.glVertexAttribPointer(uExtVertexPosition, 3,
					GL20.GL_SHORT, false, 8, 0);

			GL.glVertexAttribPointer(uExtLightPosition, 2,
					GL20.GL_UNSIGNED_BYTE, false, 8, 6);

			// draw roof
			GL.glUniform1i(uExtMode, 0);
			GL.glDrawElements(GL20.GL_TRIANGLES, el.mIndiceCnt[2],
					GL20.GL_UNSIGNED_SHORT, (el.mIndiceCnt[0] + el.mIndiceCnt[1]) * 2);

			// draw sides 1
			GL.glUniform1i(uExtMode, 1);
			GL.glDrawElements(GL20.GL_TRIANGLES, el.mIndiceCnt[0],
					GL20.GL_UNSIGNED_SHORT, 0);

			// draw sides 2
			GL.glUniform1i(uExtMode, 2);
			GL.glDrawElements(GL20.GL_TRIANGLES, el.mIndiceCnt[1],
					GL20.GL_UNSIGNED_SHORT, el.mIndiceCnt[0] * 2);

			// drawing gl_lines with the same coordinates does not result in
			// same depth values as polygons, so add offset and draw gl_lequal:
			GL.glDepthFunc(GL20.GL_LEQUAL);

			m.mvp.addDepthOffset(100);
			m.mvp.setAsUniform(uExtMatrix);

			GL.glUniform1i(uExtMode, 3);
			GL.glDrawElements(GL20.GL_LINES, el.mIndiceCnt[3],
					GL20.GL_UNSIGNED_SHORT,
					(el.mIndiceCnt[0] + el.mIndiceCnt[1] + el.mIndiceCnt[2]) * 2);

			// just a temporary reference!
			tiles[i] = null;
		}

		if (pos.scale < (1 << 18))
			GL.glDisable(GL20.GL_CULL_FACE);

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mTileLayer.releaseTiles(mTileSet);
	}

	private static void setMatrix(MapPosition pos, Matrices m,
			MapTile tile, int delta) {

		int z = tile.zoomLevel;
		double curScale = Tile.SIZE * pos.scale;
		float scale = (float) (pos.scale / (1 << z));

		float x = (float) ((tile.x - pos.x) * curScale);
		float y = (float) ((tile.y - pos.y) * curScale);
		m.mvp.setTransScale(x, y, scale / MapRenderer.COORD_SCALE);

		// scale height
		m.mvp.setValue(10, scale / 10);

		m.mvp.multiplyLhs(m.viewproj);

		m.mvp.addDepthOffset(delta);
	}

	private final float _a = 0.88f;
	private final float _r = 0xe9;
	private final float _g = 0xe8;
	private final float _b = 0xe6;
	private final float _o = 20;
	private final float _s = 4;
	private final float _l = 0;
	private final float[] mColor = {
			// roof color
			_a * ((_r + _l) / 255),
			_a * ((_g + _l) / 255),
			_a * ((_b + _l) / 255),
			0.8f,
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
			0.9f,
	};

	final static String extrusionVertexShader = ""
			//+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color[4];"
			+ "uniform int u_mode;"
			+ "uniform float u_alpha;"
			+ "attribute vec4 a_pos;"
			+ "attribute vec2 a_light;"
			+ "varying vec4 color;"
			+ "varying float depth;"
			+ "const float ff = 255.0;"
			+ "void main() {"
			//   change height by u_alpha
			+ "  gl_Position = u_mvp * vec4(a_pos.xy, a_pos.z * u_alpha, 1.0);"
			//+ "  depth = gl_Position.z;"
			+ "  if (u_mode == 0)"
			//     roof / depth pass
			+ "    color = u_color[0] * u_alpha;"
			+ "  else {"
			//    decrease contrast with distance
			+ "   if (u_mode == 1){"
			//     sides 1 - use 0xff00
			//     scale direction to -0.5<>0.5
			+ "    float dir = a_light.y / ff;"
			+ "    float z = (0.98 + gl_Position.z * 0.02);"
			+ "    float h = 0.9 + clamp(a_pos.z / 2000.0, 0.0, 0.1);"
			+ "    color = u_color[1];"
			+ "    color.rgb *= (0.8 + dir * 0.2) * z * h;"
			+ "    color *= u_alpha;"
			+ "  } else if (u_mode == 2){"
			//     sides 2 - use 0x00ff
			+ "    float dir = a_light.x / ff;"
			+ "    float z = (0.98 + gl_Position.z * 0.02);"
			+ "    float h = 0.9 + clamp(a_pos.z / 2000.0, 0.0, 0.1);"
			+ "    color = u_color[2];"
			+ "    color.rgb *= (0.8 + dir * 0.2) * z * h;"
			+ "    color *= u_alpha;"
			+ "  } else {"
			//     outline
			+ "    float z = (0.98 - gl_Position.z * 0.02);"
			+ "    color = u_color[3] * z;"
			+ "}}}";

	final static String extrusionFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";

	final static String extrusionFragmentShaderZ = ""
			+ "precision mediump float;"
			+ "varying float depth;"
			+ "void main() {"
			+ "float d = depth * 0.2;"
			+ "if (d < 0.0)"
			+ "   d = -d;"
			+ "  gl_FragColor = vec4(1.0 - d, 1.0 - d, 1.0 - d, 1.0 - d);"
			+ "}";
}
