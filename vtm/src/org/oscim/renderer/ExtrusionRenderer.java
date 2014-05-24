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
	private final int mZoomMin;
	private final int mZoomMax;
	private final boolean drawAlpha;

	protected float mAlpha = 1;

	public ExtrusionRenderer(TileRenderer tileRenderLayer,
	        int zoomMin, int zoomMax, boolean mesh, boolean alpha) {
		mTileLayer = tileRenderLayer;
		mTileSet = new TileSet();
		mZoomMin = zoomMin;
		mZoomMax = zoomMax;
		mMode = mesh ? 1 : 0;
		drawAlpha = alpha;
	}

	private boolean initialized = false;

	private final TileSet mTileSet;
	private MapTile[] mTiles;
	private int mTileCnt;

	private final int mMode;

	static class Shader extends GLShader {
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

	private Shader mShader[] = { null, null };

	private boolean initShader() {
		initialized = true;

		mShader[0] = new Shader("extrusion_layer_ext");
		mShader[1] = new Shader("extrusion_layer_mesh");

		return true;
	}

	@Override
	public void update(GLViewport v) {

		if (!initialized && !initShader())
			return;

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
		if (mTiles == null || mTiles.length < mTileSet.cnt * 4)
			mTiles = new MapTile[mTileSet.cnt * 4];

		int zoom = tiles[0].zoomLevel;

		/* compile one tile max per frame */
		boolean compiled = false;

		if (zoom >= mZoomMin && zoom <= mZoomMax) {
			// TODO - if tile is not available try parent or children

			for (int i = 0; i < mTileSet.cnt; i++) {
				ExtrusionLayer el = getLayer(tiles[i]);
				if (el == null)
					continue;

				if (el.compiled)
					mTiles[activeTiles++] = tiles[i];
				else if (!compiled && compileLayers(el)) {
					mTiles[activeTiles++] = tiles[i];
					compiled = true;
				}
			}
		} else if (zoom == mZoomMax + 1) {
			/* special case for s3db: render from parent tiles */
			O: for (int i = 0; i < mTileSet.cnt; i++) {
				MapTile t = tiles[i].node.parent();

				if (t == null)
					continue;

				for (MapTile c : mTiles)
					if (c == t)
						continue O;

				ExtrusionLayer el = getLayer(t);
				if (el == null)
					continue;

				if (el.compiled)
					mTiles[activeTiles++] = tiles[i];

				else if (!compiled && compileLayers(el)) {
					mTiles[activeTiles++] = t;
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
					ExtrusionLayer el = getLayer(c);

					if (el == null || !el.compiled)
						continue;

					mTiles[activeTiles++] = c;
				}
			}
		}

		/* load more tiles on next frame */
		if (compiled)
			MapRenderer.animate();

		mTileCnt = activeTiles;

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

		size = sumVertices * 4 * 2;
		if (vbuf.position() != sumVertices * 4) {
			int pos = vbuf.position();
			log.error("invalid vertex size: {} {}", sumVertices, pos);
			size = pos * 2;
		}

		el.vboVertices = BufferObject.get(GL20.GL_ARRAY_BUFFER, size);
		el.vboVertices.loadBufferData(vbuf.flip(), size);
		el.vboVertices.unbind();

		GLUtils.checkGlError("extrusion layer");
		return true;
	}

	private static ExtrusionLayer getLayer(MapTile t) {
		ElementLayers layers = t.getLayers();
		if (layers == null || !t.state(READY | NEW_DATA))
			return null;

		return layers.getExtrusionLayers();
	}

	private final boolean debug = false;

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

	@Override
	public void render(GLViewport v) {
		// TODO one could render in one pass to texture and then draw the texture
		// with alpha... might be faster and would allow postprocessing outlines.

		MapTile[] tiles = mTiles;
		Shader s = mShader[mMode];

		if (debug) {
			s.useProgram();

			//GLState.useProgram(shaderProgram[mMode]);

			GLState.enableVertexArrays(s.aPos, s.aLight);
			GL.glUniform1i(s.uMode, 0);
			GLUtils.glUniform4fv(s.uColor, 4, DEBUG_COLOR);
			GL.glUniform1f(s.uAlpha, 1);

			GLState.test(false, false);
			GLState.blend(true);
			for (int i = 0; i < mTileCnt; i++) {
				ExtrusionLayer el = tiles[i].getLayers().getExtrusionLayers();

				setMatrix(v, tiles[i], 0);
				v.mvp.setAsUniform(s.uMVP);

				renderCombined(s.aPos, el);

				// just a temporary reference!
				tiles[i] = null;
			}
			return;
		}

		GL.glDepthMask(true);
		GL.glClear(GL20.GL_DEPTH_BUFFER_BIT);

		GLState.test(true, false);

		s.useProgram();
		//GLState.useProgram(shaderProgram[mMode]);
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
			for (int i = 0; i < mTileCnt; i++) {
				MapTile t = tiles[i];
				ExtrusionLayer el = t.getLayers().getExtrusionLayers();
				if (el == null)
					continue;

				int d = MapTile.depthOffset(t) * 10;

				setMatrix(v, t, d);
				v.mvp.setAsUniform(s.uMVP);

				renderCombined(s.aPos, el);
			}

			GL.glColorMask(true, true, true, true);
			GL.glDepthMask(false);
			GLState.blend(true);
		}

		GLState.blend(true);
		GLState.enableVertexArrays(s.aPos, s.aLight);

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
			v.mvp.setAsUniform(s.uMVP);

			el.vboIndices.bind();
			el.vboVertices.bind();

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
}
