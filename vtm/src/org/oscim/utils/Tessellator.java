package org.oscim.utils;

import java.util.Arrays;

import org.oscim.core.GeometryBuffer;
import org.oscim.renderer.elements.VertexItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tessellator {
	static final Logger log = LoggerFactory.getLogger(Tessellator.class);

	private static final int RESULT_VERTICES = 0;
	private static final int RESULT_TRIANGLES = 1;

	/**
	 * Special version for ExtrusionLayer to match indices with vertex
	 * positions.
	 * 
	 * @param points
	 * @param ppos
	 * @param plen
	 * @param index
	 * @param ipos
	 * @param rings
	 * @param vertexOffset
	 * @param outTris
	 * @return
	 */
	public static int tessellate(float[] points, int ppos, int plen, short[] index,
	        int ipos, int rings, int vertexOffset, VertexItem outTris) {

		int[] result = new int[2];

		int numPoints = 0;
		for (int i = 0; i < rings; i++)
			numPoints += index[ipos + i];

		long ctx = Tessellator.tessellate(points, ppos, index, ipos, rings, result);
		if ((numPoints / 2) < result[RESULT_VERTICES]) {
			log.debug("skip poly: " + Arrays.toString(result) + " " + numPoints);
			Tessellator.tessFinish(ctx);
			return 0;
		}

		int cnt;
		int numIndices = 0;

		if (outTris.used == VertexItem.SIZE)
			outTris = VertexItem.pool.getNext(outTris);

		while ((cnt = Tessellator.tessGetIndicesWO(ctx,
		                                           outTris.vertices,
		                                           outTris.used)) > 0) {
			int start = outTris.used;
			int end = start + cnt;
			short[] v = outTris.vertices;

			for (int i = start; i < end; i++)
				v[i] *= 2;

			// when a ring has an odd number of points one (or rather two)
			// additional vertices will be added. so the following rings
			// needs extra offset
			int shift = 0;
			for (int i = 0, m = rings - 1; i < m; i++) {
				shift += (index[ipos + i]);

				// even number of points?
				if (((index[ipos + i] >> 1) & 1) == 0)
					continue;

				for (int j = start; j < end; j++)
					if (v[j] >= shift)
						v[j] += 2;

				shift += 2;
			}

			// shift by vertexOffset
			for (int i = start; i < end; i++)
				v[i] += vertexOffset;

			outTris.used += cnt;
			numIndices += cnt;

			if (outTris.used == VertexItem.SIZE) {
				outTris = VertexItem.pool.getNext(outTris);
				continue;
			}

			// no more indices to get.
			break;
		}

		Tessellator.tessFinish(ctx);

		return numIndices;
	}

	/**
	 * Untested!
	 * 
	 * @param geom
	 * @param out
	 * @return
	 */
	public static int tessellate(GeometryBuffer geom, GeometryBuffer out) {

		int[] result = new int[2];

		//int numPoints = 0;
		//for (int i = 0; i < rings; i++)
		//	numPoints += index[ipos + i];
		int numRings = 0;
		int numPoints = 0;

		for (int i = 0; i < geom.indexPos; i++) {
			if (geom.index[i] > 0) {
				numRings++;
				numPoints += geom.index[i];
			} else
				break;
		}

		long ctx = Tessellator.tessellate(geom.points, 0,
		                                  geom.index, 0,
		                                  numRings, result);

		//log.debug("got "
		//        + result[RESULT_VERTICES] + " "
		//        + result[RESULT_TRIANGLES]);

		boolean verticesAdded = false;
		if (numPoints < result[RESULT_VERTICES] * 2) {
			//log.debug("grow vertices" + geom.pointPos);
			verticesAdded = true;
		}

		if (out == null) {
			// overwrite geom contents
			out = geom;
			if (verticesAdded) {
				out.ensurePointSize(result[RESULT_VERTICES], false);
				Tessellator.tessGetVerticesFloat(ctx, out.points);
			}
		} else {
			out.ensurePointSize(result[RESULT_VERTICES], false);

			if (verticesAdded) {
				Tessellator.tessGetVerticesFloat(ctx, out.points);
			} else {
				System.arraycopy(geom.points, 0, out.points, 0, numPoints);
			}
		}

		out.ensureIndexSize(result[RESULT_TRIANGLES * 3], false);
		Tessellator.tessGetIndices(ctx, out.index);

		Tessellator.tessFinish(ctx);

		return 1;
	}

	/**
	 * 
	 * @param geom
	 * @param scale
	 * @param outPoints
	 * @param outTris
	 * @param vertexOffset
	 * @return
	 */
	public static int tessellate(GeometryBuffer geom, float scale,
	        VertexItem outPoints, VertexItem outTris, int vertexOffset) {

		int[] result = new int[2];

		//int numPoints = 0;
		//for (int i = 0; i < rings; i++)
		//	numPoints += index[ipos + i];

		int numRings = 0;
		int numPoints = 0;

		for (int i = 0; i <= geom.indexPos; i++) {
			if (geom.index[i] <= 0)
				break;

			numRings++;
			numPoints += (geom.index[i]) / 2;
		}

		if (numRings == 0 || numPoints == 0) {
			log.debug("missing " + numPoints + ":" + numRings);
			return 0;
		}

		long ctx = Tessellator.tessellate(geom.points, 0,
		                                  geom.index, 0,
		                                  numRings, result);

		if (numPoints >= result[RESULT_VERTICES]) {
			// TODO use vertices from geom.points
		}

		if (outPoints.used == VertexItem.SIZE) {
			outPoints = VertexItem.pool.getNext(outPoints);
		}

		int cnt;

		while ((cnt = Tessellator.tessGetVerticesWO(ctx,
		                                            outPoints.vertices,
		                                            outPoints.used,
		                                            scale)) > 0) {
			outPoints.used += cnt;

			if (outPoints.used == VertexItem.SIZE) {
				outPoints = VertexItem.pool.getNext(outPoints);
				continue;
			}
			// no more points to get.
			break;
		}

		int numIndices = 0;

		if (outTris.used == VertexItem.SIZE) {
			outTris = VertexItem.pool.getNext(outTris);
		}

		while ((cnt = Tessellator.tessGetIndicesWO(ctx,
		                                           outTris.vertices,
		                                           outTris.used)) > 0) {
			// shift by vertexOffset
			for (int pos = outTris.used, end = pos + cnt; pos < end; pos++)
				outTris.vertices[pos] += vertexOffset;

			outTris.used += cnt;
			numIndices += cnt;

			if (outTris.used == VertexItem.SIZE) {
				outTris = VertexItem.pool.getNext(outTris);
				continue;
			}

			// no more indices to get.
			break;
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
	protected static native long tessellate(float[] points, int pos,
	        short[] index, int ipos, int numRings, int[] result);

	protected static native void tessFinish(long ctx);

	protected static native int tessGetVertices(long ctx, short[] coordinates, float scale);

	protected static native int tessGetVerticesWO(long ctx, short[] coordinates,
	        int offset, float scale);

	protected static native int tessGetVerticesFloat(long ctx, float[] coordinates);

	protected static native int tessGetIndices(long ctx, short[] indices);

	protected static native int tessGetIndicesWO(long ctx, short[] indices, int offset);
}
