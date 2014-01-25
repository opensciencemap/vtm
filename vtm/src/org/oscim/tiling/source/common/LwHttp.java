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
package org.oscim.tiling.source.common;

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
import org.oscim.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight HTTP connection for tile loading.
 * 
 * Default tile url format is 'z/x/y'. Override formatTilePath() for a
 * different format.
 */
public class LwHttp {
	static final Logger log = LoggerFactory.getLogger(LwHttp.class);
	static final boolean DBG = false;

	private final static byte[] HEADER_HTTP_OK = "200 OK".getBytes();
	private final static byte[] HEADER_CONTENT_TYPE = "Content-Type".getBytes();
	private final static byte[] HEADER_CONTENT_LENGTH = "Content-Length".getBytes();
	private final static int RESPONSE_EXPECTED_LIVES = 100;
	private final static long RESPONSE_TIMEOUT = (long) 10E9; // 10 second in nanosecond

	private final static int BUFFER_SIZE = 8192;
	private final byte[] buffer = new byte[BUFFER_SIZE];

	private final String mHost;
	private final int mPort;

	private int mMaxReq = 0;
	private Socket mSocket;
	private OutputStream mCommandStream;
	private Buffer mResponseStream;
	private long mLastRequest = 0;
	private SocketAddress mSockAddr;

	private final byte[] REQUEST_GET_START;
	private final byte[] REQUEST_GET_END;
	private final byte[] mRequestBuffer;

	private final boolean mInflateContent;
	private final byte[] mContentType;

	/**
	 * @param url
	 *            Base url for tiles
	 * @param contentType
	 *            Expected Content-Type
	 * @param extension
	 *            'file' extension, usually .png
	 * @param deflate
	 *            true when content uses gzip compression
	 */
	public LwHttp(URL url, String contentType, String extension, boolean deflate) {
		mContentType = contentType.getBytes();
		mInflateContent = deflate;

		int port = url.getPort();
		if (port < 0)
			port = 80;

		String host = url.getHost();
		String path = url.getPath();
		log.debug("open database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();

		REQUEST_GET_END = (extension + " HTTP/1.1" +
		        "\nHost: " + host +
		        "\nConnection: Keep-Alive" +
		        "\n\n").getBytes();

		mHost = host;
		mPort = port;

		mRequestBuffer = new byte[1024];
		System.arraycopy(REQUEST_GET_START, 0,
		                 mRequestBuffer, 0, REQUEST_GET_START.length);
	}

	// TODO:
	// to avoid a copy in PbfDecoder one could manage the buffer
	// array directly and provide access to it.
	static class Buffer extends BufferedInputStream {
		OutputStream mCache;
		int sumRead = 0;
		int marked = -1;
		int mContentLength;

		public Buffer(InputStream is) {
			super(is, BUFFER_SIZE);
		}

		public void setCache(OutputStream cache) {
			mCache = cache;
		}

		public void start(int length) {
			sumRead = 0;
			mContentLength = length;
		}

		public boolean finishedReading() {
			return sumRead == mContentLength;
		}

		@Override
		public synchronized void mark(int readlimit) {
			marked = sumRead;
			super.mark(readlimit);
		}

		@Override
		public synchronized void reset() throws IOException {
			if (marked >= 0)
				sumRead = marked;
			// TODO could check if the mark is  already invalid
			super.reset();
		}

		@Override
		public int read() throws IOException {
			if (sumRead >= mContentLength)
				return -1;

			int data = super.read();

			sumRead += 1;

			if (DBG)
				log.debug("read {} {}", sumRead, mContentLength);

			if (mCache != null)
				mCache.write(data);

			return data;
		}

		@Override
		public int read(byte[] buffer, int offset, int byteCount)
		        throws IOException {

			if (sumRead >= mContentLength)
				return -1;

			int len = super.read(buffer, offset, byteCount);

			if (DBG)
				log.debug("read {} {} {}", len, sumRead, mContentLength);

			if (len <= 0)
				return len;

			sumRead += len;

			if (mCache != null)
				mCache.write(buffer, offset, len);

			return len;
		}
	}

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

		Buffer is = mResponseStream;
		is.mark(BUFFER_SIZE);
		is.start(BUFFER_SIZE);

		byte[] buf = buffer;
		boolean first = true;
		boolean ok = true;

		int read = 0;
		int pos = 0;
		int end = 0;
		int len = 0;

		int contentLength = -1;

		// header may not be larger than BUFFER_SIZE for this to work
		for (; (pos < read) || ((read < BUFFER_SIZE) &&
		        (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0); len = 0) {

			read += len;
			// end of header lines
			while (end < read && (buf[end] != '\n'))
				end++;

			if (end == BUFFER_SIZE) {
				return null;
			}

			if (buf[end] != '\n')
				continue;

			// empty line (header end)
			if (end - pos == 1) {
				end += 1;
				break;
			}

			if (!ok) {
				// ignore until end of header
			} else if (first) {
				first = false;
				// check only for OK ("HTTP/1.? ".length == 9)
				if (!check(HEADER_HTTP_OK, buf, pos + 9, end))
					ok = false;

			} else if (check(HEADER_CONTENT_TYPE, buf, pos, end)) {
				// check that response contains the expected
				// Content-Type
				if (!check(mContentType, buf, pos +
				        HEADER_CONTENT_TYPE.length + 2, end))
					ok = false;

			} else if (check(HEADER_CONTENT_LENGTH, buf, pos, end)) {
				// parse Content-Length
				contentLength = parseInt(buf, pos +
				        HEADER_CONTENT_LENGTH.length + 2, end - 1);
			}

			if (!ok || DBG) {
				String line = new String(buf, pos, end - pos - 1);
				log.debug("> {} <", line);
			}

			pos += (end - pos) + 1;
			end = pos;
		}

		if (!ok)
			return null;

		// back to start of content
		is.reset();
		is.mark(0);
		is.skip(end);
		is.start(contentLength);

		if (mInflateContent)
			return new InflaterInputStream(is);

		return is;
	}

	public boolean sendRequest(Tile tile) throws IOException {

		if (mSocket != null && ((mMaxReq-- <= 0)
		        || (System.nanoTime() - mLastRequest > RESPONSE_TIMEOUT))) {

			close();

			if (DBG)
				log.debug("not alive  - recreate connection " + mMaxReq);
		}

		if (mSocket == null) {
			lwHttpConnect();
			// we know our server
			mMaxReq = RESPONSE_EXPECTED_LIVES;
			// log.debug("create connection");
		} else {
			int avail = mResponseStream.available();
			if (avail > 0) {
				log.debug("left-over bytes: " + avail);
				close();
				lwHttpConnect();
				// FIXME not sure if this is correct way to drain socket
				//while ((avail = mResponseStream.available()) > 0)
				//	mResponseStream.read(buffer);
			}
		}

		byte[] request = mRequestBuffer;
		int pos = REQUEST_GET_START.length;

		pos = formatTilePath(tile, request, pos);

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

		if (DBG)
			log.debug("request: {}", new String(request, 0, len));

		try {
			mCommandStream.write(request, 0, len);
			mCommandStream.flush();
			return true;
		} catch (IOException e) {
			log.debug("recreate connection");
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
		mResponseStream = new Buffer(mSocket.getInputStream());

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

		ArrayUtils.reverse(buf, pos, pos + i);

		return pos + i;
	}

	// parse (positive) integer from byte array
	protected static int parseInt(byte[] buf, int pos, int end) {
		int val = 0;
		for (; pos < end; pos++)
			val = val * 10 + (buf[pos]) - '0';

		return val;
	}

	private static boolean check(byte[] string, byte[] buffer,
	        int position, int available) {

		int length = string.length;

		if (available - position < length)
			return false;

		for (int i = 0; i < length; i++)
			if (buffer[position + i] != string[i])
				return false;

		return true;
	}

	public void setCache(OutputStream os) {
		mResponseStream.setCache(os);
	}

	public boolean requestCompleted(boolean success) {
		mLastRequest = System.nanoTime();
		mResponseStream.setCache(null);

		if (!mResponseStream.finishedReading()) {
			log.debug("invalid buffer position");
			close();
			return false;
		}

		if (!success) {
			close();
			return false;
		}

		return true;
	}

	/**
	 * Write custom tile url
	 * 
	 * @param tile Tile
	 * @param path to write url string
	 * @param curPos current position
	 * @return new position
	 */
	protected int formatTilePath(Tile tile, byte[] request, int pos) {
		request[pos++] = '/';
		pos = writeInt(tile.zoomLevel, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileX, pos, request);
		request[pos++] = '/';
		pos = writeInt(tile.tileY, pos, request);
		return pos;
	}

}
