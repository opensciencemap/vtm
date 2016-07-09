/*
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
package org.oscim.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import org.oscim.core.MapPosition;
import org.oscim.map.Map;

public class MapPreferences {
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_SCALE = "scale";

    private final String PREFERENCES_FILE;
    Context ctx;

    public MapPreferences(String name, Context ctx) {
        this.ctx = ctx;
        this.PREFERENCES_FILE = name;
    }

    public void clear() {
        Editor editor = ctx.getSharedPreferences(PREFERENCES_FILE, Activity.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    private void putDouble(Editor editor, String key, double value) {
        editor.putLong(key, Double.doubleToLongBits(value));
    }

    private double getDouble(SharedPreferences prefs, String key) {
        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }

    public void save(Map map) {
        save(map.getMapPosition());
    }

    public void save(MapPosition pos) {
        Editor editor = ctx.getSharedPreferences(PREFERENCES_FILE,
                Activity.MODE_PRIVATE).edit();
        editor.clear();
        putDouble(editor, KEY_LATITUDE, pos.y);
        putDouble(editor, KEY_LONGITUDE, pos.x);
        putDouble(editor, KEY_SCALE, pos.scale);
        editor.apply();
    }

    private static boolean containsViewport(SharedPreferences prefs) {
        return prefs.contains(KEY_LATITUDE)
                && prefs.contains(KEY_LONGITUDE)
                && prefs.contains(KEY_SCALE);
    }

    public boolean load(Map map) {
        MapPosition pos = map.getMapPosition();
        if (load(pos)) {
            map.setMapPosition(pos);
            return true;
        }
        return false;
    }

    public boolean load(MapPosition pos) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFERENCES_FILE,
                Activity.MODE_PRIVATE);

        if (containsViewport(prefs)) {
            pos.x = getDouble(prefs, KEY_LONGITUDE);
            pos.y = getDouble(prefs, KEY_LATITUDE);
            pos.scale = getDouble(prefs, KEY_SCALE);
            return true;
        }
        return false;
    }
}
