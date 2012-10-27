/*
 * Copyright 2012 osmdroid: M.Kergall
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

import java.util.ArrayList;

import org.oscim.core.BoundingBox;
import org.oscim.overlay.OverlayItem;
import org.osmdroid.location.FlickrPOIProvider;
import org.osmdroid.location.GeoNamesPOIProvider;
import org.osmdroid.location.NominatimPOIProvider;
import org.osmdroid.location.POI;
import org.osmdroid.location.PicasaPOIProvider;
import org.osmdroid.overlays.ExtendedOverlayItem;
import org.osmdroid.overlays.ItemizedOverlayWithBubble;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

public class POISearch {
	ArrayList<POI> mPOIs;
	ItemizedOverlayWithBubble<ExtendedOverlayItem> poiMarkers;
	AutoCompleteTextView poiTagText;

	final TileMap tileMap;

	POISearch(TileMap tileMap) {
		this.tileMap = tileMap;
		//POI markers:
		final ArrayList<ExtendedOverlayItem> poiItems = new ArrayList<ExtendedOverlayItem>();
		poiMarkers = new ItemizedOverlayWithBubble<ExtendedOverlayItem>(tileMap.map, tileMap,
				poiItems, null); //new POIInfoWindow(map));

		tileMap.map.getOverlays().add(poiMarkers);

		//				if (savedInstanceState != null) {
		//					mPOIs = savedInstanceState.getParcelableArrayList("poi");
		//					updateUIWithPOI(mPOIs);
		//				}
	}

	//	void init() {
	//
	//	}

	class POITask extends AsyncTask<Object, Void, ArrayList<POI>> {
		String mTag;

		@Override
		protected ArrayList<POI> doInBackground(Object... params) {
			mTag = (String) params[0];

			if (mTag == null || mTag.equals("")) {
				return null;
			} else if (mTag.equals("wikipedia")) {
				GeoNamesPOIProvider poiProvider = new GeoNamesPOIProvider("mkergall");
				//ArrayList<POI> pois = poiProvider.getPOICloseTo(point, 30, 20.0);
				//Get POI inside the bounding box of the current map view:
				BoundingBox bb = App.map.getBoundingBox();
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, 30);
				return pois;
			} else if (mTag.equals("flickr")) {
				FlickrPOIProvider poiProvider = new FlickrPOIProvider(
						"c39be46304a6c6efda8bc066c185cd7e");
				BoundingBox bb = App.map.getBoundingBox();
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, 20);
				return pois;
			} else if (mTag.startsWith("picasa")) {
				PicasaPOIProvider poiProvider = new PicasaPOIProvider(null);
				BoundingBox bb = App.map.getBoundingBox();
				String q = mTag.substring("picasa".length());
				ArrayList<POI> pois = poiProvider.getPOIInside(bb, 20, q);
				return pois;
			}

			else {
				NominatimPOIProvider poiProvider = new NominatimPOIProvider();
				//	poiProvider.setService(NominatimPOIProvider.MAPQUEST_POI_SERVICE);
				poiProvider.setService(NominatimPOIProvider.NOMINATIM_POI_SERVICE);
				ArrayList<POI> pois;
				//				if (destinationPoint == null) {
				BoundingBox bb = App.map.getBoundingBox();
				pois = poiProvider.getPOIInside(bb, mTag, 100);
				//	} else {
				//		pois = poiProvider.getPOIAlong(mRoad.getRouteLow(), mTag, 100, 2.0);
				//	}
				return pois;
			}
		}

		@Override
		protected void onPostExecute(ArrayList<POI> pois) {
			mPOIs = pois;
			if (mTag.equals("")) {
				//no search, no message
			} else if (mPOIs == null) {
				Toast.makeText(tileMap.getApplicationContext(),
						"Technical issue when getting " + mTag + " POI.", Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(tileMap.getApplicationContext(),
						"" + mPOIs.size() + " " + mTag + " entries found",
						Toast.LENGTH_LONG).show();
				//				if (mTag.equals("flickr") || mTag.startsWith("picasa") || mTag.equals("wikipedia"))
				//					startAsyncThumbnailsLoading(mPOIs);
			}
			updateUIWithPOI(mPOIs);
		}
	}

	void updateUIWithPOI(ArrayList<POI> pois) {
		if (pois != null) {
			for (POI poi : pois) {
				int end = 0, first = 0;
				String desc = null;
				String name = poi.description;

				if (poi.serviceId == POI.POI_SERVICE_NOMINATIM) {
					if (name != null) {
						// FIXME or nominatim...
						//					String name = "";
						for (int i = 0; i < 3; i++) {
							int pos = poi.description.indexOf(',', end);
							if (pos > 0) {
								if (i == 0) {
									name = poi.description.substring(0, pos);
									first = pos + 2;
								}
								end = pos + 1;
							}
						}

						if (end > 0)
							desc = poi.description.substring(first, end - 1);
						else
							desc = poi.description;
					}
				}
				ExtendedOverlayItem poiMarker = new ExtendedOverlayItem(
						poi.type + (name == null ? "" : ": " + name), desc, poi.location);
				Drawable marker = null;

				if (poi.serviceId == POI.POI_SERVICE_NOMINATIM) {
					marker = App.res.getDrawable(R.drawable.marker_poi_default);
				} else if (poi.serviceId == POI.POI_SERVICE_GEONAMES_WIKIPEDIA) {
					if (poi.rank < 90)
						marker = App.res.getDrawable(R.drawable.marker_poi_wikipedia_16);
					else
						marker = App.res.getDrawable(R.drawable.marker_poi_wikipedia_32);
				} else if (poi.serviceId == POI.POI_SERVICE_FLICKR) {
					marker = App.res.getDrawable(R.drawable.marker_poi_flickr);
				} else if (poi.serviceId == POI.POI_SERVICE_PICASA) {
					marker = App.res.getDrawable(R.drawable.marker_poi_picasa_24);
					poiMarker.setSubDescription(poi.category);
				}
				poiMarker.setMarker(marker);
				poiMarker.setMarkerHotspot(OverlayItem.HotspotPlace.CENTER);
				//thumbnail loading moved in POIInfoWindow.onOpen for better performances. 
				poiMarker.setRelatedObject(poi);
				poiMarkers.addItem(poiMarker);
			}

			Log.d(App.TAG, "SEND INTENT");

			Intent intent = new Intent(tileMap.getApplicationContext(), POIActivity.class);
			intent.putParcelableArrayListExtra("POI", mPOIs);
			intent.putExtra("ID", poiMarkers.getBubbledItemId());
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
			tileMap.startActivityForResult(intent, TileMap.POIS_REQUEST);

		}

		App.map.redrawMap();
	}

	void getPOIAsync(String tag) {
		poiMarkers.removeAllItems();
		new POITask().execute(tag);
	}
}
