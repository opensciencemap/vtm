/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 Andrey Novikov
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
package org.oscim.renderer;

import org.oscim.core.MapPosition;
import org.oscim.map.Map;
import org.oscim.map.Viewport;

public class GLViewport extends Viewport {

    /**
     * Do not modify!
     */
    public final GLMatrix viewproj = mViewProjMatrix;
    /**
     * Do not modify!
     */
    public final GLMatrix proj = mProjMatrix;
    /**
     * Do not modify!
     */
    public final GLMatrix view = mViewMatrix;
    /**
     * Do not modify!
     */
    public final float[] plane = new float[8];

    /**
     * For temporary use, to setup MVP-Matrix
     */
    public final GLMatrix mvp = new GLMatrix();

    public final MapPosition pos = mPos;

    /**
     * Set MVP so that coordinates are in screen pixel coordinates with 0,0
     * being center
     */
    public void useScreenCoordinates(boolean center, float scale) {
        float invScale = 1f / scale;

        if (center)
            mvp.setScale(invScale, invScale, invScale);
        else
            mvp.setTransScale(-mWidth / 2, -mHeight / 2, invScale);

        mvp.multiplyLhs(proj);
    }

    /**
     * Set MVP offset in screen pixel coordinates
     */
    public void setScreenOffset(boolean center, int xOffset, int yOffset, float scale) {
        float invScale = 1f / scale;
        float x = center ? xOffset : -mWidth / 2 + xOffset;
        float y = center ? yOffset : -mHeight / 2 + yOffset;
        mvp.setTransScale(x, y, invScale);
        mvp.multiplyLhs(proj);
    }

    protected boolean changed;

    public boolean changed() {
        return changed;
    }

    void setFrom(Map map) {
        changed = map.viewport().getSyncViewport(this);
        getMapExtents(plane, 0);
    }

    public float getWidth() {
        return mWidth;
    }

    public float getHeight() {
        return mHeight;
    }
}
