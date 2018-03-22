/*
 * Copyright 2018 devemux86
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
package org.oscim.test;

import org.oscim.core.MapPosition;

import java.util.prefs.Preferences;

public final class MapPreferences {

    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_SCALE = "scale";

    private static Preferences INSTANCE;

    private MapPreferences() {
    }

    private static Preferences getInstance() {
        if (INSTANCE == null)
            INSTANCE = Preferences.userRoot().node("vtm-playground");
        return INSTANCE;
    }

    public static MapPosition getMapPosition() {
        double latitude = getInstance().getDouble(KEY_LATITUDE, Double.NaN);
        double longitude = getInstance().getDouble(KEY_LONGITUDE, Double.NaN);
        double scale = getInstance().getDouble(KEY_SCALE, Double.NaN);
        if (Double.isNaN(latitude) || Double.isNaN(longitude) || Double.isNaN(scale))
            return null;
        return new MapPosition(latitude, longitude, scale);
    }

    public static void saveMapPosition(MapPosition mapPosition) {
        getInstance().putDouble(KEY_LATITUDE, mapPosition.getLatitude());
        getInstance().putDouble(KEY_LONGITUDE, mapPosition.getLongitude());
        getInstance().putDouble(KEY_SCALE, mapPosition.getScale());
    }
}
