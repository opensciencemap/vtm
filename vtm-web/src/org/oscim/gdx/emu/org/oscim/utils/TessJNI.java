package org.oscim.utils;

public class TessJNI {

    public TessJNI() {

    }

    public TessJNI(int size) {
        throw new RuntimeException("unimplemented");
    }

    public void dispose() {
        throw new RuntimeException("unimplemented");
    }

    public void addContour2D(float[] points) {
        throw new RuntimeException("unimplemented");
    }

    public void addContour2D(float[] points, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

    public void addContour2D(int[] index, float[] contour) {
        throw new RuntimeException("unimplemented");
    }

    public void addContour2D(int[] index, float[] contour, int idxStart, int idxEnd) {
        throw new RuntimeException("unimplemented");
    }

    public boolean tesselate() {
        throw new RuntimeException("unimplemented");
    }

    public boolean tesselate(int windingRule, int elementType) {
        throw new RuntimeException("unimplemented");
    }

    public int getVertexCount() {
        throw new RuntimeException("unimplemented");
    }

    public int getElementCount() {
        throw new RuntimeException("unimplemented");
    }

    public void getVertices(float[] out, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

    public void getVertices(short[] out, int offset, int length, float scale) {
        throw new RuntimeException("unimplemented");
    }

    public void getElements(int[] out, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

    public void getElements(short[] out, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

    public void getVertexIndices(int[] out, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

    public void getElementsWithInputVertexIds(short[] dst, int dstOffset, int offset, int length) {
        throw new RuntimeException("unimplemented");
    }

}
