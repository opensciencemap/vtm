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
package org.oscim.tiling.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;

import org.oscim.core.Tile;
import org.oscim.utils.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight HTTP connection for tile loading. Does not do redirects,
 * https, full header parsing or stuff.
 */
public class LwHttp implements HttpEngine {
	static final Logger log = LoggerFactory.getLogger(LwHttp.class);
	static final boolean dbg = false;

	private final static byte[] HEADER_HTTP_OK = "200 OK".getBytes();
	//private final static byte[] HEADER_CONTENT_TYPE = "Content-Type".getBytes();
	private final static byte[] HEADER_CONTENT_LENGTH = "Content-Length".getBytes();
	private final static byte[] HEADER_CONNECTION_CLOSE = "Connection: close".getBytes();

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

	/** Server requested to close the connection */
	private boolean mMustClose;

	private final byte[] REQUEST_GET_START;
	private final byte[] REQUEST_GET_END;
	private final byte[] mRequestBuffer;

	/**
	 * @param url
	 *            Base url for tiles
	 */
	public LwHttp(URL url) {
		this(url, null);
	}

	public LwHttp(URL url, Map<String, String> header) {

		int port = url.getPort();
		if (port < 0)
			port = 80;

		String host = url.getHost();
		String path = url.getPath();
		log.debug("open database: " + host + " " + port + " " + path);

		REQUEST_GET_START = ("GET " + path).getBytes();

		String addRequest = "";
		if (header != null) {
			StringBuffer sb = new StringBuffer();
			for (Entry<String, String> l : header.entrySet())
				sb.append('\n').append(l.getKey()).append(": ").append(l.getValue());
			addRequest = sb.toString();
		}

		REQUEST_GET_END = (" HTTP/1.1" +
		        "\nUser-Agent: vtm/0.5.9" +
		        "\nHost: " + host +
		        "\nConnection: Keep-Alive" +
		        addRequest +
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
		OutputStream cache;
		int bytesRead = 0;
		int bytesWrote;
		int marked = -1;
		int contentLength;

		public Buffer(InputStream is) {
			super(is, BUFFER_SIZE);
		}

		public void setCache(OutputStream cache) {
			this.cache = cache;
		}

		public void start(int length) {
			bytesRead = 0;
			bytesWrote = 0;
			contentLength = length;
		}

		public boolean finishedReading() {
			try {
				while (bytesRead < contentLength && read() >= 0);
			} catch (IOException e) {
				log.debug(e.getMessage());
			}

			return bytesRead == contentLength;
		}

		@Override
		public void close() throws IOException {
			if (dbg)
				log.debug("close()... ignored");
		}

		@Override
		public synchronized void mark(int readlimit) {
			if (dbg)
				log.debug("mark {}", readlimit);

			marked = bytesRead;
			super.mark(readlimit);
		}

		@Override
		public synchronized long skip(long n) throws IOException {
			/* Android(4.1.2) image decoder *requires* skip to
			 * actually skip the requested amount.
			 * https://code.google.com/p/android/issues/detail?id=6066 */
			long sumSkipped = 0L;
			while (sumSkipped < n) {
				long skipped = super.skip(n - sumSkipped);
				if (skipped != 0) {
					sumSkipped += skipped;
					continue;
				}
				if (read() < 0)
					break; // EOF

				sumSkipped += 1;
				/* was incremented by read() */
				bytesRead -= 1;
			}

			if (dbg)
				log.debug("skip:{}/{} pos:{}", n, sumSkipped, bytesRead);

			bytesRead += sumSkipped;
			return sumSkipped;
		}

		@Override
		public synchronized void reset() throws IOException {
			if (dbg)
				log.debug("reset");

			if (marked >= 0)
				bytesRead = marked;
			/* TODO could check if the mark is already invalid */
			super.reset();
		}

		@Override
		public int read() throws IOException {
			if (bytesRead >= contentLength)
				return -1;

			int data = super.read();

			if (data >= 0)
				bytesRead += 1;

			//if (dbg)
			//	log.debug("read {} {}", bytesRead, contentLength);

			if (cache != null && bytesRead > bytesWrote) {
				bytesWrote = bytesRead;
				cache.write(data);
			}

			return data;
		}

		@Override
		public int read(byte[] buffer, int offset, int byteCount)
		        throws IOException {

			if (bytesRead >= contentLength)
				return -1;

			int len = super.read(buffer, offset, byteCount);

			if (dbg)
				log.debug("read {} {} {}", len, bytesRead, contentLength);

			if (len <= 0)
				return len;

			bytesRead += len;

			if (cache != null && bytesRead > bytesWrote) {
				int add = bytesRead - bytesWrote;
				bytesWrote = bytesRead;
				cache.write(buffer, offset + (len - add), add);
			}

			return len;
		}
	}

	public InputStream read() throws IOException {

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

		/* header may not be larger than BUFFER_SIZE for this to work */
		for (; (pos < read) || ((read < BUFFER_SIZE) &&
		        (len = is.read(buf, read, BUFFER_SIZE - read)) >= 0); len = 0) {

			read += len;
			/* end of header lines */
			while (end < read && (buf[end] != '\n'))
				end++;

			if (end == BUFFER_SIZE) {
				return null;
			}

			if (buf[end] != '\n')
				continue;

			/* empty line (header end) */
			if (end - pos == 1) {
				end += 1;
				break;
			}

			if (!ok) {
				/* ignore until end of header */
			} else if (first) {
				first = false;
				/* check only for OK ("HTTP/1.? ".length == 9) */
				if (!check(HEADER_HTTP_OK, buf, pos + 9, end))
					ok = false;

			} else if (check(HEADER_CONTENT_LENGTH, buf, pos, end)) {
				/* parse Content-Length */
				contentLength = parseInt(buf, pos +
				        HEADER_CONTENT_LENGTH.length + 2, end - 1);
			} else if (check(HEADER_CONNECTION_CLOSE, buf, pos, end)) {
				mMustClose = true;
			}
			//} else if (check(HEADER_CONTENT_TYPE, buf, pos, end)) {
			// check that response contains the expected
			// Content-Type
			//if (!check(mContentType, buf, pos +
			//        HEADER_CONTENT_TYPE.length + 2, end))
			//	ok = false;

			if (!ok || dbg) {
				String line = new String(buf, pos, end - pos - 1);
				log.debug("> {} <", line);
			}

			pos += (end - pos) + 1;
			end = pos;
		}

		if (!ok)
			return null;

		/* back to start of content */
		is.reset();
		is.mark(0);
		is.skip(end);
		is.start(contentLength);

		return is;
	}

	@Override
	public boolean sendRequest(UrlTileSource tileSource, Tile tile) throws IOException {

		if (mSocket != null) {
			if (mMaxReq-- <= 0)
				close();
			else if (System.nanoTime() - mLastRequest > RESPONSE_TIMEOUT)
				close();
			else {
				try {
					if (mResponseStream.available() > 0)
						close();
				} catch (IOException e) {
					log.debug(e.getMessage());
					close();
				}
			}
		}

		if (mSocket == null) {
			/* might throw IOException */
			lwHttpConnect();

			/* TODO parse from header */
			mMaxReq = RESPONSE_EXPECTED_LIVES;
		}

		byte[] request = mRequestBuffer;
		int pos = REQUEST_GET_START.length;

		pos = tileSource.formatTilePath(tile, request, pos);

		int len = REQUEST_GET_END.length;
		System.arraycopy(REQUEST_GET_END, 0, request, pos, len);
		len += pos;

		if (dbg)
			log.debug("request: {}", new String(request, 0, len));

		try {
			mCommandStream.write(request, 0, len);
			mCommandStream.flush();
			return true;
		} catch (IOException e) {
			log.debug("recreate connection");
			close();
			/* might throw IOException */
			lwHttpConnect();

			mCommandStream.write(request, 0, len);
			mCommandStream.flush();
		}

		return true;
	}

	private boolean lwHttpConnect() throws IOException {
		if (mSockAddr == null)
			mSockAddr = new InetSocketAddress(mHost, mPort);

		try {
			mSocket = new Socket();
			mSocket.connect(mSockAddr, 30000);
			mSocket.setTcpNoDelay(true);
			mCommandStream = mSocket.getOutputStream();
			mResponseStream = new Buffer(mSocket.getInputStream());
		} catch (IOException e) {
			close();
			throw e;
		}
		mMustClose = false;
		return true;
	}

	@Override
	public void close() {
		if (mSocket == null)
			return;

		try {
			mSocket.close();
		} catch (IOException e) {
		}
		mSocket = null;
		mCommandStream = null;
		mResponseStream = null;
	}

	@Override
	public void setCache(OutputStream os) {
		if (mResponseStream == null)
			return;

		mResponseStream.setCache(os);
	}

	@Override
	public boolean requestCompleted(boolean success) {
		if (mResponseStream == null)
			return false;

		mLastRequest = System.nanoTime();
		mResponseStream.setCache(null);

		if (!mResponseStream.finishedReading()) {
			//	StringBuffer sb = new StringBuffer();
			//	try {
			//		int val;
			//		while ((val = mResponseStream.read()) >= 0)
			//			sb.append((char) val);
			//	} catch (IOException e) {
			//
			//	}
			//log.debug("invalid buffer position {}", sb.toString());
			log.debug("invalid buffer position");
			close();
			return true;
		}

		if (!success) {
			close();
			return false;
		}

		if (mMustClose) {
			close();
			return true;
		}

		return true;
	}

	/** write (positive) integer to byte array */
	public static int writeInt(int val, int pos, byte[] buf) {
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

	/** parse (positive) integer from byte array */
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
}
