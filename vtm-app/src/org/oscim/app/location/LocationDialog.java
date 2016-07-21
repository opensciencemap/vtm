/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2012 Hannes Janetzek
 * Copyright 2016 devemux86
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
package org.oscim.app.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import org.oscim.app.App;
import org.oscim.app.R;
import org.oscim.app.TileMap;
import org.oscim.core.MapPosition;
import org.oscim.map.Map;

public class LocationDialog {

    public void prepareDialog(Map map, final Dialog dialog) {
        EditText editText = (EditText) dialog.findViewById(R.id.latitude);

        MapPosition mapCenter = map.getMapPosition();

        editText.setText(Double.toString(mapCenter.getLatitude()));

        editText = (EditText) dialog.findViewById(R.id.longitude);
        editText.setText(Double.toString(mapCenter.getLongitude()));

        SeekBar zoomlevel = (SeekBar) dialog.findViewById(R.id.zoomLevel);
        zoomlevel.setMax(20);
        zoomlevel.setProgress(10);

        final TextView textView = (TextView) dialog.findViewById(R.id.zoomlevelValue);
        textView.setText(String.valueOf(zoomlevel.getProgress()));
        zoomlevel.setOnSeekBarChangeListener(new SeekBarChangeListener(textView));
    }

    public Dialog createDialog(final TileMap map) {
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
                        //map.mLocation.disableSnapToLocation();
                        if (map.getLocationHandler().getMode() == LocationHandler.Mode.SNAP)
                            map.getLocationHandler()
                                    .setMode(LocationHandler.Mode.SHOW);

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

                        int zoom = zoomLevelView.getProgress();

                        MapPosition mapPosition = new MapPosition();
                        mapPosition.setPosition(latitude, longitude);
                        mapPosition.setZoomLevel(zoom);
                        App.map.setMapPosition(mapPosition);
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
