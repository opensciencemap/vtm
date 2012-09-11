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
package org.oscim.view.swrenderer;

import java.util.List;

import org.oscim.view.utils.GeometryUtils;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.util.Log;

final class WayDecorator {
	/**
	 * Minimum distance in pixels before the symbol is repeated.
	 */
	private static final int DISTANCE_BETWEEN_SYMBOLS = 200;

	/**
	 * Minimum distance in pixels before the way name is repeated.
	 */
	private static final int DISTANCE_BETWEEN_WAY_NAMES = 500;

	/**
	 * Distance in pixels to skip from both ends of a segment.
	 */
	private static final int SEGMENT_SAFETY_DISTANCE = 30;

	static void renderSymbol(Bitmap symbolBitmap, boolean alignCenter,
			boolean repeatSymbol, float[][] coordinates,
			List<SymbolContainer> waySymbols) {
		int skipPixels = SEGMENT_SAFETY_DISTANCE;

		// get the first way point coordinates
		float previousX = coordinates[0][0];
		float previousY = coordinates[0][1];

		// draw the symbol on each way segment
		float segmentLengthRemaining;
		float segmentSkipPercentage;
		float symbolAngle;
		for (int i = 2; i < coordinates[0].length; i += 2) {
			// get the current way point coordinates
			float currentX = coordinates[0][i];
			float currentY = coordinates[0][i + 1];

			// calculate the length of the current segment (Euclidian distance)
			float diffX = currentX - previousX;
			float diffY = currentY - previousY;
			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);
			segmentLengthRemaining = (float) segmentLengthInPixel;

			while (segmentLengthRemaining - skipPixels > SEGMENT_SAFETY_DISTANCE) {
				// calculate the percentage of the current segment to skip
				segmentSkipPercentage = skipPixels / segmentLengthRemaining;

				// move the previous point forward towards the current point
				previousX += diffX * segmentSkipPercentage;
				previousY += diffY * segmentSkipPercentage;
				symbolAngle = (float) Math.toDegrees(Math.atan2(currentY - previousY,
						currentX - previousX));

				waySymbols.add(new SymbolContainer(symbolBitmap, previousX, previousY,
						alignCenter, symbolAngle));

				// check if the symbol should only be rendered once
				if (!repeatSymbol) {
					return;
				}

				// recalculate the distances
				diffX = currentX - previousX;
				diffY = currentY - previousY;

				// recalculate the remaining length of the current segment
				segmentLengthRemaining -= skipPixels;

				// set the amount of pixels to skip before repeating the symbol
				skipPixels = DISTANCE_BETWEEN_SYMBOLS;
			}

			skipPixels -= segmentLengthRemaining;
			if (skipPixels < SEGMENT_SAFETY_DISTANCE) {
				skipPixels = SEGMENT_SAFETY_DISTANCE;
			}

			// set the previous way point coordinates for the next loop
			previousX = currentX;
			previousY = currentY;
		}
	}

	static void renderText(MapGenerator mapGenerator, Paint paint, Paint outline,
			float[] coordinates, WayDataContainer wayDataContainer,
			List<WayTextContainer> wayNames) {

		int pos = wayDataContainer.position[0];
		int len = wayDataContainer.length[0];
		// int coordinatesLength

		String text = null;
		// calculate the way name length plus some margin of safety
		float wayNameWidth = -1; // paint.measureText(textKey) + 5;
		float minWidth = 100;
		int skipPixels = 0;

		// get the first way point coordinates
		int previousX = (int) coordinates[pos + 0];
		int previousY = (int) coordinates[pos + 1];
		int containerSize = -1;

		// find way segments long enough to draw the way name on them
		for (int i = pos + 2; i < pos + len; i += 2) {
			// get the current way point coordinates
			int currentX = (int) coordinates[i];
			int currentY = (int) coordinates[i + 1];
			int first = i - 2;
			int last = i;

			// calculate the length of the current segment (Euclidian distance)
			int diffX = currentX - previousX;
			int diffY = currentY - previousY;

			for (int j = i + 2; j < pos + len; j += 2) {
				int nextX = (int) coordinates[j];
				int nextY = (int) coordinates[j + 1];

				if (diffY == 0) {
					if ((currentY - nextY) != 0)
						break;

					currentX = nextX;
					currentY = nextY;
					last = j;
					continue;
				} else if ((currentY - nextY) == 0)
					break;

				float diff = ((float) (diffX) / (diffY) - (float) (currentX - nextX)
						/ (currentY - nextY));

				// skip segments with corners
				if (diff >= 0.2 || diff <= -0.2)
					break;

				currentX = nextX;
				currentY = nextY;
				last = j;
			}

			diffX = currentX - previousX;
			diffY = currentY - previousY;

			if (diffX < 0)
				diffX = -diffX;
			if (diffY < 0)
				diffY = -diffY;

			if (diffX + diffY < minWidth) {
				previousX = currentX;
				previousY = currentY;
				continue;
			}

			if (wayNameWidth > 0 && diffX + diffY < wayNameWidth) {
				previousX = currentX;
				previousY = currentY;
				continue;
			}

			double segmentLengthInPixel = Math.sqrt(diffX * diffX + diffY * diffY);

			if (skipPixels > 0) {
				skipPixels -= segmentLengthInPixel;

			} else if (segmentLengthInPixel > minWidth) {

				if (wayNameWidth < 0) {
					if (text == null) {
						// text = mapGenerator.getWayName();
						// if (text == null)
						text = "blub";
					}

					wayNameWidth = (paint.measureText(text) + 10);
				}
				if (segmentLengthInPixel > wayNameWidth) {

					double s = (wayNameWidth + 10) / segmentLengthInPixel;
					int width, height;
					int x1, y1, x2, y2;

					if (previousX < currentX) {
						x1 = previousX;
						y1 = previousY;
						x2 = currentX;
						y2 = currentY;
					} else {
						x1 = currentX;
						y1 = currentY;
						x2 = previousX;
						y2 = previousY;
					}

					// estimate position of text on path
					width = (x2 - x1) / 2;
					x2 = x2 - (int) (width - s * width);
					x1 = x1 + (int) (width - s * width);

					height = (y2 - y1) / 2;
					y2 = y2 - (int) (height - s * height);
					y1 = y1 + (int) (height - s * height);

					short top = (short) (y1 < y2 ? y1 : y2);
					short bot = (short) (y1 < y2 ? y2 : y1);

					boolean intersects = false;

					if (containerSize == -1)
						containerSize = wayNames.size();

					for (int k = 0; k < containerSize; k++) {
						WayTextContainer wtc2 = wayNames.get(k);
						if (!wtc2.match)
							// outline (not 'match') are appended to the end;
							break;

						// check crossings
						if (GeometryUtils.lineIntersect(x1, y1, x2, y2, wtc2.x1, wtc2.y1,
								wtc2.x2, wtc2.y2)) {
							intersects = true;
							break;
						}

						// check overlapping labels of road with more than one
						// way
						short top2 = (wtc2.y1 < wtc2.y2 ? wtc2.y1 : wtc2.y2);
						short bot2 = (wtc2.y1 < wtc2.y2 ? wtc2.y2 : wtc2.y1);

						if (x1 - 10 < wtc2.x2 && wtc2.x1 - 10 < x2 && top - 10 < bot2
								&& top2 - 10 < bot) {

							if (wtc2.text.equals(text)) {
								intersects = true;
								break;
							}
						}
					}

					if (intersects) {
						previousX = (int) coordinates[pos + i];
						previousY = (int) coordinates[pos + i + 1];
						continue;
					}

					Log.d("mapsforge", "add " + text + " " + first + " " + last);
					WayTextContainer wtc = new WayTextContainer(first, last,
							wayDataContainer, text,
							paint);
					wtc.x1 = (short) x1;
					wtc.y1 = (short) y1;
					wtc.x2 = (short) x2;
					wtc.y2 = (short) y2;
					wtc.match = true;

					wayNames.add(0, wtc);
					containerSize++;

					if (outline != null) {
						wayNames.add(new WayTextContainer(first, last, wayDataContainer,
								text, outline));
						containerSize++;
					}
					// 500 ??? how big is a tile?!
					skipPixels = DISTANCE_BETWEEN_WAY_NAMES;
				}
			}

			// store the previous way point coordinates
			previousX = currentX;
			previousY = currentY;
		}
	}

	private WayDecorator() {
		throw new IllegalStateException();
	}
}
