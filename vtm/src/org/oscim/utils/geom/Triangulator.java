package org.oscim.utils.geom;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import org.oscim.renderer.elements.VertexItem;

public class Triangulator {
	private static boolean initialized = false;
	private static ShortBuffer sBuf;

	public static synchronized int triangulate(float[] points, int ppos, int plen, short[] index,
			int ipos, int rings, int vertexOffset, VertexItem outTris) {

		if (!initialized) {
			// FIXME also cleanup on shutdown!
			sBuf = ByteBuffer.allocateDirect(1800 * 2).order(ByteOrder.nativeOrder())
					.asShortBuffer();

			initialized = true;
		}

		sBuf.clear();
		sBuf.put(index, ipos, rings);

		int numTris = triangulate(points, ppos, plen, rings, sBuf, vertexOffset);

		int numIndices = numTris * 3;
		sBuf.limit(numIndices);
		sBuf.position(0);

		for (int k = 0, cnt = 0; k < numIndices; k += cnt) {

			if (outTris.used == VertexItem.SIZE) {
				outTris.next = VertexItem.pool.get();
				outTris = outTris.next;
			}

			cnt = VertexItem.SIZE - outTris.used;

			if (k + cnt > numIndices)
				cnt = numIndices - k;

			sBuf.get(outTris.vertices, outTris.used, cnt);
			outTris.used += cnt;
		}

		return numIndices;
	}

	/**
	 * @param points an array of x,y coordinates
	 * @param pos position in points array
	 * @param len number of points * 2 (i.e. values to read)
	 * @param numRings number of rings in polygon == outer(1) + inner rings
	 * @param io input: number of points in rings - times 2!
	 *            output: indices of triangles, 3 per triangle :) (indices use
	 *            stride=2, i.e. 0,2,4...)
	 * @param ioffset offset used to add offset to indices
	 * @return number of triangles in io buffer
	 */
	public static native int triangulate(float[] points, int pos, int len, int numRings,
			ShortBuffer io,	int ioffset) /*-{
			return 0;
	}-*/;
}
