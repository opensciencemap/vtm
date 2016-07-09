/*
 * ValueGetter.java
 *
 * PostGIS extension for PostgreSQL JDBC driver - Binary Parser
 *
 * (C) 2005 Markus Schaber, markus.schaber@logix-tt.com
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General License as published by the Free
 * Software Foundation, either version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General License
 * along with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA or visit the web at
 * http://www.gnu.org.
 *
 * $Id: ValueGetter.java 9324 2012-02-27 22:08:12Z pramsey $
 */

package org.oscim.utils.wkb;

abstract class ValueGetter {
    byte[] data;
    int position;
    final byte endian;

    ValueGetter(byte[] data, byte endian) {
        this.data = data;

        this.endian = endian;
    }

    /**
     * Get a byte, should be equal for all endians
     *
     * @return ...
     */
    byte getByte() {
        return data[position++];
    }

    int getInt() {
        int res = getInt(position);
        position += 4;
        return res;
    }

    long getLong() {
        long res = getLong(position);
        position += 8;
        return res;
    }

    /**
     * Get a 32-Bit integer
     *
     * @param index ...
     * @return ...
     */
    protected abstract int getInt(int index);

    /**
     * Get a long value. This is not needed directly, but as a nice side-effect
     * from GetDouble.
     *
     * @param index ...
     * @return ...
     */
    protected abstract long getLong(int index);

    /**
     * Get a double.
     *
     * @return ...
     */
    double getDouble() {
        long bitrep = getLong();
        return Double.longBitsToDouble(bitrep);
    }

    static class XDR extends ValueGetter {
        static final byte NUMBER = 0;

        XDR(byte[] data) {
            super(data, NUMBER);
        }

        @Override
        protected int getInt(int index) {
            return ((data[index] & 0xFF) << 24) + ((data[index + 1] & 0xFF) << 16)
                    + ((data[index + 2] & 0xFF) << 8) + (data[index + 3] & 0xFF);
        }

        @Override
        protected long getLong(int index) {

            return ((long) (data[index] & 0xFF) << 56) | ((long) (data[index + 1] & 0xFF) << 48)
                    | ((long) (data[index + 2] & 0xFF) << 40)
                    | ((long) (data[index + 3] & 0xFF) << 32)
                    | ((long) (data[index + 4] & 0xFF) << 24)
                    | ((long) (data[index + 5] & 0xFF) << 16)
                    | ((long) (data[index + 6] & 0xFF) << 8)
                    | ((long) (data[index + 7] & 0xFF) << 0);
        }
    }

    static class NDR extends ValueGetter {
        static final byte NUMBER = 1;

        NDR(byte[] data) {
            super(data, NUMBER);
        }

        @Override
        protected int getInt(int index) {
            return ((data[index + 3] & 0xFF) << 24) + ((data[index + 2] & 0xFF) << 16)
                    + ((data[index + 1] & 0xFF) << 8) + (data[index] & 0xFF);
        }

        @Override
        protected long getLong(int index) {
            return ((long) (data[index + 7] & 0xFF) << 56)
                    | ((long) (data[index + 6] & 0xFF) << 48)
                    | ((long) (data[index + 5] & 0xFF) << 40)
                    | ((long) (data[index + 4] & 0xFF) << 32)
                    | ((long) (data[index + 3] & 0xFF) << 24)
                    | ((long) (data[index + 2] & 0xFF) << 16)
                    | ((long) (data[index + 1] & 0xFF) << 8) | ((long) (data[index] & 0xFF) << 0);

        }
    }
}
