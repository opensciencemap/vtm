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
package org.oscim.stuff;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.oscim.core.MapPosition;
import org.oscim.view.MapView;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class RegionLookup {
	/* package */final static String TAG = RegionLookup.class.getName();

	private Connection connection = null;
	private PreparedStatement prepQuery = null;

	private MapView mMapView;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Log.d(TAG, "message: " + msg.what);
			// switch (msg.what) {
			// handle update
			// .....
			// }
		}
	};

	private static final String QUERY = "" +
			"SELECT * from __get_regions_around(?,?)";

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
			prepQuery.setDouble(1, pos.lat);
			prepQuery.setDouble(2, pos.lon);

			Log.d(TAG, "" + prepQuery.toString());
			prepQuery.execute();
			r = prepQuery.getResultSet();
		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return false;
		}
		String level, name, boundary;
		double minx, miny, maxx, maxy;
		try {
			while (r != null && r.next()) {
				level = r.getString(1);
				boundary = r.getString(2);
				name = r.getString(3);
				minx = r.getDouble(4);
				miny = r.getDouble(5);
				maxx = r.getDouble(6);
				maxy = r.getDouble(7);

				Log.d(TAG, "got:" + level + " b:" + boundary + " n:" + name + " " + minx
						+ " " + miny + " " + maxx + " " + maxy);

			}
		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return false;
		}
		return true;
	}

	public void updateRegion() {
		new AsyncTask<Object, Integer, Long>() {
			@Override
			protected Long doInBackground(Object... params) {
				RegionLookup.this.updatePosition();
				return null;
			}

			// @Override
			// protected void onPostExecute(Long result) {
			// Log.d(TAG, "got sth " + result);
			// }
		}.execute(null, null, null);
	}

}
