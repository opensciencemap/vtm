package org.oscim.utils.tess;

import static java.lang.System.out;

import java.io.File;
import java.util.Arrays;

public class Tesselator {

	private long inst;

	public Tesselator() {
		inst = newTess();
	}

	@Override
	protected void finalize() {
		dispose();
	}

	public void dispose() {
		if (inst != 0) {
			freeTess(inst);
			inst = 0;
		}
	}

	/**
	 * See OpenGL Red Book for description of the winding rules
	 * http://www.glprogramming.com/red/chapter11.html
	 */
	public static final class WindingRule {
		public static final int ODD = 0;
		public static final int NONZERO = 1;
		public static final int POSITIVE = 2;
		public static final int NEGATIVE = 3;
		public static final int ABS_GEQ_TWO = 4;
	}

	public static final class ElementType {
		public static final int POLYGONS = 0;
		public static final int CONNECTED_POLYGONS = 1;
		public static final int BOUNDARY_CONTOURS = 2;
	}

	public void addContour2D(float[] points) {
		addContour2D(points, 0, points.length >> 1);
	}

	public void addContour2D(float[] points, int offset, int length) {
		if (length < 6)
			return;
		if ((length % 2 != 0) || (offset % 2 != 0) || (points.length >> 1) < (offset + length))
			throw new IllegalArgumentException("Invalid input: length:" + length
			        + ", offset:" + offset
			        + ", points.length:" + points.length);

		addContour(inst, 2, points, 8, offset, length);
	}

	public void addContour2D(int[] index, float[] contour) {
		addMultiContour2D(inst, index, contour, 0, index.length);
	}

	public boolean tesselate(int windingRule, int elementType) {
		return tessContour2D(inst, windingRule, elementType, 3, 2) == 1;
	}

	public int getVertexCount() {
		return getVertexCount(inst);
	}

	public int getElementCount() {
		return getElementCount(inst);
	}

	public void getVertices(float[] out, int offset, int length) {
		getVertices(inst, out, offset, length);
	}

	public void getVertices(short[] out, int offset, int length, float scale) {
		getVerticesS(inst, out, offset, length, scale);
	}

	public void getElements(int[] out, int offset, int length) {
		getElements(inst, out, offset, length);
	}

	public void getElements(short[] out, int offset, int length) {
		getElementsS(inst, out, offset, length);
	}

	public void getVertexIndices(int[] out, int offset, int length) {
		getVertexIndices(inst, out, offset, length);
	}

	public void getElementsWithInputVertexIds(short[] out, int dstOffset, int offset, int length) {
		getElementsWithInputVertexIds(inst, out, dstOffset, offset, length);
	}

	// @formatter:off
	
	/*JNI
	#include <tesselator.h>
	#include <string.h>
	 */
	private static native long newTess(); /* {
		return (long)tessNewTess(0);
	} */
	
	private static native void freeTess(long inst); /* {
		tessDeleteTess((TESStesselator*) inst);
	} */
	
	/** 
	 * Adds a contour to be tesselated.
	 * The type of the vertex coordinates is assumed to be TESSreal.
	 *
	 * @param tess - pointer to tesselator object.
	 * @param size - number of coordinates per vertex. Must be 2 or 3.
	 * @param pointer - pointer to the first coordinate of the first vertex in the array.
	 * @param stride - defines offset in bytes between consecutive vertices.
	 * @param count - number of vertices in contour. 
	 */
	private static native void addContour(long inst, int size, float[] contour, int stride, int offset, int count);/* {
		tessAddContour((TESStesselator*) inst, size, contour + (offset * stride), stride, count);
	} */
	
	private static native void addMultiContour2D(long inst, int[] index, float[] contour, int idxStart, int idxCount);/* {
		TESStesselator* tess = (TESStesselator*) inst;
		int offset = 0;
		
		for (int i = 0; i < idxStart + idxCount; i++){
			int len = index[i];
			
			if ((len % 2 != 0) || (len < 0))
				break;
				
			if (len < 6 || i < idxStart) {
				offset += len;
				continue;
			}
			
			tessAddContour(tess, 2, contour + offset, 8, len >> 1);
			
			offset += len;
		}
	} */
	
	/**
	 * Tesselate contours.
	 * 
	 * @param tess - pointer to tesselator object.
	 * @param windingRule - winding rules used for tesselation, must be one of TessWindingRule.
	 * @param elementType - defines the tesselation result element type, must be one of TessElementType.
	 * @param polySize - defines maximum vertices per polygons if output is polygons.
	 * @param vertexSize - defines the number of coordinates in tesselation result vertex, must be 2 or 3.
	 * @param normal - defines the normal of the input contours, of null the normal is calculated automatically.
	 * @return 1 if succeed, 0 if failed. 
	 */
	private static native int tessContour2D(long inst, int windingRule, int elementType, int polySize, int vertexSize);/*{
		return tessTesselate((TESStesselator*) inst, windingRule, elementType, polySize, vertexSize, 0);
	} */
	
	private static native int getVertexCount(long inst); /*{
		return tessGetVertexCount((TESStesselator*) inst);
	}*/
	
	/**
	 * Returns pointer to first coordinate of first vertex.
	 */
	private static native boolean getVertices(long inst, float[] out, int offset, int length);/*{
		const TESSIOreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		//const TESSreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		if (!vertices)
			return 0;
			
		memcpy(out, vertices + offset, length * sizeof(TESSIOreal));
		
		//memcpy(out, vertices + offset, length * sizeof(TESSreal));
		
		return 1;
	}*/
	
	/**
	 * Returns pointer to first coordinate of first vertex.
	 */
	private static native void getVerticesS(long inst, short[] out, int offset, int length, float scale);/*{
		const TESSIOreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		//const TESSreal* vertices = tessGetVertices((TESStesselator*) inst);
		
		for(int i = 0; i < length; i++)
			out[i] = (short)(vertices[offset++] * scale + 0.5f);
	}*/
	
	/**
	 * Returns pointer to first vertex index.
	 * 
	 * Vertex indices can be used to map the generated vertices to the original vertices.
	 * Every point added using tessAddContour() will get a new index starting at 0.
	 * New vertices generated at the intersections of segments are assigned value TESS_UNDEF.
	 */
	private static native boolean getVertexIndices(long inst, int[] out, int offset, int length);/* {
		const TESSindex* indices = tessGetVertexIndices((TESStesselator*) inst);
		if (!indices)
			return 0;
			
		memcpy(out, indices + offset, length * sizeof(TESSindex));
		return 1;
	} */
	
	/**
	 * Returns number of elements in the the tesselated output.
	 */
	private static native int getElementCount(long inst);/*{
		return tessGetElementCount((TESStesselator*) inst);
	}*/
	
	/**
	 * Returns pointer to the first element. 
	 */
	private static native boolean getElements(long inst, int[] out, int offset, int length);/*{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		if (!elements)
			return 0;
			
		memcpy(out, elements + offset, length * sizeof(TESSindex));
		return 1;
	}*/
	
	
	/**
	 * Returns pointer to the first element. 
	 */
	private static native void getElementsS(long inst, short[] out, int offset, int length);/*{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		for(int i = 0; i < length; i++)
			out[i] = (short)elements[offset++];
	}*/
	
	/**
	 * Returns list of triangles indices (or to the first element of convex polygons). 
	 */
	private static native void getElementsWithInputVertexIds(long inst, short[] out, int dstOffset, int offset, int length);/*{
		const TESSindex* elements = tessGetElements((TESStesselator*) inst);
		const TESSindex* indices = tessGetVertexIndices((TESStesselator*) inst);
		
		for(int i = 0; i < length; i++)
			out[dstOffset++] = (short)indices[elements[offset++]];
	}*/
	
	//@formatter:on
	public static void main(String[] args) throws Exception {
		System.load(new File("libs/linux64/libvtm-jni64.so").getAbsolutePath());
		Tesselator tess = new Tesselator();

		float[] c1 = new float[] {
		        0, 0,
		        4, 0,
		        4, 4,
		        0, 4,
		};

		float[] c2 = new float[] {
		        1, 1,
		        1, 2,
		        2, 2,
		        2, 1
		};

		float[] c3 = new float[] {
		        0, 0,
		        4, 0,
		        4, 4,
		        0, 4,

		        1, 1,
		        1, 2,
		        2, 2,
		        2, 1
		};

		int polySize = 3;

		addContour(tess.inst, 2, c1, 8, 0, c1.length / 2);

		//addContour(tess.inst, 2, c2, 8, c2.length / 2);

		//addContour(tess.inst, 2, c3, 8, c3.length / 2);

		//int[] index = { 8, 8, -1 };

		//addMultiContour2D(tess.inst, index, c3, 0, 4);
		out.println("yup");

		tessContour2D(tess.inst,
		              WindingRule.POSITIVE,
		              ElementType.POLYGONS,
		              polySize, 2);

		out.println("y0!");

		int nElem = getElementCount(tess.inst);
		int nVert = getVertexCount(tess.inst);

		out.println(nVert + "/" + nElem);

		short[] elems = new short[nElem * polySize];
		getElementsS(tess.inst, elems, 0, nElem * polySize);

		out.println(Arrays.toString(elems));

		int half = nElem * polySize / 2;
		elems = new short[half];
		getElementsS(tess.inst, elems, half, half);

		out.println(Arrays.toString(elems));

		float[] verts = new float[nVert * 2];
		getVertices(tess.inst, verts, 0, nVert * 2);

		out.println(Arrays.toString(verts));

		short[] ids = new short[nElem * polySize];
		getElementsWithInputVertexIds(tess.inst, ids, 0, 0, ids.length);

		out.println(Arrays.toString(ids));

		for (int i = 0; i < nElem * polySize; i++) {
			out.println(c3[ids[i] * 2] + ", " + c3[ids[i] * 2 + 1]);
		}
		out.println();

		freeTess(tess.inst);
	}
}
