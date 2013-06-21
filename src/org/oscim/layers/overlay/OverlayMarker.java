/*
 * Copyright 2013
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
package org.oscim.layers.overlay;

import org.oscim.core.PointF;
import org.oscim.layers.overlay.OverlayItem.HotspotPlace;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class OverlayMarker {
	final Bitmap[] mBitmap;
	// Hotspot offset
	final PointF[] mOffset;

	public OverlayMarker(Bitmap bitmap, float relX, float relY) {
		mBitmap = new Bitmap[1];
		mBitmap[0] = bitmap;
		mOffset = new PointF[1];
		mOffset[0] = new PointF(relX, relY);
	}

	public OverlayMarker(Bitmap bitmap, HotspotPlace hotspot) {
		float x = 0, y = 0;
		switch (hotspot) {
			default:
			case NONE:
				break;
			case CENTER:
				x = 0.5f;
				y = 0.5f;
				break;
			case BOTTOM_CENTER:
				x = 0.5f;
				y = 1.0f;
				break;
			case TOP_CENTER:
				x = 0.5f;
				y = 0.0f;
				break;
			case RIGHT_CENTER:
				x = 1.0f;
				y = 0.5f;
				break;
			case LEFT_CENTER:
				x = 0.0f;
				y = 0.5f;
				break;
			case UPPER_RIGHT_CORNER:
				x = 1.0f;
				y = 0.0f;
				break;
			case LOWER_RIGHT_CORNER:
				x = 1.0f;
				y = 1.0f;
				break;
			case UPPER_LEFT_CORNER:
				x = 0.0f;
				y = 0.0f;
				break;
			case LOWER_LEFT_CORNER:
				x = 0.0f;
				y = 1.0f;
				break;
		}

		mBitmap = new Bitmap[1];
		mBitmap[0] = bitmap;
		mOffset = new PointF[1];
		mOffset[0] = new PointF(x, y);
	}

	public PointF getHotspot() {
		return mOffset[0];
	}

	public Bitmap getBitmap() {
		return mBitmap[0];
	}

	public static Bitmap drawableToBitmap(Drawable drawable) {
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		}

		android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
				drawable.getIntrinsicWidth(),
				drawable.getIntrinsicHeight(),
				Config.ARGB_8888);

		android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
		drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
		drawable.draw(canvas);

		return bitmap;
	}

	public static OverlayMarker makeMarker(Resources res, int id, HotspotPlace place) {
		return makeMarker(res.getDrawable(id), place);
	}

	public static OverlayMarker makeMarker(Drawable drawable, HotspotPlace place) {

		if (place == null)
			place = HotspotPlace.CENTER;

		return new OverlayMarker(drawableToBitmap(drawable), place);
	}
}
