/*
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 devemux86
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
package org.oscim.layers;

import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.map.Map;
import org.oscim.renderer.GridRenderer;
import org.oscim.theme.styles.LineStyle;
import org.oscim.theme.styles.TextStyle;

public class TileGridLayer extends GenericLayer {

    public TileGridLayer(Map map) {
        super(map, new GridRenderer());
    }

    public TileGridLayer(Map map, float scale) {
        super(map, new GridRenderer(scale));
    }

    public TileGridLayer(Map map, int color, float width, int repeat) {
        super(map, new GridRenderer(repeat, new LineStyle(color, width, Cap.BUTT), null));
    }

    public TileGridLayer(Map map, int color, float width, TextStyle text, int repeat) {
        super(map, new GridRenderer(repeat, new LineStyle(color, width, Cap.BUTT), text));
    }
}
