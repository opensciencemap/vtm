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
package org.oscim.tilesource.common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.zip.InflaterInputStream;

import org.oscim.core.Tile;

import android.os.SystemClock;
import android.util.Log;

public class LwHttp {
	private static final String TAG = LwHttp.class.getName();
	//private static final boolean DEBUG = false;

	private final static byte[] HEADER_HTTP_OK = "200 OK".getBytes();
	private final static byte[] HEADER_CONTENT_TYPE = "Content-Type".getBytes();
	private final static byte[] HEADER_CONTENT_LENGTH = "Content-Length".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static int RESPONSE_TIMEOUT = 10000;

	private final static int BUFFER_SIZE = 1024;
	private final byte[] buffer = new byte[BUFFER_SIZE];

	private final String mHost;
	private final int mPort;

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private InputStream mResponseStream;
	private long mLastRequest = 0;
	private SocketAddress mSockAddr;

	private final byte[] REQUEST_GET_START;
	private final byte[] REQUEST_GET_END;
	private final byte[] mRequestBuffer;

	private final boolean mInflateContent;
	private final byte[] mContentType;
	//private final String mExtension;

	private int mContentLength = -1;

	public LwHttp(URL url, String contentType, String extension, boolean deflate) {
		//mExtension = extension;
		mContentType = contentType.getBytes();
		mInflateContent = deflate;

		int port = url.getPort();
		if (port < 0)
			port = 80;

		String host = url.getHost();
		String path = url.getPath();
		Log.d(TAG, "open database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();

		REQUEST_GET_END = ("." + extension + " HTTP/1.1" +
				"\nHost: " + host +
				"\nConnection: Keep-Alive" +
				"\n\n").getBytes();

		mHost = host;
		mPort = port;

		mRequestBuffer = new byte[1024];
		System.arraycopy(REQUEST_GET_START, 0,
				mRequestBuffer, 0, REQUEST_GET_START.length);
	}

	static class Buffer extends BufferedInputStream {
		public Buffer(InputStream is) {
			super(is, 4096);
		}

		@Override
		public synchronized int read() throws IOException {
			return super.read();
		}

		@Override
		public synchronized int read(byte[] buffer, int offset, int byteCount) throws IOException {
			return super.read(buffer, offset, byteCount);
		}
	}

//	public void setServer(URL url) {
//
//		int port = url.getPort();
//		if (port < 0)
//			port = 80;
//
//		String host = url.getHost();
//		String path = url.getPath();
//		Log.d(TAG, "open database: " + host + " " + port + " " + path);
//
//		REQUEST_GET_START = ("GET " + path).getBytes();
//
//		REQUEST_GET_END = ("." + mExtension + " HTTP/1.1" +
//				"\nHost: " + host +
//				"\nConnection: Keep-Alive" +
//				"\n\n").getBytes();
//
//		mHost = host;
//		mPort = port;
//
//		mRequestBuffer = new byte[1024];
//		System.arraycopy(REQUEST_GET_START, 0,
//				mRequestBuffer, 0, REQUEST_GET_START.length);
//	}

	public void close() {
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

	public InputStream readHeader() throws IOException {

		InputStream is = mResponseStream;
		is.mark(4096);

		byte[] buf = buffer;
		boolean first = true;

		int read = 0;
		int pos = 0;
		int end = 0;
		int len = 0;

		mContentLength = -1;

		// header cannot be larger than BUFFER_SIZE for this to work
		for (; pos < read || (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0; len = 0) {
			read += len;

			// end of header lines
			while (end < read && (buf[end] != '\n'))
				end++;

			if (buf[end] == '\n') {
				if (first) {
					// check only for OK
					first = false;
					if (!check(HEADER_HTTP_OK, 6, buf, pos + 9, end)) {
						String line = new String(buf, pos, end - pos - 1);
						Log.d(TAG, ">" + line + "< ");
						return null;
					}
				} else if (end - pos == 1) {
					// check empty line (header end)
					end += 1;
					break;
				} else if (check(HEADER_CONTENT_TYPE, 12, buf, pos, end)) {
					if (!check(mContentType, mContentType.length, buf, pos + 14, end))
						return null;
				} else if (check(HEADER_CONTENT_LENGTH, 14, buf, pos, end)) {
					mContentLength =  parseInt(pos + 16, end-1, buf);

				}

				//String line = new String(buf, pos, end - pos - 1);
				//Log.d(TAG, ">" + line + "<  " + mContentLength);

				pos += (end - pos) + 1;
				end = pos;
			}
		}

		// back to start of content
		is.reset();
		is.mark(0);
		is.skip(end);

		if (mInflateContent)
			return new InflaterInputStream(is);

		return is;
	}

	public boolean sendRequest(Tile tile) throws IOException {

		if (mSocket != null && ((mMaxReq-- <= 0)
				|| (SystemClock.elapsedRealtime() - mLastRequest > RESPONSE_TIMEOUT))) {

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
			int avail = mResponseStream.available();
			if (avail > 0) {
				Log.d(TAG, "Consume left-over bytes: " + avail);

				while ((avail = mResponseStream.available()) > 0)
					mResponseStream.read(buffer);
				Log.d(TAG, "Consumed bytes");
			}
		}

		byte[] request = mRequestBuffer;
		int pos = REQUEST_GET_START.length;
		int newPos = 0;

		if ((newPos = formatTilePath(tile, request, pos)) == 0) {
			request[pos++] = '/';
			pos = writeInt(tile.zoomLevel, pos, request);
			request[pos++] = '/';
			pos = writeInt(tile.tileX, pos, request);
			request[pos++] = '/';
			pos = writeInt(tile.tileY, pos, request);
		} else {
			pos = newPos;
		}

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

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

		mCommandStream = mSocket.getOutputStream();
		mResponseStream = new BufferedInputStream(mSocket.getInputStream());

		return true;
	}

	// write (positive) integer to byte array
	protected static int writeInt(int val, int pos, byte[] buf) {
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
	// parse (positive) integer from byte array
	protected static int parseInt(int pos, int end, byte[] buf) {
		int val = 0;
		for (; pos < end; pos++)
			val = val * 10 + (buf[pos]) - '0';

		return val;
	}

	private static boolean check(byte[] string, int length, byte[] buffer,
			int position, int available) {

		if (available - position < length)
			return false;

		for (int i = 0; i < length; i++)
			if (buffer[position + i] != string[i])
				return false;

		return true;
	}

	public void requestCompleted() {
		mLastRequest = SystemClock.elapsedRealtime();
	}

	public int getContentLength(){
		return mContentLength;
	}
	/**
	 * Write custom tile url
	 *
	 * @param tile Tile
	 * @param path to write url string
	 * @param curPos current position
	 * @return new position
	 */
	protected int formatTilePath(Tile tile, byte[] path, int curPos) {
		return 0;
	}


}
