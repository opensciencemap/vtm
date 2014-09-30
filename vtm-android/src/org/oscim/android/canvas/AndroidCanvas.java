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
package org.oscim.android.canvas;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class AndroidCanvas implements Canvas {
	final android.graphics.Canvas canvas;

	public AndroidCanvas() {
		canvas = new android.graphics.Canvas();
	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		canvas.setBitmap(((AndroidBitmap) bitmap).mBitmap);
	}

	@Override
	public void drawText(String string, float x, float y, Paint fill, Paint stroke) {
		if (string != null) {
			if (stroke != null)
				canvas.drawText(string, x, y, ((AndroidPaint) stroke).mPaint);

			canvas.drawText(string, x, y, ((AndroidPaint) fill).mPaint);
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
		canvas.drawBitmap(((AndroidBitmap) bitmap).mBitmap, x, y, null);

	}

}
