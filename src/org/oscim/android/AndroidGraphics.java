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
package org.oscim.android;

import org.oscim.layers.overlay.OverlayItem.HotspotPlace;
import org.oscim.layers.overlay.OverlayMarker;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class AndroidGraphics {

	public static OverlayMarker makeMarker(Resources res, int id, HotspotPlace place) {

		//		if (place == null)
		//			place = HotspotPlace.CENTER;
		//
		//Drawable drawable = ;
		//
		//		return new OverlayMarker(drawableToBitmap(drawable), place);
		return makeMarker(res.getDrawable(id), place);
	}

	public static OverlayMarker makeMarker(Drawable drawable, HotspotPlace place) {

		if (place == null)
			place = HotspotPlace.CENTER;

		//Drawable drawable = res.getDrawable(id);

		return new OverlayMarker(drawableToBitmap(drawable), place);
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
}
