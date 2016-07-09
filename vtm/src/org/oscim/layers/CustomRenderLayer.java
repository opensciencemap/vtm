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
package org.oscim.layers;

import org.oscim.core.MapPosition;
import org.oscim.event.Event;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomRenderLayer extends Layer implements UpdateListener {

    static final Logger log = LoggerFactory.getLogger(CustomRenderLayer.class);

    class CustomRenderer extends LayerRenderer {

        // functions running on MapRender Thread
        @Override
        public void update(GLViewport v) {
            int currentState;

            synchronized (this) {
                currentState = someConccurentVariable;
                compile();
            }
            log.debug("state " + currentState);

        }

        protected void compile() {
            setReady(true);
        }

        @Override
        public void render(GLViewport v) {
        }
    }

    public CustomRenderLayer(Map map, LayerRenderer renderer) {

        super(map);
        mRenderer = new CustomRenderer();
    }

    private int someConccurentVariable;

    @Override
    public void onMapEvent(Event e, MapPosition mapPosition) {

        synchronized (mRenderer) {
            someConccurentVariable++;
        }
    }
}
