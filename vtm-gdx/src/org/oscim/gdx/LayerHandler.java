/*
 * Copyright 2016 devemux86
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
package org.oscim.gdx;

import com.badlogic.gdx.input.GestureDetector;

import org.oscim.event.Gesture;
import org.oscim.event.MotionEvent;
import org.oscim.map.Map;

public class LayerHandler extends GestureDetector.GestureAdapter {

    private final Map map;

    public LayerHandler(Map map) {
        this.map = map;
    }

    @Override
    public boolean longPress(float x, float y) {
        map.handleGesture(Gesture.LONG_PRESS, new GdxMotionEvent(MotionEvent.ACTION_DOWN, x, y));
        return true;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        map.handleGesture(Gesture.TAP, new GdxMotionEvent(MotionEvent.ACTION_UP, x, y, button));
        return false;
    }
}
