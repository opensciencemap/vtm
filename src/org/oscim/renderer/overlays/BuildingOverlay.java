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
import org.oscim.core.Tile;
import org.oscim.renderer.GLRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.layer.VertexPool;
import org.oscim.renderer.layer.VertexPoolItem;
import org.oscim.utils.FastMath;
import org.oscim.utils.GlUtils;
import org.oscim.view.MapView;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class BuildingOverlay extends RenderOverlay {

	public BuildingOverlay(MapView mapView) {
		super(mapView);
	}

	private static int buildingProgram;
	private static int hBuildingVertexPosition;
	private static int hBuildingLightPosition;
	private static int hBuildingMatrix;
	private static int hBuildingColor;
	private static int hBuildingMode;

	private boolean initialized = false;

	private int mIndicesBufferID;
	private int mVertexBufferID;

	private int mNumIndices = 0;
	private int mNumVertices = 0;
	private VertexPoolItem mVertices, mCurVertices;
	private VertexPoolItem mIndices[], mCurIndices[];

	private int mIndiceCnt[] = { 0, 0, 0 };

	private void addOutline(float[] points, float height) {
		int len = points.length;
		boolean oddFace = (len % 4 != 0);
		int verticesCnt = len + (oddFace ? 2 : 0);
		int indicesCnt = len / 2 * 6;

		short h = (short) height;

		float cx = points[len - 2];
		float cy = points[len - 1];
		float nx = points[0];
		float ny = points[1];

		float vx = nx - cx;
		float vy = ny - cy;
		float a = vx / (float) Math.sqrt(vx * vx + vy * vy);

		short color1 = (short) (200 + (40 * (a > 0 ? a : -a)));
		short fcolor = color1;
		short color2 = 0;

		boolean even = true;

		short[] vertices = mCurVertices.vertices;
		int v = mCurVertices.used;

		for (int i = 0; i < len; i += 2, v += 8) {
			cx = nx;
			cy = ny;

			if (v == VertexPoolItem.SIZE) {
				mCurVertices.used = VertexPoolItem.SIZE;
				mCurVertices.next = VertexPool.get();
				mCurVertices = mCurVertices.next;
				vertices = mCurVertices.vertices;
				v = 0;
			}

			vertices[v + 0] = vertices[v + 4] = (short) cx;
			vertices[v + 1] = vertices[v + 5] = (short) cy;

			vertices[v + 2] = 0;
			vertices[v + 6] = h;

			if (i < len - 2) {
				nx = points[i + 2];
				ny = points[i + 3];

				vx = nx - cx;
				vy = ny - cy;
				a = vx / (float) Math.sqrt(vx * vx + vy * vy);

				color2 = (short) (200 + (40 * (a > 0 ? a : -a)));
			} else {
				color2 = fcolor;
			}

			short c;
			if (even)
				c = (short) (color1 | color2 << 8);
			else
				c = (short) (color2 | color1 << 8);

			vertices[v + 3] = vertices[v + 7] = c;

			color1 = color2;
			even = !even;
		}

		if (oddFace) {
			//int v = len * 4;
			if (v == VertexPoolItem.SIZE) {
				mCurVertices.used = VertexPoolItem.SIZE;
				mCurVertices.next = VertexPool.get();
				mCurVertices = mCurVertices.next;
				vertices = mCurVertices.vertices;
				v = 0;
			}

			cx = points[0];
			cy = points[1];

			vertices[v + 0] = vertices[v + 4] = (short) cx;
			vertices[v + 1] = vertices[v + 5] = (short) cy;

			vertices[v + 2] = 0;
			vertices[v + 6] = h;

			short c = (short) (color1 | fcolor << 8);
			vertices[v + 3] = vertices[v + 7] = c;

			v += 8;
		}

		mCurVertices.used = v;

		// fill ZigZagQuadIndices(tm)
		for (int j = 0; j < 2; j++) {
			short[] indices = mCurIndices[j].vertices;
			int cnt = mCurIndices[j].used;

			for (int k = j * 2; k < len; k += 4) {
				int i = mNumVertices + k;

				short s0 = (short) (i + 0);
				short s1 = (short) (i + 1);
				short s2 = (short) (i + 2);
				short s3 = (short) (i + 3);

				if (cnt == VertexPoolItem.SIZE) {
					mCurIndices[j].used = VertexPoolItem.SIZE;
					mCurIndices[j].next = VertexPool.get();
					mCurIndices[j] = mCurIndices[j].next;
					vertices = mCurIndices[j].vertices;
					cnt = 0;
				}

				// connect last to first (when number of faces is even)
				if (k + 3 > verticesCnt) {
					s2 -= verticesCnt;
					s3 -= verticesCnt;
				}

				indices[cnt++] = s0;
				indices[cnt++] = s1;
				indices[cnt++] = s2;

				indices[cnt++] = s1;
				indices[cnt++] = s3;
				indices[cnt++] = s2;
				//System.out.println("indice:" + k + "\t" + s0 + "," + s1 + "," + s2);
				//System.out.println("indice:" + k + "\t" + s1 + "," + s3 + "," + s2);
			}
			mCurIndices[j].used = cnt;
		}

		// roof indices for convex shapes
		int cnt = mCurIndices[2].used;
		short[] indices = mCurIndices[2].vertices;
		short first = (short) (mNumVertices + 1);

		for (int k = 0; k < len - 4; k += 2) {

			if (cnt == VertexPoolItem.SIZE) {
				mCurIndices[2].used = VertexPoolItem.SIZE;
				mCurIndices[2].next = VertexPool.get();
				mCurIndices[2] = mCurIndices[2].next;
				vertices = mCurIndices[2].vertices;
				cnt = 0;
			}
			indices[cnt++] = first;
			indices[cnt++] = (short) (first + k + 4);
			indices[cnt++] = (short) (first + k + 2);

			System.out.println("indice:" + k + "\t" + indices[cnt - 3] + "," + indices[cnt - 2]
					+ "," + indices[cnt - 1]);

			indicesCnt += 3;
		}
		mCurIndices[2].used = cnt;

		mNumVertices += verticesCnt;
		mNumIndices += indicesCnt;
	}

	@Override
	public synchronized void update(MapPosition curPos, boolean positionChanged,
			boolean tilesChanged) {

		if (initialized)
			return;
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

		mVertices = mCurVertices = VertexPool.get();
		mIndices = new VertexPoolItem[3];
		mCurIndices = new VertexPoolItem[3];
		mIndices[0] = mCurIndices[0] = VertexPool.get();
		mIndices[1] = mCurIndices[1] = VertexPool.get();
		mIndices[2] = mCurIndices[2] = VertexPool.get();

		float height = 450;

		float[] points = {
				-200, -200,
				200, -200,
				200, 200,
				-200, 200,
				-300, 0
		};
		addOutline(points, height);

		float[] points2 = {
				300, -300,
				500, -300,
				600, 100,
				300, 100,
				//	350, 0
		};

		addOutline(points2, height);

		height = 650;

		float[] points4 = new float[80];
		for (int i = 0; i < 80; i += 2) {
			points4[i + 0] = (float) (Math.sin(i / -40f * Math.PI) * 200);
			points4[i + 1] = (float) (Math.cos(i / -40f * Math.PI) * 200) - 600;
		}

		addOutline(points4, height);

		height = 950;

		points4 = new float[40];
		for (int i = 0; i < 40; i += 2) {
			points4[i + 0] = (float) (Math.sin(i / -20f * Math.PI) * 100);
			points4[i + 1] = (float) (Math.cos(i / -20f * Math.PI) * 100) - 550;
		}

		addOutline(points4, height);

		float[] points3 = new float[24];
		for (int i = 0; i < 24; i += 2) {
			points3[i + 0] = (float) (Math.sin(i / -12f * Math.PI) * 200) - 600;
			points3[i + 1] = (float) (Math.cos(i / -12f * Math.PI) * 200) - 600;
		}

		addOutline(points3, height);

		int bufferSize = Math.max(mNumVertices * 4 * 2, mNumIndices * 2);
		ByteBuffer buf = ByteBuffer.allocateDirect(bufferSize)
				.order(ByteOrder.nativeOrder());

		ShortBuffer sbuf = buf.asShortBuffer();

		int[] mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexBufferID = mVboIds[1];

		// upload indices
		for (int i = 0; i < 3; i++) {
			for (VertexPoolItem vi = mIndices[i]; vi != null; vi = vi.next) {
				System.out.println("put indices: " + vi.used + " " + mNumIndices);
				sbuf.put(vi.vertices, 0, vi.used);
				mIndiceCnt[i] += vi.used;
			}
		}
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mNumIndices * 2, sbuf, GLES20.GL_STATIC_DRAW);
		sbuf.clear();

		// upload vertices
		for (VertexPoolItem vi = mVertices; vi != null; vi = vi.next) {
			System.out.println("put vertices: " + vi.used + " " + mNumVertices);
			sbuf.put(vi.vertices, 0, vi.used);
		}
		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				mNumVertices * 4 * 2, sbuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		mMapView.getMapViewPosition().getMapPosition(mMapPosition, null);

		// tell GLRenderer to call 'render'
		isReady = true;
	}

	@Override
	public synchronized void render(MapPosition pos, float[] mv, float[] proj) {

		setMatrix(pos, mv);
		Matrix.multiplyMM(mv, 0, proj, 0, mv, 0);

		GLState.useProgram(buildingProgram);

		GLES20.glUniformMatrix4fv(hBuildingMatrix, 1, false, mv, 0);
		GLES20.glUniform4f(hBuildingColor, 0.5f, 0.5f, 0.5f, 0.7f);

		GLState.enableVertexArrays(hBuildingVertexPosition, hBuildingLightPosition);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);

		GLES20.glVertexAttribPointer(hBuildingVertexPosition, 3,
				GLES20.GL_SHORT, false, 8, 0);

		GLES20.glVertexAttribPointer(hBuildingLightPosition, 2,
				GLES20.GL_UNSIGNED_BYTE, false, 8, 6);

		// GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices,
		// GLES20.GL_UNSIGNED_SHORT, 0);

		// draw to depth buffer
		GLES20.glUniform1i(hBuildingMode, 0);
		GLES20.glColorMask(false, false, false, false);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		GLState.test(true, false);
		GLES20.glEnable(GLES20.GL_CULL_FACE);

		GLES20.glDepthMask(true);
		GLES20.glDepthFunc(GLES20.GL_LESS);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mNumIndices,
				GLES20.GL_UNSIGNED_SHORT, 0);

		// enable color buffer, use depth mask
		GLES20.glColorMask(true, true, true, true);
		GLES20.glDepthMask(false);
		GLES20.glDepthFunc(GLES20.GL_EQUAL);

		// draw roof
		GLES20.glUniform4f(hBuildingColor, 0.75f, 0.7f, 0.7f, 0.9f);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndiceCnt[0],
				GLES20.GL_UNSIGNED_SHORT, (mIndiceCnt[0] + mIndiceCnt[1]) * 2);

		// draw sides 1
		//GLES20.glUniform4f(hBuildingColor, 0.8f, 0.8f, 0.8f, 1.0f);
		GLES20.glUniform4f(hBuildingColor, 0.9f, 0.905f, 0.9f, 1.0f);
		GLES20.glUniform1i(hBuildingMode, 1);
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndiceCnt[0],
				GLES20.GL_UNSIGNED_SHORT, 0);

		// draw sides 2
		GLES20.glUniform4f(hBuildingColor, 0.9f, 0.9f, 0.905f, 1.0f);
		GLES20.glUniform1i(hBuildingMode, 2);

		GLES20.glDrawElements(GLES20.GL_TRIANGLES, mIndiceCnt[1],
				GLES20.GL_UNSIGNED_SHORT, mIndiceCnt[0] * 2);

		GLES20.glDisable(GLES20.GL_CULL_FACE);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GlUtils.checkGlError("...");
	}

	@Override
	protected void setMatrix(MapPosition curPos, float[] matrix) {

		MapPosition oPos = mMapPosition;

		byte z = oPos.zoomLevel;

		float div = FastMath.pow(z - curPos.zoomLevel);
		float x = (float) (oPos.x - curPos.x * div);
		float y = (float) (oPos.y - curPos.y * div);

		// flip around date-line
		float max = (Tile.TILE_SIZE << z);
		if (x < -max / 2)
			x = max + x;
		else if (x > max / 2)
			x = x - max;

		float scale = curPos.scale / div;

		Matrix.setIdentityM(matrix, 0);

		// translate relative to map center
		matrix[12] = x * scale;
		matrix[13] = y * scale;
		// scale to current tile world coordinates
		scale = (curPos.scale / oPos.scale) / div;
		scale /= GLRenderer.COORD_MULTIPLIER;
		matrix[0] = scale;
		matrix[5] = scale;
		matrix[10] = scale / 1000f;

		Matrix.multiplyMM(matrix, 0, curPos.viewMatrix, 0, matrix, 0);
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
			+ "    color = vec4(u_color.rgb * (a_light.y / ff), 0.95);"
			+ "  else"
			//     sides 2 - use 0x00ff
			+ "    color = vec4(u_color.rgb * (a_light.x / ff), 0.95);"
			+ "}";

	final static String buildingFragmentShader = ""
			+ "precision mediump float;"
			+ "varying vec4 color;"
			+ "void main() {"
			+ "  gl_FragColor = color;"
			+ "}";

	@Override
	public void compile() {
		// TODO Auto-generated method stub

	}

	//	private short[] mVertices = {
	//	// 0 - north
	//	-200, -200, 0,
	//	(short) (220 | (200 << 8)),
	//	// 1
	//	-200, -200, 950,
	//	(short) (220 | (200 << 8)),
	//	// 2
	//	200, -200, 0,
	//	(short) (170 | (200 << 8)),
	//	// 3
	//	200, -200, 950,
	//	(short) (170 | (200 << 8)),
	//
	//	// 4 - south
	//	200, 200, 0,
	//	(short) (170 | (180 << 8)),
	//	// 5
	//	200, 200, 950,
	//	(short) (170 | (180 << 8)),
	//	// 6
	//	-200, 200, 0,
	//	(short) (220 | (180 << 8)),
	//	// 7
	//	-200, 200, 950,
	//	(short) (220 | (180 << 8)),
	//};
	//
	//private short[] mIndices = {
	//	// north
	//	0, 1, 2,
	//	1, 3, 2,
	//	// south
	//	4, 5, 6,
	//	5, 7, 6,
	//	// east
	//	2, 3, 4,
	//	3, 5, 4,
	//	// west
	//	6, 7, 0,
	//	7, 1, 0,
	//	// top
	//	1, 5, 3,
	//	7, 5, 1
	//};
	//
	//
	//private int mNumIndices = mIndices.length;
	//private int mNumVertices = mVertices.length / 4;
}
