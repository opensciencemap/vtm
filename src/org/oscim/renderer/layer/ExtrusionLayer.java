/*
 * Copyright 2012, 2013 OpenScienceMap
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
package org.oscim.renderer.layer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import org.oscim.renderer.GLRenderer;
import org.oscim.view.MapView;
import org.quake.triangle.TriangleJNI;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 */
public class ExtrusionLayer extends Layer {
	private final static String TAG = ExtrusionLayer.class.getName();
	private static final float S = GLRenderer.COORD_MULTIPLIER;
	public int mIndicesBufferID;
	public int mVertexBufferID;
	public int mNumIndices = 0;
	private int mNumVertices = 0;
	private VertexPoolItem mVertices, mCurVertices;
	private VertexPoolItem mIndices[], mCurIndices[];

	public int mIndiceCnt[] = { 0, 0, 0 };

	public ExtrusionLayer(int level) {
		this.type = Layer.EXTRUSION;
		this.layer = level;

		mVertices = mCurVertices = VertexPool.get();
		mIndices = new VertexPoolItem[3];
		mCurIndices = new VertexPoolItem[3];
		mIndices[0] = mCurIndices[0] = VertexPool.get();
		mIndices[1] = mCurIndices[1] = VertexPool.get();
		mIndices[2] = mCurIndices[2] = VertexPool.get();
	}

	public void addBuildings(float[] points, short[] index, int height) {
		int complex = 0;
		boolean simple = true;

		if (height == 0)
			height = 400;
		else
			height *= 40;

		for (int i = 0, pos = 0, n = index.length; i < n; i++) {
			int length = index[i];

			// end marker
			if (length < 0)
				break;

			// start next polygon
			if (length == 0) {
				complex = i + 1;
				simple = true;
				continue;
			}

			// need at least three points
			if (length < 6) {
				pos += length;
				continue;
			}

			// check if polygon contains inner rings
			//if (simple && ((i < n - 1) && (index[i + 1] > 0)))
			//	simple = false;

			addOutline(points, pos, length, height, simple);
			pos += length;
		}
	}

	private void addOutline(float[] points, int pos, int len, float height, boolean simple) {
		if (!MapView.enableClosePolygons)
			len -= 2;

		// add two vertices for last face to make zigzag indices work
		boolean addFace = (len % 4 != 0);

		// Log.d(TAG, "add: " + addFace + " " + len + "  (" + pos + ")");

		int vertexCnt = len + (addFace ? 2 : 0);
		int indicesCnt = (len >> 1) * 6;

		short h = (short) height;

		float cx = points[pos + len - 2];
		float cy = points[pos + len - 1];
		float nx = points[pos + 0];
		float ny = points[pos + 1];

		float vx = nx - cx;
		float vy = ny - cy;
		float ca = (float) Math.sqrt(vx * vx + vy * vy);
		float pa = ca;
		float ux = vx;
		float uy = vy;

		float vlight = vx > 0 ? (vx / ca) : -(vx / ca) - 0.1f;

		short color1 = (short) (200 + (50 * vlight));
		short fcolor = color1;
		short color2 = 0;

		boolean even = true;

		short[] vertices = mCurVertices.vertices;
		int v = mCurVertices.used;

		int convex = 0;

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

			vertices[v + 0] = vertices[v + 4] = (short) (cx * S);
			vertices[v + 1] = vertices[v + 5] = (short) (cy * S);

			vertices[v + 2] = 0;
			vertices[v + 6] = h;

			if (i < len - 2) {
				nx = points[pos + i + 2];
				ny = points[pos + i + 3];

				vx = nx - cx;
				vy = ny - cy;
				ca = (float) Math.sqrt(vx * vx + vy * vy);

				if (convex > -1) {
					// TODO fix for straight line...
					double dir = (vx * ux + vy * uy) / (ca * pa);

					if (convex == 0)
						convex = dir > 0 ? 1 : 2;
					else if (convex == 1)
						convex = dir > 0 ? 1 : -1;
					else
						convex = dir > 0 ? -1 : 2;
				}
				vlight = vx > 0 ? (vx / ca) : -(vx / ca) - 0.1f;
				color2 = (short) (200 + (50 * vlight));
			} else {
				color2 = fcolor;
			}

			short c;
			if (even)
				c = (short) (color1 | color2 << 8);
			else
				c = (short) (color2 | color1 << 8);

			vertices[v + 3] = vertices[v + 7] = c;

			pa = ca;
			ux = vx;
			uy = vy;
			color1 = color2;
			even = !even;
		}

		if (addFace) {
			if (v == VertexPoolItem.SIZE) {
				mCurVertices.used = VertexPoolItem.SIZE;
				mCurVertices.next = VertexPool.get();
				mCurVertices = mCurVertices.next;
				vertices = mCurVertices.vertices;
				v = 0;
			}

			cx = points[pos + 0];
			cy = points[pos + 1];

			vertices[v + 0] = vertices[v + 4] = (short) (cx * S);
			vertices[v + 1] = vertices[v + 5] = (short) (cy * S);

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

			// index id relative to mCurIndices
			int i = mCurIndices[j].used;

			// vertex id
			v = mNumVertices + (j * 2);

			for (int k = j * 2; k < len; k += 4) {
				short s0 = (short) (v++);
				short s1 = (short) (v++);
				short s2 = (short) (v++);
				short s3 = (short) (v++);

				if (i == VertexPoolItem.SIZE) {
					mCurIndices[j].used = VertexPoolItem.SIZE;
					mCurIndices[j].next = VertexPool.get();
					mCurIndices[j] = mCurIndices[j].next;
					indices = mCurIndices[j].vertices;
					i = 0;
				}

				if (k + 2 == len) {
					// connect last to first (when number of faces is even)
					if (!addFace) {
						//Log.d(TAG, "connect last  " + vertexCnt + " " + len);
						s2 -= len;
						s3 -= len;
					}
				}
				indices[i++] = s0;
				indices[i++] = s1;
				indices[i++] = s2;

				indices[i++] = s1;
				indices[i++] = s3;
				indices[i++] = s2;
				//System.out.println(" i:" + (mNumIndices + (k * 6))
				//	+ "\t(" + s0 + "," + s1 + "," + s2
				//	+ ")\t(" + s1 + "," + s3 + "," + s2 + ")");
			}
			mCurIndices[j].used = i;
		}

		if (simple && (len <= 8 || convex > 0)) {
			//Log.d(TAG, len + " is simple " + convex);

			// roof indices for convex shapes
			int i = mCurIndices[2].used;
			short[] indices = mCurIndices[2].vertices;
			short first = (short) (mNumVertices + 1);

			for (int k = 0; k < len - 4; k += 2) {
				if (i == VertexPoolItem.SIZE) {
					mCurIndices[2].used = VertexPoolItem.SIZE;
					mCurIndices[2].next = VertexPool.get();
					mCurIndices[2] = mCurIndices[2].next;
					indices = mCurIndices[2].vertices;
					i = 0;
				}
				indices[i++] = first;
				//if (convex != 2) {
				// cw ?
				indices[i++] = (short) (first + k + 4);
				indices[i++] = (short) (first + k + 2);
				//	} else {
				//		indices[i++] = (short) (first + k + 2);
				//		indices[i++] = (short) (first + k + 4);
				//	}

				//	System.out.println("indice:" + k + "\t" + indices[cnt - 3] + "," 
				// + indices[cnt - 2]+ "," + indices[cnt - 1]);

				indicesCnt += 3;
			}
			mCurIndices[2].used = i;
		} else if (len < 400) {
			// triangulate up to 200 points
			short first = (short) (mNumVertices + 1);
			int used = triangulate(points, pos, len, mCurIndices[2], first);
			if (used > 0) {
				indicesCnt += used;
				// find the last item added..
				VertexPoolItem it = mIndices[2];
				while (it.next != null)
					it = it.next;
				mCurIndices[2] = it;
			}

			//			mCurIndices[2].next = VertexPool.get();
			//			mCurIndices[2] = mCurIndices[2].next;
			//			short[] indices = mCurIndices[2].vertices;
			//			int used = triangulate(points, pos, len, indices);
			//			if (used > 0) {
			//				short first = (short) (mNumVertices + 1);
			//				for (int i = 0; i < used; i += 3) {
			//					indices[i] = (short) (indices[i] * 2 + first);
			//					short tmp = indices[i + 1];
			//					indices[i + 1] = (short) (indices[i + 2] * 2 + first);
			//					indices[i + 2] = (short) (tmp * 2 + first);
			//				}
			//				mCurIndices[2].used = used;
			//				indicesCnt += used;
			//			}
		} else
			Log.d(TAG, "skip >>>>>>>>>> : " + len + " <<<<<<<<<<<<<");

		//Log.d(TAG, "add building: " + vertexCnt);
		mNumVertices += vertexCnt;
		mNumIndices += indicesCnt;
	}

	public void compile(ShortBuffer sbuf) {

		if (mNumVertices == 0 || compiled)
			return;

		mVboIds = new int[2];
		GLES20.glGenBuffers(2, mVboIds, 0);
		mIndicesBufferID = mVboIds[0];
		mVertexBufferID = mVboIds[1];

		// upload indices

		sbuf.clear();
		for (int i = 0; i < 3; i++) {
			for (VertexPoolItem vi = mIndices[i]; vi != null; vi = vi.next) {
				//System.out.println("put indices: " + vi.used + " " + mNumIndices);
				sbuf.put(vi.vertices, 0, vi.used);
				mIndiceCnt[i] += vi.used;
			}
		}

		//		Log.d(TAG,"put indices: " + mNumIndices + "=="
		//				+ (mIndiceCnt[0] + mIndiceCnt[1] + mIndiceCnt[2])
		//				+ " " + mIndiceCnt[0] + " " + mIndiceCnt[1] + " " + mIndiceCnt[2]);

		mNumIndices = mIndiceCnt[0] + mIndiceCnt[1] + mIndiceCnt[2];

		sbuf.flip();

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mNumIndices * 2, sbuf, GLES20.GL_DYNAMIC_DRAW);

		sbuf.clear();

		// upload vertices
		for (VertexPoolItem vi = mVertices; vi != null; vi = vi.next) {
			//System.out.println("put vertices: " + vi.used + " " + mNumVertices);
			sbuf.put(vi.vertices, 0, vi.used);
		}

		sbuf.flip();
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				mNumVertices * 4 * 2, sbuf, GLES20.GL_DYNAMIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		for (VertexPoolItem i : mIndices)
			VertexPool.release(i);

		VertexPool.release(mVertices);

		compiled = true;
	}

	public boolean compiled = false;

	int[] mVboIds;

	public boolean ready;

	@Override
	protected void clear() {
		if (compiled) {
			GLES20.glDeleteBuffers(2, mVboIds, 0);
		}
	}

	public void render() {

	}

	private static boolean initialized = false;
	private static ShortBuffer sBuf;
	private static FloatBuffer fBuf;

	public static synchronized int triangulate(float[] points, int pos, int len,
			VertexPoolItem item, int first) {

		int numRings = 1;

		if (!initialized) {
			// FIXME also cleanup on shutdown!
			fBuf = ByteBuffer.allocateDirect(360 * 4).order(ByteOrder.nativeOrder())
					.asFloatBuffer();

			sBuf = ByteBuffer.allocateDirect(720 * 2).order(ByteOrder.nativeOrder())
					.asShortBuffer();

			initialized = true;
		}

		fBuf.clear();
		fBuf.put(points, pos, len);

		sBuf.clear();
		sBuf.put((short) (len >> 1)); // all points
		sBuf.put((short) (len >> 1)); // outer ring
		//sBuf.put((short)4); // inner ring

		int numTris = TriangleJNI.triangulate(fBuf, numRings, sBuf, first);

		int numIndices = numTris * 3;
		sBuf.limit(numIndices);
		sBuf.position(0);

		for (int k = 0, cnt = 0; k < numIndices; k += cnt) {
			cnt = VertexPoolItem.SIZE - item.used;

			if (item.used == VertexPoolItem.SIZE) {
				item.next = VertexPool.get();
				item = item.next;
			}

			if (k + cnt > numIndices)
				cnt = numIndices - k;

			sBuf.get(item.vertices, item.used, cnt);
			item.used += cnt;
		}

		//		sBuf.get(sIndices, 0, numIndices);
		//
		//		short[] indices = item.vertices;
		//		int i = item.used;
		//
		//		for (int k = 0; k < numIndices; k += 3) {
		//			if (i == VertexPoolItem.SIZE) {
		//				item.used = VertexPoolItem.SIZE;
		//				item.next = VertexPool.get();
		//				item = item.next;
		//				indices = item.vertices;
		//				i = 0;
		//			}
		//			indices[i++] = sIndices[k + 0];
		//			indices[i++] = sIndices[k + 1];
		//			indices[i++] = sIndices[k + 2];
		//		}
		//		item.used = i;

		return numIndices;
	}
}
