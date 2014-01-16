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
package org.oscim.layers.marker;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.PointF;
import org.oscim.layers.marker.MarkerItem.HotspotPlace;

public class MarkerSymbol {
	final Bitmap[] mBitmap;
	// Hotspot offset
	final PointF mOffset;

	public MarkerSymbol(Bitmap bitmap, float relX, float relY) {
		mBitmap = new Bitmap[1];
		mBitmap[0] = bitmap;
		mOffset = new PointF(relX, relY);
	}

	public MarkerSymbol(Bitmap bitmap, HotspotPlace hotspot) {
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
		mOffset = new PointF(x, y);
	}

	public PointF getHotspot() {
		return mOffset;
	}

	public Bitmap getBitmap() {
		return mBitmap[0];
	}

}
