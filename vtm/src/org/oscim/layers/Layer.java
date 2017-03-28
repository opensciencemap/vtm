/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2017 Longri
 * Copyright 2017 devemux86
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

import org.oscim.map.Map;
import org.oscim.renderer.LayerRenderer;

public abstract class Layer {

    public Layer(Map map) {
        mMap = map;
    }

    private boolean mEnabled = true;
    private EnableHandler mHandler;
    protected final Map mMap;

    protected LayerRenderer mRenderer;

    public LayerRenderer getRenderer() {
        return mRenderer;
    }

    /**
     * Enabled layers will be considered for rendering and receive onMapUpdate()
     * calls when they implement MapUpdateListener.
     *
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        boolean changed = mEnabled != enabled;
        mEnabled = enabled;
        if (mHandler != null && changed)
            mHandler.changed(enabled);
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnableHandler(EnableHandler handler) {
        mHandler = handler;
    }

    /**
     * Override to perform clean up of resources before shutdown.
     */
    public void onDetach() {
    }

    public Map map() {
        return mMap;
    }

    public interface EnableHandler {
        void changed(boolean enabled);
    }
}
