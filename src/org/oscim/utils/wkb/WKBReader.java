/*
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.utils.wkb;

import org.oscim.core.GeometryBuffer;

public class WKBReader {
	interface Callback {
		public void process(GeometryBuffer geom);
	}

	// taken from postgis-java

	private GeometryBuffer mGeom;
	private final double mScale = 1;
	private final double mOffsetX = 0;
	private final double mOffsetY = 0;

	private WKBReader.Callback mCallback;

	/**
	 * Parse a binary encoded geometry.
	 *
	 * @param value
	 *            ...
	 * @return ...
	 */
	boolean parse(byte[] value) {
		return parseGeometry(valueGetterForEndian(value), 0);
	}

	private boolean parseGeometry(ValueGetter data, int count) {
		byte endian = data.getByte(); // skip and test endian flag
		if (endian != data.endian) {
			throw new IllegalArgumentException("Endian inconsistency!");
		}
		int typeword = data.getInt();

		int realtype = typeword & 0x1FFFFFFF; // cut off high flag bits

		boolean haveZ = (typeword & 0x80000000) != 0;
		boolean haveM = (typeword & 0x40000000) != 0;
		boolean haveS = (typeword & 0x20000000) != 0;

		// int srid = Geometry.UNKNOWN_SRID;
		boolean polygon = false;
		if (haveS) {
			// srid = Geometry.parseSRID(data.getInt());
			data.getInt();
		}
		switch (realtype) {
			case Geometry.POINT:
				mGeom.startPoints();
				parsePoint(data, haveZ, haveM);
				break;
			case Geometry.LINESTRING:
				mGeom.startLine();
				parseLineString(data, haveZ, haveM);
				break;
			case Geometry.POLYGON:
				mGeom.startPolygon();
				parsePolygon(data, haveZ, haveM);
				polygon = true;
				break;
			case Geometry.MULTIPOINT:
				mGeom.startPoints();
				parseMultiPoint(data);
				break;
			case Geometry.MULTILINESTRING:
				mGeom.startLine();
				parseMultiLineString(data);
				break;
			case Geometry.MULTIPOLYGON:
				mGeom.startPolygon();
				parseMultiPolygon(data);
				polygon = true;
				break;
			case Geometry.GEOMETRYCOLLECTION:
				parseCollection(data);
				break;
			default:
				throw new IllegalArgumentException("Unknown Geometry Type: " + realtype);
		}

		if (count == 0) {
			mCallback.process(mGeom);
			mGeom.clear();
		}
		// if (srid != Geometry.UNKNOWN_SRID) {
		// result.setSrid(srid);
		// }
		return polygon;
	}

	private void parsePoint(ValueGetter data, boolean haveZ, boolean haveM) {

		float x = (float) ((data.getDouble() + mOffsetX) * mScale);
		float y = (float) ((data.getDouble() + mOffsetY) * mScale);
		mGeom.addPoint(x, y);

		if (haveZ)
			data.getDouble();

		if (haveM)
			data.getDouble();
	}

	/**
	 * Parse an Array of "full" Geometries
	 *
	 * @param data
	 *            ...
	 * @param count
	 *            ...
	 */
	private void parseGeometryArray(ValueGetter data, int count) {
		mGeom.clear();

		for (int i = 0; i < count; i++) {
			parseGeometry(data, count);
			mGeom.index[mGeom.indexPos++] = 0;
		}

		mCallback.process(mGeom);
		mGeom.clear();
	}

	private void parseMultiPoint(ValueGetter data) {
		parseGeometryArray(data, data.getInt());
	}

	private void parseLineString(ValueGetter data, boolean haveZ, boolean haveM) {

		int count = data.getInt();

		for (int i = 0; i < count; i++) {
			float x = (float) ((data.getDouble() + mOffsetX) * mScale);
			float y = (float) ((data.getDouble() + mOffsetY) * mScale);
			mGeom.addPoint(x, y);

			// ignore
			if (haveZ)
				data.getDouble();
			if (haveM)
				data.getDouble();
		}
	}

	private void parsePolygon(ValueGetter data, boolean haveZ, boolean haveM) {
		int count = data.getInt();

		for (int i = 0; i < count; i++) {

			if (i > 0)
				mGeom.startHole();

			parseLineString(data, haveZ, haveM);
		}
	}

	private void parseMultiLineString(ValueGetter data) {

		int count = data.getInt();
		if (count <= 0)
			return;

		parseGeometryArray(data, count);
	}

	private void parseMultiPolygon(ValueGetter data) {
		int count = data.getInt();
		if (count <= 0)
			return;

		parseGeometryArray(data, count);
	}

	private void parseCollection(ValueGetter data) {
		int count = data.getInt();
		parseGeometryArray(data, count);

		mCallback.process(mGeom);
		mGeom.clear();
	}

	private static ValueGetter valueGetterForEndian(byte[] bytes) {
		if (bytes[0] == ValueGetter.XDR.NUMBER) { // XDR
			return new ValueGetter.XDR(bytes);
		} else if (bytes[0] == ValueGetter.NDR.NUMBER) {
			return new ValueGetter.NDR(bytes);
		} else {
			throw new IllegalArgumentException("Unknown Endian type:" + bytes[0]);
		}
	}

}
