/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.oscim.android.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * A simple start screen for the sample activities.
 */
public class Samples extends Activity {
	public static Logger log = LoggerFactory.getLogger(Samples.class);

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_samples);
		LinearLayout linearLayout = (LinearLayout) findViewById(R.id.samples);
		linearLayout.addView(createButton(SimpleMapActivity.class));
		linearLayout.addView(createButton(BitmapTileMapActivity.class));
		linearLayout.addView(createButton(MapsforgeMapActivity.class));
		linearLayout.addView(createButton(PathOverlayActivity.class));
		linearLayout.addView(createButton(MarkerOverlayActivity.class));
		linearLayout.addView(createButton(ThemeStylerActivity.class));
		linearLayout.addView(createButton(S3DBMapActivity.class));
		linearLayout.addView(createButton(JeoIndoorMapActivity.class));
		linearLayout.addView(createButton(OsmJsonMapActivity.class));
		linearLayout.addView(createButton(VectorLayerMapActivity.class));
	}

	private Button createButton(final Class<?> clazz) {
		return this.createButton(clazz, null);
	}

	private Button createButton(final Class<?> clazz, String text) {
		Button button = new Button(this);
		if (text == null) {
			button.setText(clazz.getSimpleName());
		} else {
			button.setText(text);
		}
		button.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(Samples.this, clazz));
			}
		});
		return button;
	}
}
