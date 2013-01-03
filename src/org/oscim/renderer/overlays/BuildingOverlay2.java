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
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.MapTile;
import org.oscim.renderer.TileManager;
import org.oscim.renderer.TileSet;
import org.oscim.renderer.layer.ExtrusionLayer;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class BuildingOverlay2 extends RenderOverlay {
	private final static String TAG = BuildingOverlay2.class.getName();

	public BuildingOverlay2(MapView mapView) {
		super(mapView);
	}

	private static int buildingProgram;
	private static int hBuildingVertexPosition;
	private static int hBuildingLightPosition;
	private static int hBuildingMatrix;
	private static int hBuildingColor;
	private static int hBuildingMode;

	private boolean initialized = false;

	private int BUFFERSIZE = 65536 * 2;
	private TileSet mTileSet;
	private ShortBuffer mShortBuffer;

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		if (!initialized) {
			initialized = true;

			// Set up the program for rendering buildings
			buildingProgram = GlUtils.createProgram(buildingVertexShader,
					buildingFragmentShader);
			if (buildingProgram == 0) {
				Log.e("blah", "Could not create building program.");
				return;
			}
			hBuildingMatrix = GLES20.glGetUniformLocation(buildingProgram, "u_mvp");
			hBuildingColor = GLES20.glGetUniformLocation(buildingProgram, "u_color");
			hBuildingMode = GLES20.glGetUniformLocation(buildingProgram, "u_mode");
			hBuildingVertexPosition = GLES20.glGetAttribLocation(buildingProgram, "a_position");
			hBuildingLightPosition = GLES20.glGetAttribLocation(buildingProgram, "a_light");

			ByteBuffer buf = ByteBuffer.allocateDirect(BUFFERSIZE)
					.order(ByteOrder.nativeOrder());

			mShortBuffer = buf.asShortBuffer();
		}

		int ready = 0;
		//if (curPos.zoomLevel < 17)
		mTileSet = TileManager.getActiveTiles(mTileSet);
		MapTile[] tiles = mTileSet.tiles;
		for (int i = 0; i < mTileSet.cnt; i++) {
			if (!tiles[i].isVisible || tiles[i].layers == null
					|| tiles[i].layers.extrusionLayers == null)
				continue;

			ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;

			if (el.ready && !el.compiled) {
				el.compile(mShortBuffer);
				GlUtils.checkGlError("...");
			}

			if (el.compiled)
				ready++;
		}

		isReady = ready > 0;
	}

	// r: 0.815686275, 0.91372549
	// g:              0.901960784
	// b:              0.890196078
	// sligthly differ adjacent faces to imrpove contrast
	float mColor[] = { 0.71872549f, 0.701960784f, 0.690196078f, 0.7f };
	float mColor2[] = { 0.71372549f, 0.701960784f, 0.695196078f, 0.7f };
	float mRoofColor[] = { 0.81f, 0.80f, 0.79f, 0.7f };
	boolean debug = false;

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {

		boolean first = true;

		if (debug) {
			MapTile[] tiles = mTileSet.tiles;
			for (int i = 0; i < mTileSet.cnt; i++) {
				if (!tiles[i].isVisible || tiles[i].layers == null
						|| tiles[i].layers.extrusionLayers == null)
					continue;

				ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;
				if (!el.compiled)
					continue;

				if (first) {
					GLES20.glUseProgram(buildingProgram);
					GLRenderer.enableVertexArrays(hBuildingVertexPosition, hBuildingLightPosition);
					GLES20.glUniform1i(hBuildingMode, 0);
					GLES20.glUniform4f(hBuildingColor, 0.6f, 0.6f, 0.6f, 0.8f);
					first = false;
				}

				setMatrix(pos, mv, proj, tiles[i], 1);
				GLES20.glUniformMatrix4fv(hBuildingMatrix, 1, false, mv, 0);

				GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

				GLES20.glVertexAttribPointer(hBuildingVertexPosition, 3,
						GLES20.GL_SHORT, false, 8, 0);

				GLES20.glVertexAttribPointer(hBuildingLightPosition, 2,
						GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

				GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mNumIndices,
						GLES20.GL_UNSIGNED_SHORT, 0);
			}
			return;
		}

		int drawCount = 20;
		// draw to depth buffer
		MapTile[] tiles = mTileSet.tiles;
		for (int i = 0; i < mTileSet.cnt; i++) {
			if (!tiles[i].isVisible || tiles[i].layers == null
					|| tiles[i].layers.extrusionLayers == null)
				continue;

			ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;
			if (!el.compiled)
				continue;

			if (first) {
				GLES20.glUseProgram(buildingProgram);
				GLRenderer.enableVertexArrays(hBuildingVertexPosition, -1);

				GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
				GLES20.glEnable(GLES20.GL_DEPTH_TEST);
				GLES20.glEnable(GLES20.GL_CULL_FACE);
				GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
				//GLES20.glCullFace(GLES20.GL_CW);
				GLES20.glDepthMask(true);
				GLES20.glDepthFunc(GLES20.GL_LESS);
				GLES20.glUniform1i(hBuildingMode, 0);
				GLES20.glColorMask(false, false, false, false);
				first = false;
			}

			GLES20.glPolygonOffset(0, drawCount--);
			// seems there are not infinite offset units possible
			// this should suffice for at least two rows, i.e.
			// having not two neighbours with the same depth
			if (drawCount == 0)
				drawCount = 20;

			setMatrix(pos, mv, proj, tiles[i], 1);
			GLES20.glUniformMatrix4fv(hBuildingMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(hBuildingVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mNumIndices,
					GLES20.GL_UNSIGNED_SHORT, 0);
		}

		if (first)
			return;
		// enable color buffer, use depth mask
		GLRenderer.enableVertexArrays(hBuildingVertexPosition, hBuildingLightPosition);
		GLES20.glColorMask(true, true, true, true);
		GLES20.glDepthMask(false);
		GLES20.glDepthFunc(GLES20.GL_EQUAL);

		drawCount = 20;

		for (int i = 0; i < mTileSet.cnt; i++) {
			if (!tiles[i].isVisible || tiles[i].layers == null
					|| tiles[i].layers.extrusionLayers == null)
				continue;

			ExtrusionLayer el = (ExtrusionLayer) tiles[i].layers.extrusionLayers;
			if (!el.compiled)
				continue;

			GLES20.glPolygonOffset(0, drawCount--);
			if (drawCount == 0)
				drawCount = 20;

			setMatrix(pos, mv, proj, tiles[i], 1);
			GLES20.glUniformMatrix4fv(hBuildingMatrix, 1, false, mv, 0);

			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, el.mIndicesBufferID);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, el.mVertexBufferID);

			GLES20.glVertexAttribPointer(hBuildingVertexPosition, 3,
					GLES20.GL_SHORT, false, 8, 0);

			GLES20.glVertexAttribPointer(hBuildingLightPosition, 2,
					GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

			// draw roof
			GLES20.glUniform1i(hBuildingMode, 0);
			//GLES20.glUniform4f(hBuildingColor, 0.81f, 0.8f, 0.8f, 0.9f);
			GLES20.glUniform4fv(hBuildingColor, 1, mRoofColor, 0);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[2],
					GLES20.GL_UNSIGNED_SHORT, (el.mIndiceCnt[0] + el.mIndiceCnt[1]) * 2);

			// draw sides 1
			//GLES20.glUniform4f(hBuildingColor, 0.8f, 0.8f, 0.8f, 1.0f);
			//GLES20.glUniform4f(hBuildingColor, 0.9f, 0.905f, 0.9f, 1.0f);
			GLES20.glUniform4fv(hBuildingColor, 1, mColor, 0);
			GLES20.glUniform1i(hBuildingMode, 1);
			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[0],
					GLES20.GL_UNSIGNED_SHORT, 0);

			// draw sides 2
			//GLES20.glUniform4f(hBuildingColor, 0.9f, 0.9f, 0.905f, 1.0f);
			GLES20.glUniform4fv(hBuildingColor, 1, mColor2, 0);
			GLES20.glUniform1i(hBuildingMode, 2);

			GLES20.glDrawElements(GLES20.GL_TRIANGLES, el.mIndiceCnt[1],
					GLES20.GL_UNSIGNED_SHORT, el.mIndiceCnt[0] * 2);

			GlUtils.checkGlError("...");
		}

		if (!first) {
			GLES20.glDisable(GLES20.GL_CULL_FACE);
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);
			GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
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

		Matrix.multiplyMM(matrix, 0, mapPosition.viewMatrix, 0, matrix, 0);
		Matrix.multiplyMM(matrix, 0, proj, 0, matrix, 0);
	}

	final static String buildingVertexShader = ""
			+ "precision mediump float;"
			+ "uniform mat4 u_mvp;"
			+ "uniform vec4 u_color;"
			+ "uniform int u_mode;"
			+ "uniform float u_scale;"
			+ "attribute vec4 a_position;"
			+ "attribute vec2 a_light;"
			+ "varying vec4 color;"
			+ "const float ff = 255.0;"
			+ "void main() {"
			+ "  gl_Position = u_mvp * a_position;"
			+ "  if (u_mode == 0)"
			//     roof / depth pass
			+ "    color = u_color;"
			+ "  else if (u_mode == 1)"
			//     sides 1 - use 0xff00
			+ "    color = vec4(u_color.rgb * (a_light.y / ff), 0.8);"
			+ "  else"
			//     sides 2 - use 0x00ff
			+ "    color = vec4(u_color.rgb * (a_light.x / ff), 0.8);"
			+ "}";

	final static String buildingFragmentShader = ""
			+ "precision lowp float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";
}
