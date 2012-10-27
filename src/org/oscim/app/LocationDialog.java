/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.app;

import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.view.MapView;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class LocationDialog {

	void prepareDialog(MapView mapView, final Dialog dialog) {
		EditText editText = (EditText) dialog.findViewById(R.id.latitude);
		GeoPoint mapCenter = mapView.getMapPosition().getMapCenter();
		editText.setText(Double.toString(mapCenter.getLatitude()));

		editText = (EditText) dialog.findViewById(R.id.longitude);
		editText.setText(Double.toString(mapCenter.getLongitude()));

		SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
		zoomlevel.setMax(20); // FIXME
								// map.getMapGenerator().getZoomLevelMax());
		zoomlevel.setProgress(mapView.getMapPosition().getZoomLevel());

		final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
		textView.setText(String.valueOf(zoomlevel.getProgress()));
		zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
	}

	Dialog createDialog(final TileMap map) {
		AlertDialog.Builder builder = new AlertDialog.Builder(map);
		builder.setIcon(android.R.drawable.ic_menu_mylocation);
		builder.setTitle(R.string.menu_position_enter_coordinates);
		LayoutInflater factory = LayoutInflater.from(map);
		final View view = factory.inflate(R.layout.dialog_enter_coordinates, null);
		builder.setView(view);

		builder.setPositiveButton(R.string.go_to_position,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// disable GPS follow mode if it is enabled
						map.mLocation.disableSnapToLocation(true);

						// set the map center and zoom level
						EditText latitudeView = (EditText) view
								.findViewById(R.id.latitude);
						EditText longitudeView = (EditText) view
								.findViewById(R.id.longitude);
						double latitude = Double.parseDouble(latitudeView.getText()
								.toString());
						double longitude = Double.parseDouble(longitudeView.getText()
								.toString());

						SeekBar zoomLevelView = (SeekBar) view
								.findViewById(R.id.zoomLevel);

						byte zoom = (byte) (zoomLevelView.getProgress());

						MapPosition mapPosition = new MapPosition(latitude,
								longitude, zoom, 1, 0);

						map.map.setMapCenter(mapPosition);
					}
				});
		builder.setNegativeButton(R.string.cancel, null);
		return builder.create();
	}

	class SeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
		private final TextView textView;

		SeekBarChangeListener(TextView textView) {
			this.textView = textView;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			this.textView.setText(String.valueOf(progress));
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// do nothing
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// do nothing
		}
	}
}
