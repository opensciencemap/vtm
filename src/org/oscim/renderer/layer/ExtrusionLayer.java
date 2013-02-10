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

import org.oscim.core.Tile;
import org.oscim.renderer.BufferObject;
import org.oscim.renderer.GLRenderer;
import org.oscim.utils.LineClipper;
import org.oscim.view.MapView;
import org.quake.triangle.TriangleJNI;

import android.opengl.GLES20;
import android.util.Log;

/**
 * @author Hannes Janetzek
 *         FIXME check if polygon has self intersections or 0/180 degree
 *         angles! or bad things might happen in Triangle
 */
public class ExtrusionLayer extends Layer {
	private final static String TAG = ExtrusionLayer.class.getName();
	private static final float S = GLRenderer.COORD_MULTIPLIER;
	private int mNumVertices = 0;
	private final VertexPoolItem mVertices;
	private VertexPoolItem mCurVertices;
	private final VertexPoolItem mIndices[], mCurIndices[];
	private LineClipper mClipper;

	// indices for:
	// 0. even sides, 1. odd sides, 2. roof, 3. roof outline
	public int mIndiceCnt[] = { 0, 0, 0, 0 };
	public int mNumIndices = 0;

	public int mIndicesBufferID;
	public int mVertexBufferID;
	private BufferObject mIndiceBO;
	private BufferObject mVertexBO;

	//private final static int IND_EVEN_SIDE = 0;
	//private final static int IND_ODD_SIDE = 1;
	private final static int IND_ROOF = 2;
	private final static int IND_OUTLINE = 3;

	public boolean compiled = false;

	//private int[] mVboIds;

	public ExtrusionLayer(int level) {
		this.type = Layer.EXTRUSION;
		this.layer = level;

		mVertices = mCurVertices = VertexPool.get();

		mIndices = new VertexPoolItem[4];
		mCurIndices = new VertexPoolItem[4];
		for (int i = 0; i < 4; i++)
			mIndices[i] = mCurIndices[i] = VertexPool.get();

		mClipper = new LineClipper(0, 0, Tile.TILE_SIZE, Tile.TILE_SIZE);
	}

	public void addBuildings(float[] points, short[] index, int height) {

		// start outer ring
		int outer = 0;

		boolean simple = true;
		int startVertex = mNumVertices;

		// just a guessing to make it look ok
		if (height == 0)
			height = 10;
		height = (int) (height * -Math.log(height / 100000f) * 3.6f);

		int length = 0;
		for (int ipos = 0, ppos = 0, n = index.length; ipos < n; ipos++, ppos += length) {
			length = index[ipos];

			// end marker
			if (length < 0)
				break;

			// start next polygon
			if (length == 0) {
				outer = ipos + 1;
				startVertex = mNumVertices;
				simple = true;
				continue;
			}

			// check: drop last point from explicitly closed rings
			int len = length;
			if (!MapView.enableClosePolygons) {
				len -= 2;
			} else if (points[ppos] == points[ppos + len - 2]
					&& points[ppos + 1] == points[ppos + len - 1]) {
				// vector-tile-map does not produce implicty closed
				// polygons (yet)
				len -= 2;
			}

			// need at least three points
			if (len < 6)
				continue;

			// check if polygon contains inner rings
			if (simple && (ipos < n - 1) && (index[ipos + 1] > 0))
				simple = false;

			boolean convex = addOutline(points, ppos, len, height, simple);

			if (simple && (convex || len <= 8))
				addRoofSimple(startVertex, len);
			else if (ipos == outer) { // add roof only once
				addRoof(startVertex, index, ipos, points, ppos);
			}
		}
	}

	private void addRoofSimple(int startVertex, int len) {

		// roof indices for convex shapes
		int i = mCurIndices[IND_ROOF].used;
		short[] indices = mCurIndices[IND_ROOF].vertices;
		short first = (short) (startVertex + 1);

		for (int k = 0; k < len - 4; k += 2) {
			if (i == VertexPoolItem.SIZE) {
				mCurIndices[IND_ROOF].used = VertexPoolItem.SIZE;
				mCurIndices[IND_ROOF].next = VertexPool.get();
				mCurIndices[IND_ROOF] = mCurIndices[2].next;
				indices = mCurIndices[IND_ROOF].vertices;
				i = 0;
			}
			indices[i++] = first;
			indices[i++] = (short) (first + k + 2);
			indices[i++] = (short) (first + k + 4);
		}
		mCurIndices[IND_ROOF].used = i;
	}

	private void addRoof(int startVertex, short[] index, int ipos, float[] points, int ppos) {
		int len = 0;
		int rings = 0;

		// get sum of points in polygon
		for (int i = ipos, n = index.length; i < n && index[i] > 0; i++) {
			len += index[i];
			rings++;
		}

		// triangulate up to 600 points (limited only by prepared buffers)
		// some buildings in paris have even more...
		if (len > 1200) {
			Log.d(TAG, ">>> skip building : " + len + " <<<");
			return;
		}

		int used = triangulate(points, ppos, len, index, ipos, rings,
				startVertex + 1, mCurIndices[IND_ROOF]);

		if (used > 0) {
			// get back to the last item added..
			VertexPoolItem it = mIndices[IND_ROOF];
			while (it.next != null)
				it = it.next;
			mCurIndices[IND_ROOF] = it;
		}
	}

	private boolean addOutline(float[] points, int pos, int len, float height,
			boolean convex) {

		// add two vertices for last face to make zigzag indices work
		boolean addFace = (len % 4 != 0);
		int vertexCnt = len + (addFace ? 2 : 0);

		short h = (short) height;

		float cx = points[pos + len - 2];
		float cy = points[pos + len - 1];
		float nx = points[pos + 0];
		float ny = points[pos + 1];

		// vector to next point
		float vx = nx - cx;
		float vy = ny - cy;
		// vector from previous point
		float ux, uy;

		float a = (float) Math.sqrt(vx * vx + vy * vy);
		short color1 = (short) ((1 + vx / a) * 127);
		short fcolor = color1;
		short color2 = 0;

		int even = 0;
		int changeX = 0;
		int changeY = 0;

		// vertex offset for all vertices in layer
		int vOffset = mNumVertices;

		short[] vertices = mCurVertices.vertices;
		int v = mCurVertices.used;

		mClipper.clipStart((int) nx, (int) ny);

		for (int i = 2, n = vertexCnt + 2; i < n; i += 2, v += 8) {
			cx = nx;
			cy = ny;

			ux = vx;
			uy = vy;

			/* add bottom and top vertex for each point */
			if (v == VertexPoolItem.SIZE) {
				mCurVertices.used = VertexPoolItem.SIZE;
				mCurVertices.next = VertexPool.get();
				mCurVertices = mCurVertices.next;
				vertices = mCurVertices.vertices;
				v = 0;
			}

			// set coordinate
			vertices[v + 0] = vertices[v + 4] = (short) (cx * S);
			vertices[v + 1] = vertices[v + 5] = (short) (cy * S);

			// set height
			vertices[v + 2] = 0;
			vertices[v + 6] = h;

			// get direction to next point
			if (i < len) {
				nx = points[pos + i + 0];
				ny = points[pos + i + 1];
			} else if (i == len) {
				nx = points[pos + 0];
				ny = points[pos + 1];
			} else { // if (addFace)
				short c = (short) (color1 | fcolor << 8);
				vertices[v + 3] = vertices[v + 7] = c;
				v += 8;
				break;
			}

			vx = nx - cx;
			vy = ny - cy;

			// set lighting (by direction)
			a = (float) Math.sqrt(vx * vx + vy * vy);
			color2 = (short) ((1 + vx / a) * 127);

			short c;
			if (even == 0)
				c = (short) (color1 | color2 << 8);
			else
				c = (short) (color2 | color1 << 8);

			vertices[v + 3] = vertices[v + 7] = c;
			color1 = color2;

			/* check if polygon is convex */
			if (convex) {
				// TODO simple polys with only one concave arc
				// could be handled without special triangulation
				if ((ux < 0 ? 1 : -1) != (vx < 0 ? 1 : -1))
					changeX++;
				if ((uy < 0 ? 1 : -1) != (vy < 0 ? 1 : -1))
					changeY++;

				if (changeX > 2 || changeY > 2)
					convex = false;
			}

			/* check if face is within tile */
			if (!mClipper.clipNext((int) nx, (int) ny)) {
				even = (even == 0 ? 1 : 0);
				continue;
			}

			/* add ZigZagQuadIndices(tm) for sides */
			short vert = (short) (vOffset + (i - 2));
			short s0 = vert++;
			short s1 = vert++;
			short s2 = vert++;
			short s3 = vert++;

			// connect last to first (when number of faces is even)
			if (!addFace && i == len) {
				s2 -= len;
				s3 -= len;
			}

			short[] indices = mCurIndices[even].vertices;
			// index id relative to mCurIndices item
			int ind = mCurIndices[even].used;

			if (ind == VertexPoolItem.SIZE) {
				mCurIndices[even].next = VertexPool.get();
				mCurIndices[even] = mCurIndices[even].next;
				indices = mCurIndices[even].vertices;
				ind = 0;
			}

			indices[ind + 0] = s0;
			indices[ind + 1] = s2;
			indices[ind + 2] = s1;

			indices[ind + 3] = s1;
			indices[ind + 4] = s2;
			indices[ind + 5] = s3;

			mCurIndices[even].used += 6;
			even = (even == 0 ? 1 : 0);

			/* add roof outline indices */
			VertexPoolItem it = mCurIndices[IND_OUTLINE];
			if (it.used == VertexPoolItem.SIZE) {
				it.next = VertexPool.get();
				it = mCurIndices[IND_OUTLINE] = it.next;
			}
			it.vertices[it.used++] = s1;
			it.vertices[it.used++] = s3;
		}

		mCurVertices.used = v;

		mNumVertices += vertexCnt;
		return convex;
	}

	public void compile(ShortBuffer sbuf) {

		if (mNumVertices == 0 || compiled)
			return;

		mVertexBO = BufferObject.get(0);
		mIndiceBO = BufferObject.get(0);
		mIndicesBufferID = mIndiceBO.id;
		mVertexBufferID = mVertexBO.id;

		// upload indices
		sbuf.clear();
		mNumIndices = 0;
		for (int i = 0; i < 4; i++) {
			for (VertexPoolItem vi = mIndices[i]; vi != null; vi = vi.next) {
				sbuf.put(vi.vertices, 0, vi.used);
				mIndiceCnt[i] += vi.used;
			}
			mNumIndices += mIndiceCnt[i];
		}

		sbuf.flip();
		mIndiceBO.size = mNumIndices * 2;
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, mIndicesBufferID);
		GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER,
				mIndiceBO.size, sbuf, GLES20.GL_DYNAMIC_DRAW);

		// upload vertices
		sbuf.clear();
		for (VertexPoolItem vi = mVertices; vi != null; vi = vi.next)
			sbuf.put(vi.vertices, 0, vi.used);

		sbuf.flip();
		mVertexBO.size = mNumVertices * 4 * 2;
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mVertexBufferID);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
				mVertexBO.size, sbuf, GLES20.GL_DYNAMIC_DRAW);

		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		for (VertexPoolItem i : mIndices)
			VertexPool.release(i);

		VertexPool.release(mVertices);

		mClipper = null;

		compiled = true;
	}

	@Override
	protected void clear() {
		if (compiled) {
			BufferObject.release(mIndiceBO);
			BufferObject.release(mVertexBO);
			mIndiceBO = null;
			mVertexBO = null;
			//GLES20.glDeleteBuffers(2, mVboIds, 0);
		} else {
			VertexPool.release(mVertices);
			for (VertexPoolItem i : mIndices)
				VertexPool.release(i);
		}
	}

	private static boolean initialized = false;
	private static ShortBuffer sBuf;
	private static FloatBuffer fBuf;

	public static synchronized int triangulate(float[] points, int ppos, int plen, short[] index,
			int ipos, int rings, int vertexOffset, VertexPoolItem item) {

		if (!initialized) {
			// FIXME also cleanup on shutdown!
			fBuf = ByteBuffer.allocateDirect(1200 * 4).order(ByteOrder.nativeOrder())
					.asFloatBuffer();

			sBuf = ByteBuffer.allocateDirect(1800 * 2).order(ByteOrder.nativeOrder())
					.asShortBuffer();

			initialized = true;
		}

		fBuf.clear();
		fBuf.put(points, ppos, plen);

		sBuf.clear();

		sBuf.put((short) plen); // all points
		sBuf.put(index, ipos, rings);

		int numTris = TriangleJNI.triangulate(fBuf, rings, sBuf, vertexOffset);

		int numIndices = numTris * 3;
		sBuf.limit(numIndices);
		sBuf.position(0);

		for (int k = 0, cnt = 0; k < numIndices; k += cnt) {

			if (item.used == VertexPoolItem.SIZE) {
				item.next = VertexPool.get();
				item = item.next;
			}

			cnt = VertexPoolItem.SIZE - item.used;

			if (k + cnt > numIndices)
				cnt = numIndices - k;

			sBuf.get(item.vertices, item.used, cnt);
			item.used += cnt;
		}

		return numIndices;
	}
}
