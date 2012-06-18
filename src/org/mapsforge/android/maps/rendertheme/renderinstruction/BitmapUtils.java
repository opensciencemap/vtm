/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.mapsforge.android.maps.rendertheme.renderinstruction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Shader.TileMode;

final class BitmapUtils {
	private static final String PREFIX_FILE = "file:";
	private static final String PREFIX_JAR = "jar:";

	private static InputStream createInputStream(String src) throws FileNotFoundException {
		if (src.startsWith(PREFIX_JAR)) {
			String name = src.substring(PREFIX_JAR.length());
			InputStream inputStream = Thread.currentThread().getClass().getResourceAsStream(name);
			if (inputStream == null) {
				throw new FileNotFoundException("resource not found: " + src);
			}
			return inputStream;
		} else if (src.startsWith(PREFIX_FILE)) {
			File file = new File(src.substring(PREFIX_FILE.length()));
			if (!file.exists()) {
				throw new IllegalArgumentException("file does not exist: " + src);
			} else if (!file.isFile()) {
				throw new IllegalArgumentException("not a file: " + src);
			} else if (!file.canRead()) {
				throw new IllegalArgumentException("cannot read file: " + src);
			}
			return new FileInputStream(file);
		}
		throw new IllegalArgumentException("invalid bitmap source: " + src);
	}

	static Bitmap createBitmap(String src) throws IOException {
		if (src == null || src.length() == 0) {
			// no image source defined
			return null;
		}

		InputStream inputStream = createInputStream(src);
		Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
		inputStream.close();
		return bitmap;
	}

	static BitmapShader createBitmapShader(String src) throws IOException {
		Bitmap bitmap = BitmapUtils.createBitmap(src);
		if (bitmap == null) {
			return null;
		}

		return new BitmapShader(bitmap, TileMode.REPEAT, TileMode.REPEAT);
	}

	private BitmapUtils() {
		throw new IllegalStateException();
	}
}
