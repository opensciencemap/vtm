package org.oscim.utils;

import org.oscim.renderer.elements.VertexItem;

public class Tessellator {
	private static final int RESULT_VERTICES = 0;
	//private static final int RESULT_TRIANGLES = 1;

	private static final short[] coordinates = new short[720];

	public static synchronized int triangulate(float[] points, int ppos, int plen, short[] index,
	        int ipos, int rings, int vertexOffset, VertexItem outTris) {

		int[] result = new int[2];

		int numPoints = 0;
		for (int i = 0; i < rings; i++)
			numPoints += index[ipos + i];

		long ctx = Tessellator.tessellate(points, ppos, index, ipos, rings, result);
		if ((numPoints / 2) < result[RESULT_VERTICES]) {
			//Log.d(TAG, "nup" + Arrays.toString(result) + " " + numPoints);
			Tessellator.tessFinish(ctx);
			return 0;
		}

		//while (Tessellator.tessGetCoordinates(ctx, coordinates, 2) > 0) {
		//	Log.d(TAG, Arrays.toString(coordinates));
		//}

		int cnt;
		int numIndices = 0;

		while ((cnt = Tessellator.tessGetIndices(ctx, coordinates)) > 0) {
			//if (cnt > (VertexItem.SIZE - outTris.used))
			//	Log.d(TAG, "ok" + Arrays.toString(result));

			//Log.d(TAG,Arrays.toString(coordinates));
			numIndices += cnt;

			for (int j = 0; j < cnt; j++)
				coordinates[j] *= 2;

			// when a ring has an odd number of points one (or rather two)
			// additional vertices will be added. so the following rings
			// needs extra offset
			int shift = 0;
			for (int i = 0, m = rings - 1; i < m; i++) {
				shift += (index[ipos + i]);

				// even number of points?
				if (((index[ipos + i] >> 1) & 1) == 0)
					continue;

				for (int j = 0; j < cnt; j++)
					if (coordinates[j] >= shift)
						coordinates[j] += 2;

				shift += 2;
			}

			for (int j = 0; j < cnt;) {
				int outPos = outTris.used;
				short[] v = outTris.vertices;

				if (outPos == VertexItem.SIZE) {
					outTris.next = VertexItem.pool.get();
					outTris = outTris.next;
					v = outTris.vertices;
					outPos = 0;
				}

				// shift to vertex offset
				v[outPos++] = (short) (vertexOffset + coordinates[j++]);
				v[outPos++] = (short) (vertexOffset + coordinates[j++]);
				v[outPos++] = (short) (vertexOffset + coordinates[j++]);
				outTris.used = outPos;
			}
		}

		Tessellator.tessFinish(ctx);

		return numIndices;
	}

	/**
	 * @param points an array of x,y coordinates
	 * @param pos position in points array
	 * @param index geom indices
	 * @param ipos position in index array
	 * @param numRings number of rings in polygon == outer(1) + inner rings
	 * @param result contains number of vertices and number of triangles
	 * @return context - must be freed with tessFinish()
	 */
	public static native long tessellate(float[] points, int pos,
	        short[] index, int ipos, int numRings, int[] result);

	public static native void tessFinish(long ctx);

	public static native int tessGetCoordinates(long ctx, short[] coordinates, float scale);

	public static native int tessGetIndices(long ctx, short[] indices);
}
