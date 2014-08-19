/**
 * Created by github.com/MarsVard on 19/08/14.
 * compass class with callbacks and listeners
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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.oscim.map.Map;
public class Compass implements SensorEventListener {

    private final SensorManager mSensorManager;
    private final Sensor mAccelerometer;
    private final Sensor mMagnetometer;
    private Map mMap = null;
    private CompassUpdateListener compassUpdateListener;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;

    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    public Compass(Context ctx, Map map){
        this(ctx);
        this.mMap = map;
    }

    public Compass(Context ctx) {
        // get SensorManager from Context
        mSensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);

        // get Sensors
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        // reset
        mLastAccelerometerSet = false;
        mLastMagnetometerSet = false;
    }

    /**
     * Enable sensor listeners, this should be called in onResume
     */
    public void enable() {
        // register sensors and this(listener)
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    /**
     * Disable sensor listeners, this should be called in onPause
     */
    public void disable() {
        // unregister listener
        mSensorManager.unregisterListener(this);
    }

    /**
     * get Compass angle in degrees
     *
     * @return float degrees angle
     */
    public float getAngleDegrees() {
        float azimuthInRadians = mOrientation[0];
        float azimuthInDegress = (float) Math.toDegrees(azimuthInRadians);
        if (azimuthInDegress < 0.0f) {
            azimuthInDegress += 360.0f;
        }
        return azimuthInDegress;
    }

    /**
     * set map that needs to be updated according to compass
     *
     * @param mMap
     */
    public void setMap(Map mMap) {
        this.mMap = mMap;
    }

    /**
     * unset map, disabling automatic updates to viewport
     */
    public void unsetMap() {
        this.mMap = null;
    }

    /**
     * set compass update listener to receive azimuth, pitch, roll values
     *
     * @param compassUpdateListener
     */
    public void setCompassUpdateListener(CompassUpdateListener compassUpdateListener) {
        this.compassUpdateListener = compassUpdateListener;
    }

    /**
     * unset compassUpdateListener
     */
    public void removeCompassUpdateListener() {
        this.compassUpdateListener = null;
    }

    // callback interface to listen for updates outside this class
    public interface CompassUpdateListener {
        public void compassUpdateReceived(float azimuth, float pitch, float roll);
    }

    //
    // sensor event listeners
    //
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // check sensore type
        // store latest sensor data locally
        if (sensorEvent.sensor == mAccelerometer) {
            System.arraycopy(sensorEvent.values, 0, mLastAccelerometer, 0, sensorEvent.values.length);
            mLastAccelerometerSet = true;
        } else if (sensorEvent.sensor == mMagnetometer) {
            System.arraycopy(sensorEvent.values, 0, mLastMagnetometer, 0, sensorEvent.values.length);
            mLastMagnetometerSet = true;
        }
        // if both sensor data has been received measure orientation
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);

            if (mMap != null) {
                mMap.viewport().setRotation(getAngleDegrees());
                mMap.updateMap(true);
            }

            // report compass orientation change
            if (compassUpdateListener != null) {
                compassUpdateListener.compassUpdateReceived(mOrientation[0], mOrientation[1], mOrientation[2]);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
