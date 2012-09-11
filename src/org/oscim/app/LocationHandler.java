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

import org.oscim.core.GeoPoint;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;

public class LocationHandler {
	private static final int DIALOG_LOCATION_PROVIDER_DISABLED = 2;

	private MyLocationListener mLocationListener;
	private LocationManager mLocationManager;
	private boolean mShowMyLocation;

	private ToggleButton mSnapToLocationView;
	private boolean mSnapToLocation;

	/* package */final TileMap mTileMap;

	LocationHandler(TileMap tileMap) {
		mTileMap = tileMap;
		mLocationManager = (LocationManager) tileMap
				.getSystemService(Context.LOCATION_SERVICE);
		mLocationListener = new MyLocationListener(tileMap);

		mSnapToLocationView = (ToggleButton) tileMap
				.findViewById(R.id.snapToLocationView);

		mSnapToLocationView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if (isSnapToLocationEnabled()) {
					disableSnapToLocation(true);
				} else {
					enableSnapToLocation(true);
				}
			}
		});
	}

	boolean enableShowMyLocation(boolean centerAtFirstFix) {
		Log.d("TileMap", "enableShowMyLocation " + mShowMyLocation);

		if (!mShowMyLocation) {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_FINE);
			String bestProvider = mLocationManager.getBestProvider(criteria, true);

			if (bestProvider == null) {
				mTileMap.showDialog(DIALOG_LOCATION_PROVIDER_DISABLED);
				return false;
			}

			mShowMyLocation = true;

			Log.d("TileMap", "enableShowMyLocation " + mShowMyLocation);

			mLocationListener.setCenterAtFirstFix(centerAtFirstFix);

			mLocationManager.requestLocationUpdates(bestProvider, 1000, 0,
					mLocationListener);

			mSnapToLocationView.setVisibility(View.VISIBLE);

			return true;
		}
		return false;
	}

	void gotoLastKnownPosition() {
		Location currentLocation;
		Location bestLocation = null;
		for (String provider : mLocationManager.getProviders(true)) {
			currentLocation = mLocationManager.getLastKnownLocation(provider);
			if (currentLocation == null)
				continue;
			if (bestLocation == null
					|| currentLocation.getAccuracy() < bestLocation.getAccuracy()) {
				bestLocation = currentLocation;
			}
		}

		if (bestLocation != null) {
			GeoPoint point = new GeoPoint(bestLocation.getLatitude(),
					bestLocation.getLongitude());

			mTileMap.mMapView.setCenter(point);

		} else {
			mTileMap.showToastOnUiThread(mTileMap
					.getString(R.string.error_last_location_unknown));
		}
	}

	/**
	 * Disables the "show my location" mode.
	 * 
	 * @return ...
	 */
	boolean disableShowMyLocation() {
		if (mShowMyLocation) {
			mShowMyLocation = false;
			disableSnapToLocation(false);

			mLocationManager.removeUpdates(mLocationListener);
			// if (circleOverlay != null) {
			// mapView.getOverlays().remove(circleOverlay);
			// mapView.getOverlays().remove(itemizedOverlay);
			// circleOverlay = null;
			// itemizedOverlay = null;
			// }

			mSnapToLocationView.setVisibility(View.GONE);

			return true;
		}
		return false;
	}

	/**
	 * Returns the status of the "show my location" mode.
	 * 
	 * @return true if the "show my location" mode is enabled, false otherwise.
	 */
	boolean isShowMyLocationEnabled() {
		return mShowMyLocation;
	}

	/**
	 * Disables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void disableSnapToLocation(boolean showToast) {
		if (mSnapToLocation) {
			mSnapToLocation = false;
			mSnapToLocationView.setChecked(false);

			mTileMap.mMapView.setClickable(true);

			if (showToast) {
				mTileMap.showToastOnUiThread(mTileMap
						.getString(R.string.snap_to_location_disabled));
			}
		}
	}

	/**
	 * Enables the "snap to location" mode.
	 * 
	 * @param showToast
	 *            defines whether a toast message is displayed or not.
	 */
	void enableSnapToLocation(boolean showToast) {
		if (!mSnapToLocation) {
			mSnapToLocation = true;

			mTileMap.mMapView.setClickable(false);

			if (showToast) {
				mTileMap.showToastOnUiThread(mTileMap
						.getString(R.string.snap_to_location_enabled));
			}
		}
	}

	/**
	 * Returns the status of the "snap to location" mode.
	 * 
	 * @return true if the "snap to location" mode is enabled, false otherwise.
	 */
	boolean isSnapToLocationEnabled() {
		return mSnapToLocation;
	}

	class MyLocationListener implements LocationListener {
		private final TileMap tileMap;
		private boolean centerAtFirstFix;

		MyLocationListener(TileMap tileMap) {
			this.tileMap = tileMap;
		}

		@Override
		public void onLocationChanged(Location location) {

			Log.d("LocationListener", "onLocationChanged, "
					+ " lon:" + location.getLongitude()
					+ " lat:" + location.getLatitude());

			if (!isShowMyLocationEnabled()) {
				return;
			}

			GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());

			// this.advancedMapViewer.overlayCircle.setCircleData(point, location.getAccuracy());
			// this.advancedMapViewer.overlayItem.setPoint(point);
			// this.advancedMapViewer.circleOverlay.requestRedraw();
			// this.advancedMapViewer.itemizedOverlay.requestRedraw();

			if (this.centerAtFirstFix || isSnapToLocationEnabled()) {
				this.centerAtFirstFix = false;
				this.tileMap.mMapView.setCenter(point);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			// do nothing
		}

		@Override
		public void onProviderEnabled(String provider) {
			// do nothing
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// do nothing
		}

		boolean isCenterAtFirstFix() {
			return this.centerAtFirstFix;
		}

		void setCenterAtFirstFix(boolean centerAtFirstFix) {
			this.centerAtFirstFix = centerAtFirstFix;
		}
	}
}
