/*
 * Copyright 2013
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
package org.oscim.database.common;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.utils.UTF8Decoder;

import android.util.Log;

public class ProtobufDecoder {
	private final static String TAG = ProtobufDecoder.class.getName();

	private final static int VARINT_LIMIT = 6;
	private final static int VARINT_MAX = 10;

	private final static int BUFFER_SIZE = 1 << 15; // 32kb
	protected byte[] buffer = new byte[BUFFER_SIZE];

	// position in buffer
	protected int bufferPos;

	// bytes available in buffer
	int bufferFill;

	// offset of buffer in message
	private int mBufferOffset;

	// max bytes to read: message = header + content
	private int mMsgEnd;

	// overall bytes of message read
	private int mMsgPos;

	private InputStream mInputStream;

	private final UTF8Decoder mStringDecoder;

	public ProtobufDecoder() {
		mStringDecoder = new UTF8Decoder();
	}

	protected static int readUnsignedInt(InputStream is, byte[] buf) throws IOException {
		// check 4 bytes available..
		int read = 0;
		int len = 0;

		while (read < 4 && (len = is.read(buf, read, 4 - read)) >= 0)
			read += len;

		if (read < 4)
			return read < 0 ? (read * 10) : read;

		return decodeInt(buf, 0);
	}

	static int decodeInt(byte[] buffer, int offset) {
		return buffer[offset] << 24 | (buffer[offset + 1] & 0xff) << 16
				| (buffer[offset + 2] & 0xff) << 8
				| (buffer[offset + 3] & 0xff);
	}

	public void setInputStream(InputStream is, int contentLength) {
		mInputStream = is;

		bufferFill = 0;
		bufferPos = 0;
		mBufferOffset = 0;

		mMsgPos = 0;
		mMsgEnd = contentLength;
	}

//	public void skipAvailable() throws IOException {
//		int bytes = decodeVarint32();
//		bufferPos += bytes;
//	}

	protected int decodeInterleavedPoints(float[] coords, int numPoints, float scale)
			throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);

		int cnt = 0;
		int lastX = 0;
		int lastY = 0;
		boolean even = true;

		byte[] buf = buffer;
		int pos = bufferPos;
		int end = pos + bytes;
		int val;

		while (pos < end) {
			if (buf[pos] >= 0) {
				val = buf[pos++];

			} else if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| buf[pos++] << 7;

			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;

			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;

			} else {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos]) << 28;

				int max = pos + VARINT_LIMIT;
				while (buf[pos++] < 0)
					if (pos == max)
						throw new IOException("malformed VarInt32");
			}

			// zigzag decoding
			int s = ((val >>> 1) ^ -(val & 1));

			if (even) {
				lastX = lastX + s;
				coords[cnt++] = lastX / scale;
				even = false;
			} else {
				lastY = lastY + s;
				coords[cnt++] = lastY / scale;
				even = true;
			}
		}
		if (pos != bufferPos + bytes)
			throw new IOException("invalid array " + numPoints);

		bufferPos = pos;

		return cnt;
	}

	public void decodeVarintArray(int num, short[] array) throws IOException {
		int bytes = decodeVarint32();

		readBuffer(bytes);

		int cnt = 0;

		byte[] buf = buffer;
		int pos = bufferPos;
		int end = pos + bytes;
		int val;

		while (pos < end) {
			if (cnt == num)
				throw new IOException("invalid array size " + num);

			if (buf[pos] >= 0) {
				val = buf[pos++];
			} else if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| buf[pos++] << 7;
			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;
			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;
			} else if (buf[pos + 4] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos++]) << 28;
			} else
				throw new IOException("malformed VarInt32");

			array[cnt++] = (short) val;
		}

		if (pos != bufferPos + bytes)
			throw new IOException("invalid array " + num);

		bufferPos = pos;
	}

	/**
	 * fill short array from packed uint32. Array values must be positive
	 * as the end will be marked by -1 if the resulting array is larger
	 * than the input!
	 */
	protected short[] decodeUnsignedVarintArray(short[] array) throws IOException {
		int bytes = decodeVarint32();

		int arrayLength = 0;
		if (array == null) {
			arrayLength = 32;
			array = new short[32];
		}

		readBuffer(bytes);
		int cnt = 0;

		byte[] buf = buffer;
		int pos = bufferPos;
		int end = pos + bytes;
		int val;

		while (pos < end) {
			if (buf[pos] >= 0) {
				val = buf[pos++];
			} else if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| buf[pos++] << 7;
			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;
			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;
			} else {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos]) << 28;

				int max = pos + VARINT_LIMIT;
				while (pos < max)
					if (buf[pos++] >= 0)
						break;

				if (pos > max)
					throw new IOException("malformed VarInt32");
			}

			if (arrayLength <= cnt) {
				arrayLength = cnt + 16;
				short[] tmp = array;
				array = new short[arrayLength];
				System.arraycopy(tmp, 0, array, 0, cnt);
			}

			array[cnt++] = (short) val;
		}

		bufferPos = pos;

		if (arrayLength > cnt)
			array[cnt] = -1;

		return array;
	}

	protected int decodeVarint32() throws IOException {
		if (bufferPos + VARINT_MAX > bufferFill)
			readBuffer(4096);

		return decodeVarint32Filled();
	}

	protected int decodeVarint32Filled() throws IOException {
		if (bufferPos + VARINT_MAX > bufferFill)
			readBuffer(4096);

		byte[] buf = buffer;
		int pos = bufferPos;
		int val;

		if (buf[pos] >= 0) {
			val = buf[pos++];
		} else {

			if (buf[pos + 1] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++]) << 7;

			} else if (buf[pos + 2] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++]) << 14;

			} else if (buf[pos + 3] >= 0) {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++]) << 21;
			} else {
				val = (buf[pos++] & 0x7f)
						| (buf[pos++] & 0x7f) << 7
						| (buf[pos++] & 0x7f) << 14
						| (buf[pos++] & 0x7f) << 21
						| (buf[pos]) << 28;

				// 'Discard upper 32 bits'
				int max = pos + VARINT_LIMIT;
				while (pos < max)
					if (buf[pos++] >= 0)
						break;

				if (pos == max)
					throw new IOException("malformed VarInt32");
			}
		}

		bufferPos = pos;

		return val;
	}
	// FIXME this also accept uin64 atm.
//	protected int decodeVarint32Filled() throws IOException {
//
//		if (buffer[bufferPos] >= 0)
//			return buffer[bufferPos++];
//
//		byte[] buf = buffer;
//		int pos = bufferPos;
//		int val = 0;
//
//		if (buf[pos + 1] >= 0) {
//			val = (buf[pos++] & 0x7f)
//					| (buf[pos++]) << 7;
//
//		} else if (buf[pos + 2] >= 0) {
//			val = (buf[pos++] & 0x7f)
//					| (buf[pos++] & 0x7f) << 7
//					| (buf[pos++]) << 14;
//
//		} else if (buf[pos + 3] >= 0) {
//			val = (buf[pos++] & 0x7f)
//					| (buf[pos++] & 0x7f) << 7
//					| (buf[pos++] & 0x7f) << 14
//					| (buf[pos++]) << 21;
//		} else {
//			val = (buf[pos++] & 0x7f)
//					| (buf[pos++] & 0x7f) << 7
//					| (buf[pos++] & 0x7f) << 14
//					| (buf[pos++] & 0x7f) << 21
//					| (buf[pos]) << 28;
//
//
//			// 'Discard upper 32 bits'
//			int max = pos + VARINT_LIMIT;
//			while (pos < max)
//				if (buf[pos++] >= 0)
//					break;
//			if (pos == max)
//				throw new IOException("malformed VarInt32");
//		}
//		bufferPos = pos;
//
//		return val;
//	}

	public String decodeString() throws IOException {
		final int size = decodeVarint32();
		readBuffer(size);

		String result;

		if (mStringDecoder == null)
			result = new String(buffer, bufferPos, size, "UTF-8");
		else
			result = mStringDecoder.decode(buffer, bufferPos, size);

		bufferPos += size;

		return result;

	}

	public float decodeFloat() throws IOException {
		if (bufferPos + 4 > bufferFill)
			readBuffer(4096);

		byte[] buf = buffer;
		int pos = bufferPos;

		int val = (buf[pos++] & 0xFF
				| (buf[pos++] & 0xFF) << 8
				| (buf[pos++] & 0xFF) << 16
				| (buf[pos++] & 0xFF) << 24);

		bufferPos += 4;
		return Float.intBitsToFloat(val);
	}

	public double decodeDouble() throws IOException {
		if (bufferPos + 8 > bufferFill)
			readBuffer(4096);

		byte[] buf = buffer;
		int pos = bufferPos;

		long val = (buf[pos++] & 0xFF
				| (buf[pos++] & 0xFF) << 8
				| (buf[pos++] & 0xFF) << 16
				| (buf[pos++] & 0xFF) << 24
				| (buf[pos++] & 0xFF) << 32
				| (buf[pos++] & 0xFF) << 40
				| (buf[pos++] & 0xFF) << 48
				| (buf[pos++] & 0xFF) << 56);

		bufferPos += 8;
		return Double.longBitsToDouble(val);
	}

	public boolean decodeBool() throws IOException {
		if (bufferPos + 1 > bufferFill)
			readBuffer(4096);

		return buffer[bufferPos++] != 0;
	}

	public boolean hasData() throws IOException {
		if (mBufferOffset + bufferPos >= mMsgEnd)
			return false;

		return readBuffer(1);
	}

	public int position() {
		return mBufferOffset + bufferPos;
	}

	public boolean readBuffer(int size) throws IOException {
		// check if buffer already contains the request bytes
		if (bufferPos + size < bufferFill)
			return true;

		// check if inputstream is read to the end
		if (mMsgPos >= mMsgEnd)
			return false;

		int maxSize = buffer.length;

		if (size > maxSize) {
			Log.d(TAG, "increase read buffer to " + size + " bytes");
			maxSize = size;
			bufferFill -= bufferPos;

			byte[] tmp = buffer;
			buffer = new byte[maxSize];
			System.arraycopy(tmp, bufferPos, buffer, 0, bufferFill);

			mBufferOffset += bufferPos;
			bufferPos = 0;
		}

		if (bufferFill == bufferPos) {
			mBufferOffset += bufferPos;
			bufferPos = 0;
			bufferFill = 0;
		} else if (bufferPos + size > maxSize) {
			// copy bytes left to the beginning of buffer
			bufferFill -= bufferPos;
			System.arraycopy(buffer, bufferPos, buffer, 0, bufferFill);
			mBufferOffset += bufferPos;
			bufferPos = 0;
		}

		int max = maxSize - bufferFill;

		while ((bufferFill - bufferPos) < size && max > 0) {

			max = maxSize - bufferFill;
			if (max > mMsgEnd - mMsgPos)
				max = mMsgEnd - mMsgPos;

			// read until requested size is available in buffer
			int len = mInputStream.read(buffer, bufferFill, max);

			if (len < 0) {
				// finished reading, mark end
				buffer[bufferFill] = 0;
				return false;
			}

			mMsgPos += len;
			bufferFill += len;

			if (mMsgPos == mMsgEnd)
				break;

		}
		return true;
	}
}
