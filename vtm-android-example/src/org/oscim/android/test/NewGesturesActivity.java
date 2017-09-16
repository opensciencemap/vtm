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
package org.oscim.android.test;

import org.oscim.utils.Parameters;

public class NewGesturesActivity extends MarkerOverlayActivity {

    public NewGesturesActivity() {
        super();
        Parameters.MAP_EVENT_LAYER2 = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Revert gestures for other activities
        Parameters.MAP_EVENT_LAYER2 = false;
    }
}
