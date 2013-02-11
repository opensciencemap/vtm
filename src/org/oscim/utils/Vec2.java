package org.oscim.utils;




public final class Vec2 {

	public static void set(float[] v, int pos, float x, float y){
		v[(pos << 1) + 0] = x;
		v[(pos << 1) + 1] = y;
	}

	public static float dot(float[] a, int apos, float[] b, int bpos) {
		return a[apos << 1] * b[bpos << 1] + a[(apos << 1) + 1] * b[(bpos << 1) + 1];
	}

	public final static float lengthSquared(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		return x * x + y * y;
	}

	public final static void normalizeSquared(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		float length = x * x + y * y;

		v[(pos << 1) + 0] = x / length;
		v[(pos << 1) + 1] = y / length;
	}

	public final static void normalize(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		double length = Math.sqrt(x * x + y * y);

		v[(pos << 1) + 0] = (float)(x / length);
		v[(pos << 1) + 1] = (float)(y / length);
	}

	public final static float length(float[] v, int pos) {
		float x = v[(pos << 1) + 0];
		float y = v[(pos << 1) + 1];

		return (float) Math.sqrt(x * x + y * y);
	}

	public final static void add(float[] result, int rpos, float[] a, int apos, float[] b, int bpos) {
		result[(rpos << 1) + 0] = a[(apos << 1) + 0] + b[(bpos << 1) + 0];
		result[(rpos << 1) + 1] = a[(apos << 1) + 1] + b[(bpos << 1) + 1];
	}

	public final static void sub(float[] result, int rpos, float[] a, int apos, float[] b, int bpos) {
		result[(rpos << 1) + 0] = a[(apos << 1) + 0] - b[(bpos << 1) + 0];
		result[(rpos << 1) + 1] = a[(apos << 1) + 1] - b[(bpos << 1) + 1];
	}

	public final static void mul(float[] v, int pos, float a){
		v[(pos << 1) + 0] *= a;
		v[(pos << 1) + 1] *= a;
	}

}
