/*
 * Copyright 2013 Hannes Janetzek
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
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
import org.oscim.core.Tile;

public class WKBReader {
    public interface Callback {
        public void process(GeometryBuffer geom);
    }

    private final GeometryBuffer mGeom;
    private final boolean mFlipY;

    private WKBReader.Callback mCallback;

    public WKBReader(GeometryBuffer geom, boolean flipY) {
        mGeom = geom;
        mFlipY = flipY;
    }

    public void setCallback(WKBReader.Callback cb) {
        mCallback = cb;
    }

    /**
     * Parse a binary encoded geometry.
     */
    public void parse(byte[] value) {
        parseGeometry(valueGetterForEndian(value), 0);
    }

    /**
     * Parse a hex encoded geometry.
     */
    public void parse(String value) {
        byte[] b = hexStringToByteArray(value);
        if (b == null)
            return;

        parse(b);
    }

    private void parseGeometry(ValueGetter data, int count) {
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
    }

    private void parsePoint(ValueGetter data, boolean haveZ, boolean haveM) {

        float x = (float) data.getDouble();
        float y = (float) data.getDouble();
        if (mFlipY)
            y = Tile.SIZE - y;

        mGeom.addPoint(x, y);

        if (haveZ)
            data.getDouble();

        if (haveM)
            data.getDouble();
    }

    /**
     * Parse an Array of "full" Geometries
     *
     * @param data  ...
     * @param count ...
     */
    private void parseGeometryArray(ValueGetter data, int count, int type) {
        mGeom.clear();

        for (int i = 0; i < count; i++) {
            if (i > 0) {
                if (type == Geometry.LINESTRING)
                    mGeom.startLine();
                else if (type == Geometry.POLYGON)
                    mGeom.startPolygon();
                else {
                    mCallback.process(mGeom);
                    mGeom.clear();
                }
            }
            parseGeometry(data, count);
            // mGeom.index[++mGeom.indexPos] = -1;
        }

        mCallback.process(mGeom);
        mGeom.clear();
    }

    private void parseMultiPoint(ValueGetter data) {
        parseGeometryArray(data, data.getInt(), Geometry.POINT);
    }

    private void parseLineString(ValueGetter data, boolean haveZ, boolean haveM) {

        int count = data.getInt();

        for (int i = 0; i < count; i++) {
            float x = (float) data.getDouble();
            float y = (float) data.getDouble();

            if (mFlipY)
                y = Tile.SIZE - y;

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

            int points = data.getInt();

            for (int j = 0; j < points; j++) {
                float x = (float) data.getDouble();
                float y = (float) data.getDouble();

                if (mFlipY)
                    y = Tile.SIZE - y;

                // drop redundant closing point
                if (j < points - 1)
                    mGeom.addPoint(x, y);

                // ignore
                if (haveZ)
                    data.getDouble();
                if (haveM)
                    data.getDouble();
            }
        }
    }

    private void parseMultiLineString(ValueGetter data) {

        int count = data.getInt();
        if (count <= 0)
            return;

        parseGeometryArray(data, count, Geometry.LINESTRING);
    }

    private void parseMultiPolygon(ValueGetter data) {
        int count = data.getInt();
        if (count <= 0)
            return;

        parseGeometryArray(data, count, Geometry.POLYGON);
    }

    private void parseCollection(ValueGetter data) {
        int count = data.getInt();

        parseGeometryArray(data, count, Geometry.GEOMETRYCOLLECTION);

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

    /**
     * Converting a string of hex character to bytes
     * <p/>
     * from http://stackoverflow.com/questions/140131/convert-a-string-
     * representation-of-a-hex-dump-to-a-byte-array-using-java
     */
    public static byte[] hexStringToByteArray(String s) {

        int len = s.length();
        if (len < 2)
            return null;

        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
