/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.renderer;

import static org.oscim.layers.tile.MapTile.State.NEW_DATA;
import static org.oscim.layers.tile.MapTile.State.READY;

import java.nio.ShortBuffer;

import org.oscim.backend.GL20;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO move MapTile part to BuildingLayer and make
// this class work on ExtrusionLayers

public class ExtrusionRenderer extends LayerRenderer {
	static final Logger log = LoggerFactory.getLogger(ExtrusionRenderer.class);

	private final TileRenderer mTileLayer;
	private final int mTileZoom;
	private final boolean drawAlpha;

	protected float mAlpha = 1;

	public ExtrusionRenderer(TileRenderer tileRenderLayer, int tileZoom) {
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
		mTileZoom = tileZoom;
		mMode = 0;
		drawAlpha = true;
	}

	public ExtrusionRenderer(TileRenderer tileRenderLayer, int tileZoom, boolean mesh, boolean alpha) {
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
		mTileZoom = tileZoom;
		mMode = mesh ? 1 : 0;
		drawAlpha = false; //alpha;
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

	private final int mMode;

	private boolean initShader() {
		initialized = true;

		for (int i = 0; i < 2; i++) {
			if (i == 0) {
				shaderProgram[i] = GLShader.createProgram(extrusionVertexShader,
				                                          extrusionFragmentShader);
			} else {
				shaderProgram[i] = GLShader.createProgram(extrusionVertexShader2,
				                                          extrusionFragmentShader);
			}

			if (shaderProgram[i] == 0) {
				log.error("Could not create extrusion shader program. " + i);
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
	public void update(GLViewport v) {

		if (!initialized && !initShader())
			return;

		if (shaderProgram[0] == 0)
			return;

		if (mAlpha == 0 || v.pos.zoomLevel < mTileZoom) {
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
		if (zoom == mTileZoom) {
			for (int i = 0; i < mTileSet.cnt; i++) {
				if (compileLayers(getLayer(tiles[i])))
					mTiles[activeTiles++] = tiles[i];
			}
		} else if (zoom == mTileZoom + 1) {
			O: for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i].node.parent();

				if (t == null)
					continue;

				for (MapTile c : mTiles)
					if (c == t)
						continue O;

				el = getLayer(t);
				if (el == null)
					continue;

				if (compileLayers(el))
					mTiles[activeTiles++] = t;
			}
		} else if (zoom == mTileZoom - 1) {
			// check if proxy children are ready
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i];
				for (byte j = 0; j < 4; j++) {
					if (!t.hasProxy(1 << j))
						continue;

					MapTile c = t.node.child(j);
					el = getLayer(c);

					if (el == null || !el.compiled)
						continue;

					mTiles[activeTiles++] = c;
				}
			}
		}

		mTileCnt = activeTiles;
		//log.debug("" + activeTiles + " " + zoom);

		if (activeTiles > 0)
			setReady(true);
		else
			mTileLayer.releaseTiles(mTileSet);
	}

	private boolean compileLayers(ExtrusionLayer el) {
		if (el == null)
			return false;

		if (el.compiled)
			return true;

		int sumIndices = 0;
		int sumVertices = 0;
		for (ExtrusionLayer l = el; l != null; l = l.next()) {
			//if (l.sumIndices == 0){
			//	l.clear();
			//}
			sumIndices += l.sumIndices;
			sumVertices += l.sumVertices;
		}
		if (sumIndices == 0) {
			return false;
		}
		ShortBuffer vbuf = MapRenderer.getShortBuffer(sumVertices * 4);
		ShortBuffer ibuf = MapRenderer.getShortBuffer(sumIndices);

		for (ExtrusionLayer l = el; l != null; l = l.next())
			l.compile(vbuf, ibuf);

		int size = sumIndices * 2;
		if (ibuf.position() != sumIndices) {
			int pos = ibuf.position();
			log.error("invalid indice size: {} {}", sumIndices, pos);
			size = pos * 2;
		}
		el.vboIndices = BufferObject.get(GL20.GL_ELEMENT_ARRAY_BUFFER, size);
		el.vboIndices.loadBufferData(ibuf.flip(), size);
		el.vboIndices.unbind();

		//GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		size = sumVertices * 4 * 2;
		if (vbuf.position() != sumVertices * 4) {
			int pos = vbuf.position();
			log.error("invalid vertex size: {} {}", sumVertices, pos);
			size = pos * 2;
		}

		el.vboVertices = BufferObject.get(GL20.GL_ARRAY_BUFFER, size);
		el.vboVertices.loadBufferData(vbuf.flip(), size);
		el.vboVertices.unbind();

		//GL.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		GLUtils.checkGlError("compile extrusion layer");
		return true;
	}

	private static ExtrusionLayer getLayer(MapTile t) {
		ElementLayers layers = t.getLayers();
		if (layers == null || !t.state(READY | NEW_DATA))
			return null;

		return layers.getExtrusionLayers();
	}

	private final boolean debug = false;

	@Override
	public void render(GLViewport v) {
	}

	private void renderCombined(int vertexPointer, ExtrusionLayer el) {
		for (; el != null; el = el.next()) {

			if (el.vboIndices == null)
				continue;

			el.vboIndices.bind();
			el.vboVertices.bind();

			GL.glVertexAttribPointer(vertexPointer, 3,
			                         GL20.GL_SHORT, false, 8, 0);

			int sumIndices = el.numIndices[0] + el.numIndices[1] + el.numIndices[2];
			if (sumIndices > 0)
				GL.glDrawElements(GL20.GL_TRIANGLES, sumIndices,
				                  GL20.GL_UNSIGNED_SHORT, 0);

			if (el.numIndices[2] > 0) {
				int offset = sumIndices * 2;
				GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[4],
				                  GL20.GL_UNSIGNED_SHORT, offset);
			}
		}
	}

	public void render2(GLViewport v) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster and would allow postprocessing outlines.

		MapTile[] tiles = mTiles;

		int uExtAlpha = hAlpha[mMode];
		int uExtColor = hColor[mMode];
		int uExtVertexPosition = hVertexPosition[mMode];
		int uExtLightPosition = hLightPosition[mMode];
		int uExtMatrix = hMatrix[mMode];
		int uExtMode = hMode[mMode];

		if (debug) {
			GLState.useProgram(shaderProgram[mMode]);

			GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);
			GL.glUniform1i(uExtMode, 0);
			GLUtils.glUniform4fv(uExtColor, 4, DEBUG_COLOR);
			GL.glUniform1f(uExtAlpha, 1);

			GLState.test(false, false);
			GLState.blend(true);
			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = tiles[i].getLayers().getExtrusionLayers();

				setMatrix(v, tiles[i], 0);
				v.mvp.setAsUniform(uExtMatrix);

				renderCombined(uExtVertexPosition, el);

				// just a temporary reference!
				tiles[i] = null;
			}
			return;
		}

		GL.glDepthMask(true);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		GLState.useProgram(shaderProgram[mMode]);
		GLState.enableVertexArrays(uExtVertexPosition, -1);
		GLState.blend(false);

		GL.glEnable(GL20.GL_CULL_FACE);
		GL.glDepthFunc(GL20.GL_LESS);

		//GL.glUniform1f(uExtAlpha, mAlpha);
		GL.glUniform1f(uExtAlpha, 1);

		if (drawAlpha) {
			GL.glColorMask(false, false, false, false);
			GL.glUniform1i(uExtMode, -1);
			//GLUtils.glUniform4fv(uExtColor, 4, mColor);

			// draw to depth buffer
			for (int i = 0; i < mTileCnt; i++) {
				MapTile t = tiles[i];
				ExtrusionLayer el = t.getLayers().getExtrusionLayers();
				if (el == null)
					continue;

				int d = MapTile.depthOffset(t) * 10;

				setMatrix(v, t, d);
				v.mvp.setAsUniform(uExtMatrix);

				renderCombined(uExtVertexPosition, el);
			}

			GL.glColorMask(true, true, true, true);
			GL.glDepthMask(false);
			GLState.blend(true);
		}

		//GLState.blend(false);
		//GLState.blend(true);
		//GL.glEnable(GL20.GL_BLEND);

		GLState.blend(true);

		GLState.enableVertexArrays(uExtVertexPosition, uExtLightPosition);

		float[] currentColor = null;

		for (int i = 0; i < mTileCnt; i++) {
			MapTile t = tiles[i];
			ExtrusionLayer el = t.getLayers().getExtrusionLayers();

			if (el == null)
				continue;

			if (el.vboIndices == null)
				continue;

			int d = 1;
			if (drawAlpha) {
				GL.glDepthFunc(GL20.GL_EQUAL);
				d = MapTile.depthOffset(t) * 10;
			}

			setMatrix(v, t, d);
			v.mvp.setAsUniform(uExtMatrix);

			el.vboIndices.bind();
			el.vboVertices.bind();

			for (; el != null; el = el.next()) {

				if (el.colors != currentColor) {
					currentColor = el.colors;
					GLUtils.glUniform4fv(uExtColor, mMode == 0 ? 4 : 1,
					                     el.colors);
				}

				/* indices offset */
				int indexOffset = el.indexOffset;
				/* vertex byte offset */
				int vertexOffset = el.getOffset();

				GL.glVertexAttribPointer(uExtVertexPosition, 3,
				                         GL20.GL_SHORT, false, 8, vertexOffset);

				GL.glVertexAttribPointer(uExtLightPosition, 2,
				                         GL20.GL_UNSIGNED_BYTE, false, 8, vertexOffset + 6);

				/* draw extruded outlines */
				if (el.numIndices[0] > 0) {
					/* draw roof */
					GL.glUniform1i(uExtMode, 0);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[2],
					                  GL20.GL_UNSIGNED_SHORT,
					                  (el.numIndices[0] + el.numIndices[1]) * 2);

					/* draw sides 1 */
					GL.glUniform1i(uExtMode, 1);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[0],
					                  GL20.GL_UNSIGNED_SHORT, 0);

					/* draw sides 2 */
					GL.glUniform1i(uExtMode, 2);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[1],
					                  GL20.GL_UNSIGNED_SHORT, el.numIndices[0] * 2);

					if (drawAlpha) {
						/* drawing gl_lines with the same coordinates does not
						 * result in same depth values as polygons, so add
						 * offset and draw gl_lequal: */
						GL.glDepthFunc(GL20.GL_LEQUAL);
					}

					v.mvp.addDepthOffset(100);
					v.mvp.setAsUniform(uExtMatrix);

					GL.glUniform1i(uExtMode, 3);

					int offset = 2 * (indexOffset
					        + el.numIndices[0]
					        + el.numIndices[1]
					        + el.numIndices[2]);

					GL.glDrawElements(GL20.GL_LINES, el.numIndices[3],
					                  GL20.GL_UNSIGNED_SHORT, offset);
				}

				/* draw triangle meshes */
				if (el.numIndices[4] > 0) {
					int offset = 2 * (indexOffset
					        + el.numIndices[0]
					        + el.numIndices[1]
					        + el.numIndices[2]
					        + el.numIndices[3]);

					GL.glUniform1i(uExtMode, 4);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[4],
					                  GL20.GL_UNSIGNED_SHORT, offset);
				}
			}
			// just a temporary reference!
			tiles[i] = null;
		}

		GL.glDepthMask(false);
		GL.glDisable(GL20.GL_CULL_FACE);

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mTileLayer.releaseTiles(mTileSet);
	}

	private static void setMatrix(GLViewport v, MapTile tile, int delta) {
		int z = tile.zoomLevel;
		double curScale = Tile.SIZE * v.pos.scale;
		float scale = (float) (v.pos.scale / (1 << z));

		float x = (float) ((tile.x - v.pos.x) * curScale);
		float y = (float) ((tile.y - v.pos.y) * curScale);
		v.mvp.setTransScale(x, y, scale / MapRenderer.COORD_SCALE);

		// scale height ???
		v.mvp.setValue(10, scale / 10);

		v.mvp.multiplyLhs(v.viewproj);

		v.mvp.addDepthOffset(delta);
	}

	private static float A = 0.88f;
	private static float R = 0xe9;
	private static float G = 0xe8;
	private static float B = 0xe6;
	private static float O = 20;
	private static float S = 4;
	private static float L = 0;

	private static float[] DEBUG_COLOR = {
	        // roof color
	        A * ((R + L) / 255),
	        A * ((G + L) / 255),
	        A * ((B + L) / 255),
	        0.8f,
	        // sligthly differ adjacent side
	        // faces to improve contrast
	        A * ((R - S) / 255 + 0.01f),
	        A * ((G - S) / 255 + 0.01f),
	        A * ((B - S) / 255),
	        A,
	        A * ((R - S) / 255),
	        A * ((G - S) / 255),
	        A * ((B - S) / 255),
	        A,
	        // roof outline
	        (R - O) / 255,
	        (G - O) / 255,
	        (B - O) / 255,
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
	        + "  if (u_mode == -1){;"
	        //     roof / depth pass
	        //+ "    color = u_color[0] * u_alpha;"
	        + "  } else if (u_mode == 0){"
	        //     roof / depth pass
	        + "    color = u_color[0] * u_alpha;"
	        + "  } else if (u_mode == 1){"
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
	        + "  } else if (u_mode == 3) {"
	        //     outline
	        + "    float z = (0.98 - gl_Position.z * 0.02);"
	        + "    color = u_color[3] * z;"
	        + "}}";

	final static String extrusionVertexShader2 = ""
	        + "uniform mat4 u_mvp;"
	        + "uniform vec4 u_color;"
	        + "uniform float u_alpha;"
	        + "attribute vec4 a_pos;"
	        + "attribute vec2 a_light;"
	        + "varying vec4 color;"
	        + "varying float depth;"
	        + "const float alpha = 1.0;"

	        + "void main() {"
	        //   change height by u_alpha
	        + "  vec4 pos = a_pos;"
	        + "  pos.z *= u_alpha;"
	        + "  gl_Position = u_mvp * pos;"

	        //   normalize face x/y direction
	        + "  vec2 enc =  (a_light / 255.0);"
	        + "  vec2 fenc = enc * 4.0 - 2.0;"
	        + "  float f = dot(fenc, fenc);"
	        + "  float g = sqrt(1.0 - f / 4.0);"
	        + "  vec3 r_norm;"
	        + "  r_norm.xy = fenc * g;"
	        + "  r_norm.z = 1.0 - f / 2.0;"

	        //     normal points up or down (1,-1)
	        ////+ "    float dir = 1.0 - (2.0 * abs(mod(a_light.x,2.0)));"
	        //     recreate face normal vector
	        ///+ "    vec3 r_norm = vec3(n.xy, dir * (1.0 - length(n.xy)));"

	        + "  vec3 light_dir = normalize(vec3(0.2, 0.2, 1.0));"
	        + "  float l = dot(r_norm, light_dir) * 0.8;"

	        + "  light_dir = normalize(vec3(-0.2, -0.2, 1.0));"
	        + "  l += dot(r_norm, light_dir) * 0.2;"

	        //+ "  l = (l + (1.0 - r_norm.z))*0.5;"
	        /** ambient */
	        + "  l = 0.4 + l * 0.6;"

	        /** extreme fake-ssao by height */
	        + "  l += (clamp(a_pos.z / 2048.0, 0.0, 0.1) - 0.05);"
	        + "  color = vec4(u_color.rgb * (clamp(l, 0.0, 1.0) * alpha), u_color.a * alpha);"
	        + "}";

	final static String extrusionFragmentShader = ""
	        + "precision mediump float;"
	        + "varying vec4 color;"
	        + "void main() {"
	        + "  gl_FragColor = color;"
	        + "}";

	//final static String extrusionFragmentShaderZ = ""
	//        + "precision mediump float;"
	//        + "varying float depth;"
	//        + "void main() {"
	//        + "float d = depth * 0.2;"
	//        + "if (d < 0.0)"
	//        + "   d = -d;"
	//        + "  gl_FragColor = vec4(1.0 - d, 1.0 - d, 1.0 - d, 1.0 - d);"
	//        + "}";
}
