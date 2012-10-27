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
package org.oscim.app;


public class MapInfoDialog {
	//onCreate
	//	else if (id == DIALOG_INFO_MAP_FILE) {
	//		builder.setIcon(android.R.drawable.ic_menu_info_details);
	//		builder.setTitle(R.string.menu_info_map_file);
	//		LayoutInflater factory = LayoutInflater.from(this);
	//		builder.setView(factory.inflate(R.layout.dialog_info_map_file, null));
	//		builder.setPositiveButton(R.string.ok, null);
	//		return builder.create();

	//onPrepare
	// } else if (id == DIALOG_INFO_MAP_FILE) {
	// MapInfo mapInfo = map.getMapDatabase().getMapInfo();
	//
	// TextView textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewName);
	// textView.setText(map.getMapFile());
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewSize);
	// textView.setText(FileUtils.formatFileSize(mapInfo.fileSize,
	// getResources()));
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewVersion);
	// textView.setText(String.valueOf(mapInfo.fileVersion));
	//
	// // textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewDebug);
	// // if (mapFileInfo.debugFile) {
	// // textView.setText(R.string.info_map_file_debug_yes);
	// // } else {
	// // textView.setText(R.string.info_map_file_debug_no);
	// // }
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewDate);
	// Date date = new Date(mapInfo.mapDate);
	// textView.setText(DateFormat.getDateTimeInstance().format(date));
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewArea);
	// BoundingBox boundingBox = mapInfo.boundingBox;
	// textView.setText(boundingBox.getMinLatitude() + ", "
	// + boundingBox.getMinLongitude() + " - \n"
	// + boundingBox.getMaxLatitude() + ", " +
	// boundingBox.getMaxLongitude());
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewStartPosition);
	// GeoPoint startPosition = mapInfo.startPosition;
	// if (startPosition == null) {
	// textView.setText(null);
	// } else {
	// textView.setText(startPosition.getLatitude() + ", "
	// + startPosition.getLongitude());
	// }
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewStartZoomLevel);
	// Byte startZoomLevel = mapInfo.startZoomLevel;
	// if (startZoomLevel == null) {
	// textView.setText(null);
	// } else {
	// textView.setText(startZoomLevel.toString());
	// }
	//
	// textView = (TextView) dialog
	// .findViewById(R.id.infoMapFileViewLanguagePreference);
	// textView.setText(mapInfo.languagePreference);
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewComment);
	// textView.setText(mapInfo.comment);
	//
	// textView = (TextView)
	// dialog.findViewById(R.id.infoMapFileViewCreatedBy);
	// textView.setText(mapInfo.createdBy);
}
