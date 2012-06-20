package org.mapsforge.android.utils;

/**
 * Copyright (c) 2009-2010 jMonkeyEngine All rights reserved. FastMath.java
 */
public class FastMath {

	private static final byte b0x7c = (byte) 0x7c;
	private static final byte b0x00 = (byte) 0x00;
	private static final byte b0x01 = (byte) 0x01;
	private static final byte b0xfc = (byte) 0xfc;
	private static final byte b0x80 = (byte) 0x80;
	private static final byte b0x7b = (byte) 0x7b;
	private static final byte b0xff = (byte) 0xff;
	private static final byte b0xfb = (byte) 0xfb;
	private static final float FLOAT_HALF_PREC = 5.96046E-8f;
	private static final float FLOAT_HALF_MAX = 65504f;

	/**
	 * @param flt
	 *            ...
	 * @param data
	 *            ...
	 * @param pos
	 *            ..
	 */
	public static void convertFloatToHalf(float flt, byte[] data, int pos) {
		if (flt == 0f) {
			data[pos + 1] = b0x00;
			data[pos + 0] = b0x00;
		} else if (flt == -0f) {
			data[pos + 1] = b0x80;
			data[pos + 0] = b0x00;
		} else if (flt > FLOAT_HALF_MAX) {
			if (flt == Float.POSITIVE_INFINITY) {
				data[pos + 1] = b0x7c;
				data[pos + 0] = b0x00;
			} else {
				data[pos + 1] = b0x7b;
				data[pos + 0] = b0xff;
			}
		} else if (flt < -FLOAT_HALF_MAX) {
			if (flt == Float.NEGATIVE_INFINITY) {
				data[pos + 1] = b0xfc;
				data[pos + 0] = b0x00;
			} else {
				data[pos + 1] = b0xfb;
				data[pos + 0] = b0xff;
			}
		} else if (flt > 0f && flt < FLOAT_HALF_PREC) {
			data[pos + 1] = b0x00;
			data[pos + 0] = b0x01;
		} else if (flt < 0f && flt > -FLOAT_HALF_PREC) {
			data[pos + 1] = b0x80;
			data[pos + 0] = b0x01;
		} else {
			int f = Float.floatToIntBits(flt);

			if (f == 0x7fc00000)
				throw new UnsupportedOperationException(
						"NaN to half conversion not supported!");

			data[pos + 1] = (byte) (((f >> 24) & 0x80)
					| ((((f & 0x7f800000) - 0x38000000) >> 21) & 0x7c)
					| ((f >> 21) & 0x03));

			data[pos + 0] = (byte) ((f >> 13) & 0xff);
		}
	}

	/**
	 * @param flt
	 *            ...
	 * @param data
	 *            ...
	 * @param pos
	 *            ..
	 */
	public static void convertFloatToHalf(float flt, short[] data, int pos) {
		if (flt == 0f) {
			data[pos] = (short) 0x0000;
		} else if (flt == -0f) {
			data[pos] = (short) 0x8000;
		} else if (flt == 1f) {
			data[pos] = (short) 0x3c00;
		} else if (flt == -1f) {
			data[pos] = (short) 0xbc00;
		} else if (flt > FLOAT_HALF_MAX) {
			if (flt == Float.POSITIVE_INFINITY) {
				data[pos] = (short) 0x7c00;
			} else {
				data[pos] = (short) 0x7bff;
			}
		} else if (flt < -FLOAT_HALF_MAX) {
			if (flt == Float.NEGATIVE_INFINITY) {
				data[pos] = (short) 0xfc00;
			} else {
				data[pos] = (short) 0xfbff;
			}
		} else if (flt > 0f && flt < FLOAT_HALF_PREC) {
			data[pos] = (short) 0x0001;
		} else if (flt < 0f && flt > -FLOAT_HALF_PREC) {
			data[pos] = (short) 0x8001;
		} else {
			int f = Float.floatToIntBits(flt);

			if (f == 0x7fc00000)
				throw new UnsupportedOperationException(
						"NaN to half conversion not supported!");

			data[pos] = (short) (((f >> 16) & 0x8000)
					| ((((f & 0x7f800000) - 0x38000000) >> 13) & 0x7c00)
					| ((f >> 13) & 0x03ff));
		}
	}
}
