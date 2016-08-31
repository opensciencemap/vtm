/*
 * Copyright 2016 Erik Duisters
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
package org.oscim.scalebar;

import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLState;
import org.oscim.renderer.GLViewport;

public class MapScaleBarRenderer extends BitmapRenderer {
    @Override
    public synchronized void render(GLViewport v) {
        GLState.test(false, false);
        super.render(v);
    }
}
