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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.oscim.backend.Log;
import org.oscim.core.Tile;

import com.google.gwt.typedarrays.client.Uint8ArrayNative;
import com.google.gwt.typedarrays.shared.Uint8Array;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.google.gwt.xhr.client.XMLHttpRequest.ResponseType;

public class LwHttp {
	private static final String TAG = LwHttp.class.getName();


	private final static int BUFFER_SIZE = 1024;
	private final byte[] buffer = new byte[BUFFER_SIZE];

	//private final byte[] REQUEST_GET_START;
	private final byte[] REQUEST_GET_END;
	private final byte[] mRequestBuffer;

	private final boolean mInflateContent;
	private final String mContentType;

	private int mContentLength = -1;
	private XMLHttpRequest mHttpRequest;

	private ReadyStateChangeHandler mResponseHandler;

	public LwHttp(URL url, String contentType, String extension, boolean deflate) {
		mContentType = contentType;
		mInflateContent = deflate;

		REQUEST_GET_END = ("." + extension).getBytes();

		mRequestBuffer = new byte[1024];
	}

	static class Buffer extends InputStream {
		Uint8Array mBuffer;
		int mPos;
		int mEnd;
		public Buffer(Uint8Array buf) {
			mBuffer = buf;
			mPos = 0;
			mEnd = buf.byteLength();
		}

		@Override
		public synchronized int read() throws IOException {
			if (mPos < mEnd)
				return mBuffer.get(mPos++);

			return -1;
		}
	}

	public void close() {
		if (mHttpRequest != null)
			mHttpRequest.abort();
	}
	private String mCurrentUrl;

	private PbfTileDataSource mDataSource;
	public boolean sendRequest(Tile tile, PbfTileDataSource dataSource) throws IOException {
		mDataSource = dataSource;

		byte[] request = mRequestBuffer;
		int pos = 0;
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

		String url = "/tiles" + (new String(request, 0, pos + len));
		Log.d(TAG, "load " + url);
		mCurrentUrl = url;

		mHttpRequest = XMLHttpRequest.create();
		mHttpRequest.open("GET", url);
		mHttpRequest.setResponseType(ResponseType.ArrayBuffer);

		mResponseHandler = new ReadyStateChangeHandler() {

			@Override
			public void onReadyStateChange(XMLHttpRequest xhr) {
				int status = xhr.getStatus();
				int state = xhr.getReadyState();

				Log.d(TAG, mCurrentUrl + "response " + status + "/" + state);

				if (state == XMLHttpRequest.DONE && status == 200){
					Uint8Array buf =  Uint8ArrayNative.create(xhr.getResponseArrayBuffer());

					mDataSource.process(new Buffer(buf), buf.byteLength());
				} else if (state == XMLHttpRequest.DONE){
					mDataSource.process(null, 0);
				}
			}
		};

		mHttpRequest.setOnReadyStateChange(mResponseHandler);
		mHttpRequest.send();

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
	protected static int parseInt(byte[] buf, int pos, int end) {
		int val = 0;
		for (; pos < end; pos++)
			val = val * 10 + (buf[pos]) - '0';

		return val;
	}

	public void requestCompleted() {

		mHttpRequest.clearOnReadyStateChange();
		mHttpRequest = null;
	}

	public int getContentLength() {
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
