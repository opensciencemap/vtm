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
package org.oscim.renderer;

import org.oscim.core.Tile;
import org.oscim.renderer.layer.TextItem;
import org.oscim.theme.renderinstruction.Text;

public final class WayDecorator {
	// /**
	// * Minimum distance in pixels before the symbol is repeated.
	// */
	// private static final int DISTANCE_BETWEEN_SYMBOLS = 200;

	// /**
	// * Minimum distance in pixels before the way name is repeated.
	// */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 100;

	// /**
	// * Distance in pixels to skip from both ends of a segment.
	// */
	// private static final int SEGMENT_SAFETY_DISTANCE = 30;

	// static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter,
	// boolean repeatSymbol, float[][] coordinates,
	// List<SymbolContainer> waySymbols) {
	// int skipPixels = SEGMENT_SAFETY_DISTANCE;
	//
	// // get the first way point coordinates
	// float previousX = coordinates[0][0];
	// float previousY = coordinates[0][1];
	//
	// // draw the symbol on each way segment
	// float segmentLengthRemaining;
	// float segmentSkipPercentage;
	// float symbolAngle;
	// for (int i = 2; i < coordinates[0].length; i += 2) {
	// // get the current way point coordinates
	// float currentX = coordinates[0][i];
	// float currentY = coordinates[0][i + 1];
	//
	// // calculate the length of the current segment (Euclidian distance)
	// float diffX = currentX - previousX;
	// float diffY = currentY - previousY;
	// double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
	// segmentLengthRemaining = (float) segmentLengthInPixel;
	//
	// while (segmentLengthRemaining - skipPixels > SEGMENT_SAFETY_DISTANCE) {
	// // calculate the percentage of the current segment to skip
	// segmentSkipPercentage = skipPixels / segmentLengthRemaining;
	//
	// // move the previous point forward towards the current point
	// previousX += diffX * segmentSkipPercentage;
	// previousY += diffY * segmentSkipPercentage;
	// symbolAngle = (float) Math.toDegrees(Math.atan2(currentY - previousY,
	// currentX - previousX));
	//
	// waySymbols.add(new SymbolContainer(symbolBitmap, previousX, previousY,
	// alignCenter, symbolAngle));
	//
	// // check if the symbol should only be rendered once
	// if (!repeatSymbol) {
	// return;
	// }
	//
	// // recalculate the distances
	// diffX = currentX - previousX;
	// diffY = currentY - previousY;
	//
	// // recalculate the remaining length of the current segment
	// segmentLengthRemaining -= skipPixels;
	//
	// // set the amount of pixels to skip before repeating the symbol
	// skipPixels = DISTANCE_BETWEEN_SYMBOLS;
	// }
	//
	// skipPixels -= segmentLengthRemaining;
	// if (skipPixels < SEGMENT_SAFETY_DISTANCE) {
	// skipPixels = SEGMENT_SAFETY_DISTANCE;
	// }
	//
	// // set the previous way point coordinates for the next loop
	// previousX = currentX;
	// previousY = currentY;
	// }
	// }

	public static TextItem renderText(float[] coordinates, String string, Text text,
			int pos, int len, TextItem textItems) {
		TextItem items = textItems;
		TextItem t = null;
		// calculate the way name length plus some margin of safety
		float wayNameWidth = -1;
		float minWidth = Tile.TILE_SIZE / 10;
		int skipPixels = 0;

		// get the first way point coordinates
		int prevX = (int) coordinates[pos + 0];
		int prevY = (int) coordinates[pos + 1];

		// find way segments long enough to draw the way name on them
		for (int i = pos + 2; i < pos + len; i += 2) {
			// get the current way point coordinates
			int curX = (int) coordinates[i];
			int curY = (int) coordinates[i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float vx = prevX - curX;
			float vy = prevY - curY;
			float a = (float) Math.sqrt(vx * vx + vy * vy);
			vx /= a;
			vy /= a;

			int last = i;
			int nextX = 0, nextY = 0;

			// add additional segments if possible
			for (int j = last + 2; j < pos + len; j += 2) {
				nextX = (int) coordinates[j];
				nextY = (int) coordinates[j + 1];

				float wx = curX - nextX;
				float wy = curY - nextY;

				a = (float) Math.sqrt(wx * wx + wy * wy);
				wx /= a;
				wy /= a;

				float ux = vx + wx;
				float uy = vy + wy;

				float diff = wx * uy - wy * ux;

				if (diff > 0.1 || diff < -0.1)
					break;

				last = j;
				curX = nextX;
				curY = nextY;
				continue;
			}

			vx = curX - prevX;
			vy = curY - prevY;

			if (vx < 0)
				vx = -vx;
			if (vy < 0)
				vy = -vy;

			// minimum segment to label
			if (vx + vy < minWidth) {
				// restart from next node
				prevX = (int) coordinates[i];
				prevY = (int) coordinates[i + 1];
				continue;
			}

			// compare against max segment length
			if (wayNameWidth > 0 && vx + vy < wayNameWidth) {
				// restart from next node
				prevX = (int) coordinates[i];
				prevY = (int) coordinates[i + 1];
				continue;
			}

			float segmentLength = (float) Math.sqrt(vx * vx + vy * vy);

			if (skipPixels > 0) {
				skipPixels -= segmentLength;

			} else if (segmentLength < minWidth) {
				// restart from next node
				prevX = (int) coordinates[i];
				prevY = (int) coordinates[i + 1];
				continue;
			}

			if (wayNameWidth < 0) {
				wayNameWidth = text.paint.measureText(string);
			}

			if (segmentLength < wayNameWidth * 0.50) {
				// restart from next node
				prevX = (int) coordinates[i];
				prevY = (int) coordinates[i + 1];
				continue;
			}

			//float s = (wayNameWidth + 20) / segmentLength;
			float s;

			//if (wayNameWidth < segmentLength)
			//	s = (segmentLength - 10) / segmentLength;
			//else
			s = (wayNameWidth + 20) / segmentLength;

			float width, height;
			float x1, y1, x2, y2;

			if (prevX < curX) {
				x1 = prevX;
				y1 = prevY;
				x2 = curX;
				y2 = curY;
			} else {
				x1 = curX;
				y1 = curY;
				x2 = prevX;
				y2 = prevY;
			}

//			// estimate position of text on path
//			width = (x2 - x1) / 2f;
//			//width += 4 * (width / wayNameWidth);
//			x2 = x2 - (width - s * width);
//			x1 = x1 + (width - s * width);
//
//			height = (y2 - y1) / 2f;
//			//height += 4 * (height / wayNameWidth);
//			y2 = y2 - (height - s * height);
//			y1 = y1 + (height - s * height);

			TextItem n = TextItem.get();

			// link items together
			if (t != null) {
				t.n1 = n;
				n.n2 = t;
			}

			t = n;
			t.x = x1 + (x2 - x1) / 2f;
			t.y = y1 + (y2 - y1) / 2f;
			t.string = string;
			t.text = text;
			t.width = wayNameWidth;
			t.x1 = x1;
			t.y1 = y1;
			t.x2 = x2;
			t.y2 = y2;
			t.length = (short) segmentLength;

			t.next = items;
			items = t;

			skipPixels = DISTANCE_BETWEEN_WAY_NAMES;

			// skip to last
			i = last;
			// store the previous way point coordinates
			prevX = curX;
			prevY = curY;
		}
		return items;
	}

	private WayDecorator() {
		throw new IllegalStateException();
	}
}
