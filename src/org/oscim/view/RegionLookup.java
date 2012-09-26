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
package org.oscim.view;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import org.oscim.core.GeoPoint;
import org.oscim.core.MercatorProjection;

import android.os.AsyncTask;
import android.util.FloatMath;
import android.util.Log;
import android.widget.Toast;

public class RegionLookup {
	final static String TAG = RegionLookup.class.getName();

	class BBox {
		String level, name, boundary;
		GeoPoint min, max;
		float area;
	}

	private Connection connection = null;
	private PreparedStatement prepQuery = null;

	/* package */MapView mMapView;
	/* pacakge */BBox mSelected;

	// __get_regions_around(lat double precision, lon double precision)
	// returns table (level text, boundary text, name text, xmin double precision, ymin double precision, xmax double
	// precision, ymax double precision)
	// language sql as
	// $$
	// select admin_level, boundary, name, xmin(box), ymin(box), xmax(box), ymax(box) from
	// .. (select distinct on (box) * from
	// .... (select *, st_transform(st_envelope(way),4326) box
	// ...... from planet_polygon, st_transform(st_setsrid(st_makepoint($2,$1),4326),900913) point
	// ...... where (boundary is not null) and st_contains(way, point)
	// .... )p
	// .. )p order by st_area(box)
	// $$;
	private static final String QUERY = "SELECT * from __get_regions_around(?,?)";

	public RegionLookup(MapView mapView) {
		mMapView = mapView;
	}

	boolean connect() {
		Connection conn = null;
		String dburl = "jdbc:postgresql://city.informatik.uni-bremen.de:5432/gis";

		Properties dbOpts = new Properties();
		dbOpts.setProperty("user", "osm");
		dbOpts.setProperty("password", "osm");
		dbOpts.setProperty("socketTimeout", "50");
		dbOpts.setProperty("tcpKeepAlive", "true");

		try {
			DriverManager.setLoginTimeout(20);
			Log.d(TAG, "Creating JDBC connection...");

			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(dburl, dbOpts);
			connection = conn;
			prepQuery = conn.prepareStatement(QUERY);
		} catch (Exception e) {
			Log.d(TAG, "Aborted due to error:" + e);
			return false;
		}
		return true;
	}

	boolean updatePosition() {
		if (connection == null) {
			if (!connect())
				return false;
		}

		ResultSet r;

		MapPosition pos = mMapView.getMapPosition().getMapPosition();

		try {
			if (mPos == null) {
				prepQuery.setDouble(1, pos.lat);
				prepQuery.setDouble(2, pos.lon);
			} else {
				prepQuery.setDouble(1, mPos.getLatitude());
				prepQuery.setDouble(2, mPos.getLongitude());
			}

			Log.d(TAG, prepQuery.toString());

			prepQuery.execute();
			r = prepQuery.getResultSet();

		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return false;
		}

		ArrayList<BBox> boxes = new ArrayList<BBox>(10);

		try {
			while (r != null && r.next()) {
				BBox bbox = new BBox();

				bbox.level = r.getString(1);
				bbox.boundary = r.getString(2);
				bbox.name = r.getString(3);
				bbox.min = new GeoPoint(r.getDouble(5), r.getDouble(4));
				bbox.max = new GeoPoint(r.getDouble(7), r.getDouble(6));

				bbox.area = (float) Math.abs((bbox.max.getLatitude()
						- bbox.min.getLatitude())
						* (bbox.max.getLongitude()
						- bbox.min.getLongitude()));
				Log.d(TAG, "got:" + bbox.area
						+ " " + bbox.level + " b:" + bbox.boundary +
						" n:" + bbox.name + " " + bbox.min + " " + bbox.max);
				boxes.add(bbox);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return false;
		}

		double w = (mMapView.getWidth() >> 1) / pos.scale;
		double h = (mMapView.getHeight() >> 1) / pos.scale;

		float minx = (float) MercatorProjection
				.pixelXToLongitude(pos.x - w, pos.zoomLevel);
		float maxx = (float) MercatorProjection
				.pixelXToLongitude(pos.x + w, pos.zoomLevel);
		float miny = (float) MercatorProjection
				.pixelYToLatitude(pos.y - h, pos.zoomLevel);
		float maxy = (float) MercatorProjection
				.pixelYToLatitude(pos.y + h, pos.zoomLevel);

		float area = Math.abs((maxx - minx) * (miny - maxy));

		Log.d(TAG, " BBOX " + area + " " + minx + " "
				+ miny + " " + maxx + " " + maxy);

		w = mMapView.getWidth();
		h = mMapView.getHeight();
		mSelected = null;
		if (mDirection < 0) {
			// get the next larger bbox, union with current and set view region to it.
			for (int i = 0, n = boxes.size(); i < n; i++) {
				BBox bbox = boxes.get(i);
				if (bbox.area > area || i == n - 1) {
					mSelected = bbox;
					break;
				}
			}
		} else {
			for (int i = boxes.size() - 1; i >= 0; i--) {
				BBox bbox = boxes.get(i);
				// if (bbox.area < area) {
				//
				// double clat = bbox.min.getLatitude()
				// + (bbox.max.getLatitude() - bbox.min.getLatitude()) / 2;
				double clon = Math.abs(bbox.max.getLongitude()
						- bbox.min.getLongitude());

				float zoom = (float) (-Math.log(4) * Math.log(clon / 360) + 0.25);
				// float scale = 1 + (zoom - FloatMath.floor(zoom));
				// MapPosition mapPos = new MapPosition(clat,
				// bbox.min.getLongitude() + clon / 2,
				// (byte) zoom, 1 + (zoom - FloatMath.floor(zoom)), 0);

				mSelected = bbox;
				Log.d(TAG, "jump to: " + bbox.name + " " +
						-Math.log(4) * Math.log(clon / 360) + " " + (byte) zoom);

				if ((byte) zoom > pos.zoomLevel) {
					break;
				}
			}
		}
		if (mSelected != null) {
			BBox bbox = mSelected;

			double clat = bbox.min.getLatitude()
					+ (bbox.max.getLatitude() - bbox.min.getLatitude()) / 2;
			double clon = Math.abs(bbox.max.getLongitude() - bbox.min.getLongitude());

			Log.d(TAG, "jump to: " + bbox.name + " " +
					-Math.log(4) * Math.log(clon / 360));

			float zoom = (float) (-Math.log(4) * Math.log(clon / 360) + 0.25);

			MapPosition mapPos = new MapPosition(clat,
					bbox.min.getLongitude() + clon / 2,
					(byte) zoom, 1 + (zoom - FloatMath.floor(zoom)), 0);

			mMapView.setMapCenter(mapPos);

		}
		return true;
	}

	private int mDirection;
	private GeoPoint mPos;

	public synchronized void updateRegion(int direction, GeoPoint pos) {
		mDirection = direction;
		mPos = pos;

		new AsyncTask<Object, Integer, Long>() {
			@Override
			protected Long doInBackground(Object... params) {
				RegionLookup.this.updatePosition();
				return null;
			}

			@Override
			protected void onPostExecute(Long result) {
				// Log.d(TAG, "got sth " + result);
				if (mSelected != null) {
					Toast toast = Toast.makeText(mMapView.getContext(), mSelected.name,
							Toast.LENGTH_SHORT);
					// toast.setDuration(1000);
					toast.show();
				}

			}
		}.execute(null, null, null);
		mSelected = null;
	}

	// private final Handler mHandler = new Handler() {
	// @Override
	// public void handleMessage(Message msg) {
	// Log.d(TAG, "message: " + msg.what);
	// // switch (msg.what) {
	// // handle update
	// // .....
	// // }
	// }
	// };

}
