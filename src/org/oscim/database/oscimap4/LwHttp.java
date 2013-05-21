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
package org.oscim.database.oscimap4;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;

import org.oscim.core.Tile;

import android.os.SystemClock;
import android.util.Log;

public class LwHttp {
	private static final String TAG = LwHttp.class.getName();
	private final static int BUFFER_SIZE = 1 << 15;
	byte[] buffer = new byte[BUFFER_SIZE];

	// position in buffer
	int bufferPos;

	// bytes available in buffer
	int bufferFill;


	// offset of buffer in message
	private int mBufferOffset;

	// max bytes to read: message = header + content
	private long mReadEnd;

	// overall bytes of message read
	private int mReadPos;

	private String mHost;
	private int mPort;
	private InputStream mInputStream;

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private InputStream mResponseStream;
	long mLastRequest = 0;
	private SocketAddress mSockAddr;

	private final static byte[] RESPONSE_HTTP_OK = "HTTP/1.1 200 OK".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static int RESPONSE_EXPECTED_TIMEOUT = 10000;
	private final static String TILE_EXT = ".vtm";

	private byte[] REQUEST_GET_START;
	private byte[] REQUEST_GET_END;

	private byte[] mRequestBuffer;

	boolean setServer(String urlString) {
		URL url;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {

			e.printStackTrace();
			return false;
			//return new OpenResult("invalid url: " + options.get("url"));
		}

		int port = url.getPort();
		if (port < 0)
			port = 80;

		String host = url.getHost();
		String path = url.getPath();
		Log.d(TAG, "open oscim database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();
		REQUEST_GET_END = (TILE_EXT + " HTTP/1.1\n" +
				"Host: " + host + "\n" +
				"Connection: Keep-Alive\n\n").getBytes();

		mHost = host;
		mPort = port;

		mRequestBuffer = new byte[1024];
		System.arraycopy(REQUEST_GET_START, 0,
				mRequestBuffer, 0, REQUEST_GET_START.length);
		return true;
	}

	void close() {
		if (mSocket != null) {
			try {
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				mSocket = null;
			}
		}
	}

	int readHeader() throws IOException {
		InputStream is = mResponseStream;
		mResponseStream.mark(BUFFER_SIZE);

		byte[] buf = buffer;
		boolean first = true;
		int read = 0;
		int pos = 0;
		int end = 0;
		int len = 0;

		// header cannot be larger than BUFFER_SIZE for this to work
		for (; pos < read || (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0; len = 0) {
			read += len;
			while (end < read && (buf[end] != '\n'))
				end++;

			if (buf[end] == '\n') {
				if (first) {
					// check only for OK
					first = false;
					if (!compareBytes(buf, pos, end, RESPONSE_HTTP_OK, 15))
						return -1;

				} else if (end - pos == 1) {
					// check empty line (header end)
					end += 1;
					break;
				}

				// String line = new String(buf, pos, end - pos - 1);
				// Log.d(TAG, ">" + line + "< " + resp_len);

				pos += (end - pos) + 1;
				end = pos;
			}
		}

		// check 4 bytes available..
		while ((read - end) < 4 && (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0)
			read += len;

		if (read - len < 4)
			return -1;

		int contentLength = decodeInt(buf, end);

//		// back to start of content
//		mResponseStream.reset();
//		mResponseStream.mark(0);
//		mResponseStream.skip(end + 4);

		// start of content
		bufferPos = end + 4;
		// buffer fill
		bufferFill = read;
		mBufferOffset = 0;

		// overall bytes of already read
		mReadPos = read;
		mReadEnd = bufferPos + contentLength;

		mInputStream = mResponseStream;

		return 1;
	}

	boolean sendRequest(Tile tile) throws IOException {

		bufferFill = 0;
		bufferPos = 0;
		mReadPos = 0;
		mCacheFile = null;

		if (mSocket != null && ((mMaxReq-- <= 0)
				|| (SystemClock.elapsedRealtime() - mLastRequest
				> RESPONSE_EXPECTED_TIMEOUT))) {
			try {
				mSocket.close();
			} catch (IOException e) {

			}

			// Log.d(TAG, "not alive  - recreate connection " + mMaxReq);
			mSocket = null;
		}

		if (mSocket == null) {
			lwHttpConnect();
			// we know our server
			mMaxReq = RESPONSE_EXPECTED_LIVES;
			// Log.d(TAG, "create connection");
		} else {
			// should not be needed
			int avail = mResponseStream.available();
			if (avail > 0) {
				Log.d(TAG, "Consume left-over bytes: " + avail);
				mResponseStream.read(buffer, 0, avail);
			}
		}

		byte[] request = mRequestBuffer;
		int pos = REQUEST_GET_START.length;

		pos = writeInt(tile.zoomLevel, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileX, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileY, pos, request);

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

		// this does the same but with a few more allocations:
		// byte[] request = String.format(REQUEST,
		// Integer.valueOf(tile.zoomLevel),
		// Integer.valueOf(tile.tileX), Integer.valueOf(tile.tileY)).getBytes();

		try {
			mCommandStream.write(request, 0, len);
			mCommandStream.flush();
			return true;
		} catch (IOException e) {
			Log.d(TAG, "recreate connection");
		}

		lwHttpConnect();

		mCommandStream.write(request, 0, len);
		mCommandStream.flush();

		return true;
	}

	private boolean lwHttpConnect() throws IOException {
		if (mSockAddr == null)
			mSockAddr = new InetSocketAddress(mHost, mPort);

		mSocket = new Socket();
		mSocket.connect(mSockAddr, 30000);
		mSocket.setTcpNoDelay(true);

		mCommandStream = mSocket.getOutputStream(); //new BufferedOutputStream();
		mResponseStream = mSocket.getInputStream();

		return true;
	}

	// write (positive) integer as char sequence to buffer
	private static int writeInt(int val, int pos, byte[] buf) {
		if (val == 0) {
			buf[pos] = '0';
			return pos + 1;
		}

		int i = 0;
		for (int n = val; n > 0; n = n / 10, i++)
			buf[pos + i] = (byte) ('0' + n % 10);

		// reverse bytes
		for (int j = pos, end = pos + i - 1, mid = pos + i / 2; j < mid; j++, end--) {
			byte tmp = buf[j];
			buf[j] = buf[end];
			buf[end] = tmp;
		}

		return pos + i;
	}

	private static boolean compareBytes(byte[] buffer, int position, int available,
			byte[] string, int length) {

		if (available - position < length)
			return false;

		for (int i = 0; i < length; i++)
			if (buffer[position + i] != string[i])
				return false;

		return true;
	}

	static int decodeInt(byte[] buffer, int offset) {
		return buffer[offset] << 24 | (buffer[offset + 1] & 0xff) << 16
				| (buffer[offset + 2] & 0xff) << 8
				| (buffer[offset + 3] & 0xff);
	}

	public boolean hasData() {
		return mBufferOffset + bufferPos < mReadEnd;
	}

	public int position() {
		return mBufferOffset + bufferPos;
	}

	public void readBuffer(int size) throws IOException {
		// check if buffer already contains the request bytes
		if (bufferPos + size < bufferFill)
			return;

		// check if inputstream is read to the end
		if (mReadPos == mReadEnd)
			return;

		int maxSize = buffer.length;

		if (size > maxSize) {
			Log.d(TAG, "increase read buffer to " + size + " bytes");
			maxSize = size;
			byte[] tmp = new byte[maxSize];
			bufferFill -= bufferPos;
			System.arraycopy(buffer, bufferPos, tmp, 0, bufferFill);
			mBufferOffset += bufferPos;
			bufferPos = 0;
			buffer = tmp;
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
			if (max > mReadEnd - mReadPos)
				max = (int) (mReadEnd - mReadPos);

			// read until requested size is available in buffer
			int len = mInputStream.read(buffer, bufferFill, max);

			if (len < 0) {
				// finished reading, mark end
				buffer[bufferFill] = 0;
				break;
			}

			mReadPos += len;

			//			if (mCacheFile != null)
			//				mCacheFile.write(mReadBuffer, mBufferFill, len);

			if (mReadPos == mReadEnd)
				break;

			bufferFill += len;
		}
	}

	private FileOutputStream mCacheFile;

	boolean cacheRead(Tile tile, File f) {
		if (f.exists() && f.length() > 0) {
			FileInputStream in;

			try {
				in = new FileInputStream(f);

				mReadEnd = f.length();
				Log.d(TAG, tile + " - using cache: " + mReadEnd);
				mInputStream = in;

				//decode();
				in.close();

				return true;

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			f.delete();
			return false;
		}

		return false;
	}

	boolean cacheBegin(Tile tile, File f) {
		if (MapDatabase.USE_CACHE) {
			try {
				Log.d(TAG, tile + " - writing cache");
				mCacheFile = new FileOutputStream(f);

				if (mReadPos > 0) {
					try {
						mCacheFile.write(buffer, bufferPos,
								bufferFill - bufferPos);

					} catch (IOException e) {
						e.printStackTrace();
						mCacheFile = null;
						return false;
					}
				}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				mCacheFile = null;
				return false;
			}
		}
		return true;
	}

	void cacheFinish(Tile tile, File file, boolean success) {
		if (MapDatabase.USE_CACHE) {
			if (success) {
				try {
					mCacheFile.flush();
					mCacheFile.close();
					Log.d(TAG, tile + " - cache written " + file.length());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				file.delete();
			}
		}
		mCacheFile = null;
	}
}
