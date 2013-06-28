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
package org.oscim.backend;

import java.io.IOException;
import java.io.InputStream;

import org.oscim.backend.canvas.Bitmap;

public final class BitmapUtils {
	public static Bitmap createBitmap(String src) throws IOException {
		if (src == null || src.length() == 0) {
			// no image source defined
			return null;
		}

		InputStream inputStream = AssetAdapter.g.openFileAsStream(src);
		if (inputStream == null)
			throw new IllegalArgumentException("invalid bitmap source: " + src);

		Bitmap bitmap = CanvasAdapter.g.decodeBitmap(inputStream);
		inputStream.close();
		return bitmap;
	}

	private BitmapUtils() {
	}

}
