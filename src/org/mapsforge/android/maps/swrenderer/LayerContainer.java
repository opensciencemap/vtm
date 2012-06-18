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
package org.mapsforge.android.maps.swrenderer;

import java.util.List;


import android.graphics.Paint;

class LayerContainer {

	final LevelContainer[] mLevels;
	final boolean[] mLevelActive;
	boolean mActive;

	LayerContainer(int levels) {
		mLevels = new LevelContainer[levels];
		mLevelActive = new boolean[levels];
		mActive = false;
	}

	List<ShapeContainer> add(int level, ShapeContainer shapeContainer, Paint paint) {
		mActive = true;
		LevelContainer levelContainer = mLevels[level];
		if (levelContainer == null) {
			levelContainer = new LevelContainer();
			mLevels[level] = levelContainer;
		}

		levelContainer.add(shapeContainer, paint);

		mLevelActive[level] = true;

		return levelContainer.mShapeContainers;
	}

	void clear() {
		if (!mActive)
			return;

		mActive = false;

		for (int level = mLevels.length - 1; level >= 0; level--) {
			if (mLevelActive[level]) {
				LevelContainer levelContainer = mLevels[level];
				mLevelActive[level] = false;
				levelContainer.clear();
			}
		}
	}
}
