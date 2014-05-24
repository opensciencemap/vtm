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

import org.oscim.backend.GL20;
import org.oscim.core.Tile;
import org.oscim.layers.tile.MapTile;
import org.oscim.layers.tile.TileRenderer;
import org.oscim.layers.tile.TileSet;
import org.oscim.layers.tile.vector.BuildingLayer;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.renderer.elements.ExtrusionLayer;
import org.oscim.renderer.elements.ExtrusionLayers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtrusionRenderer extends LayerRenderer {
	static final Logger log = LoggerFactory.getLogger(ExtrusionRenderer.class);
	private final boolean debug = false;

	private final TileRenderer mTileLayer;
	private final int mZoomMin;
	private final int mZoomMax;
	private final boolean drawAlpha;

	protected float mAlpha = 1;
	private final int mMode;

	public ExtrusionRenderer(TileRenderer tileRenderLayer,
	        int zoomMin, int zoomMax, boolean mesh, boolean alpha) {
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
		mZoomMin = zoomMin;
		mZoomMax = zoomMax;
		mMode = mesh ? 1 : 0;
		drawAlpha = alpha;
	}

	private final TileSet mTileSet;
	private ExtrusionLayers[] mExtrusionLayerSet;
	private int mExtrusionLayerCnt;

	public static class Shader extends GLShader {
		int uMVP, uColor, uAlpha, uMode, aPos, aLight;

		public Shader(String shader) {
			if (!create(shader))
				return;

			uMVP = getUniform("u_mvp");
			uColor = getUniform("u_color");
			uAlpha = getUniform("u_alpha");
			uMode = getUniform("u_mode");
			aPos = getAttrib("a_pos");
			aLight = getAttrib("a_light");
		}
	}

	protected Shader mShader;

	@Override
	protected boolean setup() {
		if (mMode == 0)
			mShader = new Shader("extrusion_layer_ext");
		else
			mShader = new Shader("extrusion_layer_mesh");
		return true;
	}

	@Override
	public void update(GLViewport v) {

		if (mAlpha == 0 || v.pos.zoomLevel < (mZoomMin - 1)) {
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

		/* keep a list of tiles available for rendering */
		if (mExtrusionLayerSet == null || mExtrusionLayerSet.length < mTileSet.cnt * 4)
			mExtrusionLayerSet = new ExtrusionLayers[mTileSet.cnt * 4];

		int zoom = tiles[0].zoomLevel;

		/* compile one tile max per frame */
		boolean compiled = false;

		if (zoom >= mZoomMin && zoom <= mZoomMax) {
			/* TODO - if tile is not available try parent or children */

			for (int i = 0; i < mTileSet.cnt; i++) {
				ExtrusionLayers els = getLayer(tiles[i]);
				if (els == null)
					continue;

				if (els.compiled)
					mExtrusionLayerSet[activeTiles++] = els;
				else if (!compiled && els.compileLayers()) {
					mExtrusionLayerSet[activeTiles++] = els;
					compiled = true;
				}
			}
		} else if (zoom == mZoomMax + 1) {
			/* special case for s3db: render from parent tiles */
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i].node.parent();

				if (t == null)
					continue;

				//	for (MapTile c : mTiles)
				//		if (c == t)
				//			continue O;

				ExtrusionLayers els = getLayer(t);
				if (els == null)
					continue;

				if (els.compiled)
					mExtrusionLayerSet[activeTiles++] = els;

				else if (!compiled && els.compileLayers()) {
					mExtrusionLayerSet[activeTiles++] = els;
					compiled = true;
				}
			}
		} else if (zoom == mZoomMin - 1) {
			/* check if proxy children are ready */
			for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i];
				for (byte j = 0; j < 4; j++) {
					if (!t.hasProxy(1 << j))
						continue;

					MapTile c = t.node.child(j);
					ExtrusionLayers el = getLayer(c);

					if (el == null || !el.compiled)
						continue;

					mExtrusionLayerSet[activeTiles++] = el;
				}
			}
		}

		/* load more tiles on next frame */
		if (compiled)
			MapRenderer.animate();

		mExtrusionLayerCnt = activeTiles;

		//log.debug("active tiles: {}", mExtrusionLayerCnt);

		if (activeTiles > 0)
			setReady(true);
		else
			mTileLayer.releaseTiles(mTileSet);
	}

	private static ExtrusionLayers getLayer(MapTile t) {
		ElementLayers layers = t.getLayers();
		if (layers != null && !t.state(READY | NEW_DATA))
			return null;

		return BuildingLayer.get(t);
	}

	private void renderCombined(int vertexPointer, ExtrusionLayers els) {
		if (els.vboIndices == null)
			return;

		els.vboIndices.bind();
		els.vboVertices.bind();

		for (ExtrusionLayer el = els.layers; el != null; el = el.next()) {

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

	@Override
	public void render(GLViewport v) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster and would allow postprocessing outlines.

		ExtrusionLayers[] els = mExtrusionLayerSet;
		Shader s = mShader; //[mMode];

		if (debug) {
			s.useProgram();

			//GLState.useProgram(shaderProgram[mMode]);

			GLState.enableVertexArrays(s.aPos, s.aLight);
			GL.glUniform1i(s.uMode, 0);
			GLUtils.glUniform4fv(s.uColor, 4, DEBUG_COLOR);
			GL.glUniform1f(s.uAlpha, 1);

			GLState.test(false, false);
			GLState.blend(true);
			for (int i = 0; i < mExtrusionLayerCnt; i++) {
				ExtrusionLayer el = els[i].getLayers();

				setMatrix(v, els[i], 0);
				v.mvp.setAsUniform(s.uMVP);

				renderCombined(s.aPos, els[i]);

				/* just a temporary reference! */
				els[i] = null;
			}
			return;
		}

		GL.glDepthMask(true);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		s.useProgram();
		GLState.enableVertexArrays(s.aPos, -1);
		GLState.blend(false);

		GL.glEnable(GL20.GL_CULL_FACE);
		GL.glDepthFunc(GL20.GL_LESS);

		GL.glUniform1f(s.uAlpha, mAlpha);

		if (drawAlpha) {
			GL.glColorMask(false, false, false, false);
			GL.glUniform1i(s.uMode, -1);
			//GLUtils.glUniform4fv(uExtColor, 4, mColor);

			/* draw to depth buffer */
			for (int i = 0; i < mExtrusionLayerCnt; i++) {
				ExtrusionLayer el = els[i].getLayers();
				if (el == null)
					continue;

				int d = 0; // FIXME MapTile.depthOffset(t) * 10;

				setMatrix(v, els[i], d);
				v.mvp.setAsUniform(s.uMVP);

				renderCombined(s.aPos, els[i]);
			}

			GL.glColorMask(true, true, true, true);
			GL.glDepthMask(false);
			GLState.blend(true);
		}

		GLState.blend(true);
		GLState.enableVertexArrays(s.aPos, s.aLight);

		float[] currentColor = null;

		for (int i = 0; i < mExtrusionLayerCnt; i++) {
			ExtrusionLayer el = els[i].getLayers();

			if (el == null)
				continue;

			if (els[i].vboIndices == null)
				continue;

			els[i].vboIndices.bind();
			els[i].vboVertices.bind();

			int d = 0;
			if (drawAlpha) {
				GL.glDepthFunc(GL20.GL_EQUAL);

				// FIXME d = MapTile.depthOffset(t) * 10;
			}

			setMatrix(v, els[i], d);
			v.mvp.setAsUniform(s.uMVP);

			for (; el != null; el = el.next()) {

				if (el.colors != currentColor) {
					currentColor = el.colors;
					GLUtils.glUniform4fv(s.uColor, mMode == 0 ? 4 : 1,
					                     el.colors);
				}

				/* indices offset */
				int indexOffset = el.indexOffset;
				/* vertex byte offset */
				int vertexOffset = el.getOffset();

				GL.glVertexAttribPointer(s.aPos, 3,
				                         GL20.GL_SHORT, false, 8, vertexOffset);

				GL.glVertexAttribPointer(s.aLight, 2,
				                         GL20.GL_UNSIGNED_BYTE, false, 8, vertexOffset + 6);

				/* draw extruded outlines */
				if (el.numIndices[0] > 0) {
					/* draw roof */
					GL.glUniform1i(s.uMode, 0);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[2],
					                  GL20.GL_UNSIGNED_SHORT,
					                  (el.numIndices[0] + el.numIndices[1]) * 2);

					/* draw sides 1 */
					GL.glUniform1i(s.uMode, 1);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[0],
					                  GL20.GL_UNSIGNED_SHORT, 0);

					/* draw sides 2 */
					GL.glUniform1i(s.uMode, 2);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[1],
					                  GL20.GL_UNSIGNED_SHORT, el.numIndices[0] * 2);

					if (drawAlpha) {
						/* drawing gl_lines with the same coordinates does not
						 * result in same depth values as polygons, so add
						 * offset and draw gl_lequal: */
						GL.glDepthFunc(GL20.GL_LEQUAL);
					}

					v.mvp.addDepthOffset(100);
					v.mvp.setAsUniform(s.uMVP);

					GL.glUniform1i(s.uMode, 3);

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

					GL.glUniform1i(s.uMode, 4);
					GL.glDrawElements(GL20.GL_TRIANGLES, el.numIndices[4],
					                  GL20.GL_UNSIGNED_SHORT, offset);
				}
			}

			/* just a temporary reference! */
			els[i] = null;
		}

		GL.glDepthMask(false);
		GL.glDisable(GL20.GL_CULL_FACE);

		GL.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);

		mTileLayer.releaseTiles(mTileSet);
	}

	private static void setMatrix(GLViewport v, ExtrusionLayers l, int delta) {
		int z = l.zoomLevel;
		double curScale = Tile.SIZE * v.pos.scale;
		float scale = (float) (v.pos.scale / (1 << z));

		float x = (float) ((l.x - v.pos.x) * curScale);
		float y = (float) ((l.y - v.pos.y) * curScale);
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
}
