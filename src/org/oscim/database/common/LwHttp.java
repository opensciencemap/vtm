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
package org.oscim.database.common;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.zip.InflaterInputStream;

import org.oscim.core.Tile;

import android.os.SystemClock;
import android.util.Log;

public class LwHttp extends InputStream{
	private static final String TAG = LwHttp.class.getName();
	//private static final boolean DEBUG = false;

	private final static byte[] RESPONSE_HTTP_OK = "200 OK".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static int RESPONSE_TIMEOUT = 10000;

	private final static int BUFFER_SIZE = 1024;
	private final byte[] buffer = new byte[BUFFER_SIZE];

	private String mHost;
	private int mPort;

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private InputStream mResponseStream;
	private long mLastRequest = 0;
	private SocketAddress mSockAddr;

	private byte[] REQUEST_GET_START;
	private byte[] REQUEST_GET_END;
	private byte[] mRequestBuffer;

	private boolean mInflateContent;

	public boolean setServer(String urlString, String extension, boolean zlibDeflate) {
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
		REQUEST_GET_END = (extension + " HTTP/1.1\n" +
				"Host: " + host + "\n" +
				"Connection: Keep-Alive\n\n").getBytes();
		mInflateContent = zlibDeflate;

		mHost = host;
		mPort = port;

		mRequestBuffer = new byte[1024];
		System.arraycopy(REQUEST_GET_START, 0,
				mRequestBuffer, 0, REQUEST_GET_START.length);
		return true;
	}

	@Override
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
					if (!compareBytes(buf, pos + 9, end, RESPONSE_HTTP_OK, 6)) {
						String line = new String(buf, pos, end - pos - 1);
						Log.d(TAG, ">" + line + "< ");
						return null;
					}
				} else if (end - pos == 1) {
					// check empty line (header end)
					end += 1;
					break;
				}

				//String line = new String(buf, pos, end - pos - 1);
				//Log.d(TAG, ">" + line + "< ");

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
			if (avail > 0){
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
		mResponseStream = new BufferedInputStream(mSocket.getInputStream(), 4096);

		return true;
	}

	// write (positive) integer as char sequence to buffer
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

	public void requestCompleted() {
		mLastRequest = SystemClock.elapsedRealtime();
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

	@Override
	public int read() throws IOException {
		return mResponseStream.read();
	}


}
