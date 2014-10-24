/*
 * Copyright 2012 Hannes Janetzek
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

import org.oscim.map.Map;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class Compass {

	private final SensorEventListener mListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			if (Math.abs(event.values[0] - mAngle) > 0.25) {
				mAngle = event.values[0];

				if (mMap != null) {
					mMap.viewport().setRotation(-mAngle);
					mMap.updateMap(true);
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}
	};

	/* package */float mAngle = 0;
	/* package */Map mMap;

	private final SensorManager mSensorManager;
	private final Sensor mSensor;

	@SuppressWarnings("deprecation")
	public Compass(Context context, Map map) {
		mMap = map;
		mSensorManager = (SensorManager) context
		    .getSystemService(Context.SENSOR_SERVICE);

		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
	}

	public void enable() {
		mSensorManager.registerListener(mListener, mSensor,
		                                SensorManager.SENSOR_DELAY_UI);
	}

	public void disable() {
		mSensorManager.unregisterListener(mListener);
		mMap.viewport().setRotation(0);
	}
}
