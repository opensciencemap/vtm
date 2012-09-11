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

import java.util.ArrayList;


import android.graphics.Paint;

class LevelContainer {
	final ArrayList<ShapeContainer> mShapeContainers;
	Paint[] mPaint;

	LevelContainer() {
		mShapeContainers = new ArrayList<ShapeContainer>(20);
		mPaint = new Paint[2];
	}

	void add(ShapeContainer shapeContainer, Paint paint) {
		if (mPaint[0] == null)
			mPaint[0] = paint;
		else if (mPaint[0] != paint)
			mPaint[1] = paint;

		mShapeContainers.add(shapeContainer);
	}

	void clear() {
		mShapeContainers.clear();
		mPaint[0] = null;
		mPaint[1] = null;
	}
}
