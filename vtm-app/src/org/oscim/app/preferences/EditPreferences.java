/*
 * Copyright 2010, 2011, 2012 mapsforge.org
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
package org.oscim.app.preferences;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import org.oscim.app.App;
import org.oscim.app.R;

/**
 * Activity to edit the application preferences.
 */
public class EditPreferences extends PreferenceActivity {
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        Preference button = (Preference) findPreference("clear_cache");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference arg0) {
                App.activity.getMapLayers().deleteCache();
                return true;
            }
        });
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_left, R.anim.slide_right2);
    }

    // @TargetApi(11)
    @Override
    protected void onResume() {
        super.onResume();

        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
        // getActionBar().hide();

        // check if the full screen mode should be activated
        // if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("fullscreen",
        // false)) {
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        // } else {
        // getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        // }
    }
}
