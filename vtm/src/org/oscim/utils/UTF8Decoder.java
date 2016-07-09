package org.oscim.utils;

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

// modified new String so that it reuses temporary buffer instead of reallocation
// when guess for size was incorrect.
public class UTF8Decoder {
    private static final char REPLACEMENT_CHAR = (char) 0xfffd;

    char[] mBuffer;
    int mBufferSize = 0;

    public String decode(byte[] data, int offset, int byteCount) {
        if ((offset | byteCount) < 0 || byteCount > data.length - offset) {
            throw new IllegalArgumentException("Brrr " + data.length
                    + " " + offset + " " + byteCount);
        }

        byte[] d = data;
        char[] v;

        if (mBufferSize < byteCount)
            v = mBuffer = new char[byteCount];
        else
            v = mBuffer;

        int idx = offset;
        int last = offset + byteCount;
        int s = 0;
        outer:
        while (idx < last) {
            byte b0 = d[idx++];
            if ((b0 & 0x80) == 0) {
                // 0xxxxxxx
                // Range:  U-00000000 - U-0000007F
                int val = b0 & 0xff;
                v[s++] = (char) val;
            } else if (((b0 & 0xe0) == 0xc0) || ((b0 & 0xf0) == 0xe0) ||
                    ((b0 & 0xf8) == 0xf0) || ((b0 & 0xfc) == 0xf8) || ((b0 & 0xfe) == 0xfc)) {
                int utfCount = 1;
                if ((b0 & 0xf0) == 0xe0)
                    utfCount = 2;
                else if ((b0 & 0xf8) == 0xf0)
                    utfCount = 3;
                else if ((b0 & 0xfc) == 0xf8)
                    utfCount = 4;
                else if ((b0 & 0xfe) == 0xfc)
                    utfCount = 5;

                // 110xxxxx (10xxxxxx)+
                // Range:  U-00000080 - U-000007FF (count == 1)
                // Range:  U-00000800 - U-0000FFFF (count == 2)
                // Range:  U-00010000 - U-001FFFFF (count == 3)
                // Range:  U-00200000 - U-03FFFFFF (count == 4)
                // Range:  U-04000000 - U-7FFFFFFF (count == 5)

                if (idx + utfCount > last) {
                    v[s++] = REPLACEMENT_CHAR;
                    break;
                }

                // Extract usable bits from b0
                int val = b0 & (0x1f >> (utfCount - 1));
                for (int i = 0; i < utfCount; i++) {
                    byte b = d[idx++];
                    if ((b & 0xC0) != 0x80) {
                        v[s++] = REPLACEMENT_CHAR;
                        idx--; // Put the input char back
                        continue outer;
                    }
                    // Push new bits in from the right side
                    val <<= 6;
                    val |= b & 0x3f;
                }

                // Note: Java allows overlong char
                // specifications To disallow, check that val
                // is greater than or equal to the minimum
                // value for each count:
                //
                // count    min value
                // -----   ----------
                //   1           0x80
                //   2          0x800
                //   3        0x10000
                //   4       0x200000
                //   5      0x4000000

                // Allow surrogate values (0xD800 - 0xDFFF) to
                // be specified using 3-byte UTF values only
                if ((utfCount != 2) && (val >= 0xD800) && (val <= 0xDFFF)) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }

                // Reject chars greater than the Unicode maximum of U+10FFFF.
                if (val > 0x10FFFF) {
                    v[s++] = REPLACEMENT_CHAR;
                    continue;
                }

                // Encode chars from U+10000 up as surrogate pairs
                if (val < 0x10000) {
                    v[s++] = (char) val;
                } else {
                    int x = val & 0xffff;
                    int u = (val >> 16) & 0x1f;
                    int w = (u - 1) & 0xffff;
                    int hi = 0xd800 | (w << 6) | (x >> 10);
                    int lo = 0xdc00 | (x & 0x3ff);
                    v[s++] = (char) hi;
                    v[s++] = (char) lo;
                }
            } else {
                // Illegal values 0x8*, 0x9*, 0xa*, 0xb*, 0xfd-0xff
                v[s++] = REPLACEMENT_CHAR;
            }
        }
        return new String(v, 0, s);
    }
}
