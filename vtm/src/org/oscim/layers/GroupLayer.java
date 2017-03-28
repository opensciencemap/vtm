/*
 * Copyright 2016-2017 devemux86
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

import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

/**
 * A layer which is a group of other layers.
 */
public class GroupLayer extends Layer {

    /**
     * The group of other layers.
     */
    public final List<Layer> layers = new ArrayList<>();

    public GroupLayer(Map map) {
        super(map);
    }

    @Override
    public void onDetach() {
        for (Layer layer : layers) {
            layer.onDetach();
        }
    }

    @Override
    public void setEnableHandler(EnableHandler handler) {
        for (Layer layer : layers) {
            layer.setEnableHandler(handler);
        }
    }
}
