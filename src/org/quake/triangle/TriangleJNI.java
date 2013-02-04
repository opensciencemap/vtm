package org.quake.triangle;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class TriangleJNI {
	public TriangleJNI() {

	}

	//	private static boolean initialized = false;
	//
	//	private static ShortBuffer sBuf;
	//	private static FloatBuffer fBuf;
	//
	//	public static synchronized int triangulate(float[] points, int pos, int len, short[] indices) {
	//
	//		int numRings = 1;
	//
	//		if (!initialized) {
	//			fBuf = ByteBuffer.allocateDirect(360 * 4).order(ByteOrder.nativeOrder())
	//					.asFloatBuffer();
	//			sBuf = ByteBuffer.allocateDirect(360 * 2).order(ByteOrder.nativeOrder())
	//					.asShortBuffer();
	//			initialized = true;
	//		}
	//
	//		fBuf.clear();
	//		fBuf.put(points, pos, len);
	//
	//		sBuf.clear();
	//		sBuf.put((short) (len >> 1)); // all points
	//		sBuf.put((short) (len >> 1)); // outer ring
	//		//sBuf.put((short)4); // inner ring
	//
	//		int numTris = TriangleJNI.triangulate(fBuf, numRings, sBuf);
	//		if (numTris > 100)
	//			Log.d("triangle", "Triangles: " + numTris);
	//
	//		sBuf.limit(numTris * 3);
	//		sBuf.position(0);
	//
	//		//		for(int i = 0; i < numTris * 3; i+=3){
	//		//			Log.d("triangle", ">>" + sBuf.get()+ " "+ sBuf.get() + " "+ sBuf.get());
	//		//		}
	//
	//		sBuf.get(indices, 0, numTris * 3);
	//
	//		return numTris * 3;
	//	}

	/**
	 * !!! NOT for general use!!! - this is specifically for ExtrusionLayer
	 * .
	 *
	 * @param points points to use: array of x,y coordinates
	 * @param numRings number of rings in polygon == outer(1) + inner rings
	 * @param io input: 1. number of all points, 2.. number of points in rings -
	 *            times 2!
	 *            output: indices of triangles, 3 per triangle :) (indices use
	 *            stride=2, i.e. 0,2,4...)
	 * @param ioffset offset used to add offset to indices
	 * @return number of triangles in io buffer
	 */
	public static native int triangulate(FloatBuffer points, int numRings, ShortBuffer io,
			int ioffset);

	static {
		System.loadLibrary("triangle-jni");
	}
}
