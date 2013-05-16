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
package org.oscim.database.mapnik;

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

public class LwHttp {
	private static final String TAG = LwHttp.class.getName();
	private final static int BUFFER_SIZE = 65536;

	//
	byte[] buffer = new byte[BUFFER_SIZE];

	// position in buffer
	int bufferPos;

	// bytes available in buffer
	int bufferFill;

	// offset of buffer in message
	private int mBufferOffset;

	private String mHost;
	private int mPort;
	private InputStream mInputStream;

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private BufferedInputStream mResponseStream;
	long mLastRequest = 0;
	private SocketAddress mSockAddr;

	//private final static byte[] RESPONSE_HTTP_OK = "HTTP/1.1 200 OK".getBytes();
	private final static byte[] RESPONSE_HTTP_OK = "200 OK".getBytes();
	private final static byte[] RESPONSE_CONTENT_LEN = "Content-Length: ".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static int RESPONSE_EXPECTED_TIMEOUT = 10000;

	private byte[] REQUEST_GET_START;
	private byte[] REQUEST_GET_END;

	private byte[] mRequestBuffer;

	boolean setServer(String urlString) {
		urlString = "http://d1s11ojcu7opje.cloudfront.net/dev/764e0b8d";

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
		Log.d(TAG, "open database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();
		REQUEST_GET_END = (".vector.pbf HTTP/1.1\n" +
				"User-Agent: Wget/1.13.4 (linux-gnu)\n" +
				"Accept: */*\n" +
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
		mResponseStream.mark(1 << 16);

		byte[] buf = buffer;
		boolean first = true;
		int read = 0;
		int pos = 0;
		int end = 0;
		int len = 0;

		int contentLength = 0;

		// header cannot be larger than BUFFER_SIZE for this to work
		for (; pos < read || (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0; len = 0) {
			read += len;
			while (end < read && (buf[end] != '\n'))
				end++;

			if (buf[end] == '\n') {
				if (first) {
					// check only for OK
					first = false;
					if (!compareBytes(buf, pos + 9, end, RESPONSE_HTTP_OK, 6)){
						String line = new String(buf, pos, end - pos - 1);
						Log.d(TAG, ">" + line + "< ");
						return -1;
					}
				} else if (end - pos == 1) {
					// check empty line (header end)
					end += 1;
					break;
				}
				else {
					// parse Content-Length, TODO just encode this with message
					for (int i = 0; pos + i < end - 1; i++) {
						if (i < 16) {
							if (buf[pos + i] == RESPONSE_CONTENT_LEN[i])
								continue;

							break;
						}

						// read int value
						contentLength = contentLength * 10 + (buf[pos + i]) - '0';
					}
				}
				//String line = new String(buf, pos, end - pos - 1);
				//Log.d(TAG, ">" + line + "< ");

				pos += (end - pos) + 1;
				end = pos;
			}
		}

		// back to start of content
		mResponseStream.reset();
		mResponseStream.mark(0);
		mResponseStream.skip(end);

		// start of content
		bufferPos = 0;
		mBufferOffset = 0;

		// buffer fill
		bufferFill = 0;

		// decode zlib compressed content
		mInputStream = new InflaterInputStream(mResponseStream);

		return 1;
	}

	boolean sendRequest(Tile tile) throws IOException {

		bufferFill = 0;
		bufferPos = 0;
		//mReadPos = 0;

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

		request[pos++] = '/';
		request[pos++] = pos2hex(tile.tileX);
		request[pos++] = pos2hex(tile.tileY);
		request[pos++] = '/';
		pos = writeInt(tile.zoomLevel, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileX, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileY, pos, request);

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

		//Log.d(TAG, "request " + new String(request,0,len));
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

	private final static byte[] hexTable = {
			'0', '1', '2', '3', '4', '5', '6', '7',
			'8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static byte pos2hex(int pos){
		return hexTable[(pos % 16)];
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
		try {
			return readBuffer(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public int position() {
		return mBufferOffset + bufferPos;
	}

	public boolean readBuffer(int size) throws IOException {
		// check if buffer already contains the request bytes
		if (bufferPos + size < bufferFill)
			return true;

		// check if inputstream is read to the end
		//if (mReadPos == mReadEnd)
		//	return;

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

			// read until requested size is available in buffer
			int len = mInputStream.read(buffer, bufferFill, max);

			if (len < 0) {
				// finished reading, mark end
				buffer[bufferFill] = 0;
				return false;
			}

			bufferFill += len;
		}
		return true;
	}

}
