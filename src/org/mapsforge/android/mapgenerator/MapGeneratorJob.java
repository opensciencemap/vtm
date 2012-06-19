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
package org.mapsforge.android.mapgenerator;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import org.mapsforge.android.DebugSettings;

import android.graphics.Bitmap;

/**
 * A MapGeneratorJob holds all immutable rendering parameters for a single map image together with a mutable priority
 * field, which indicates the importance of this job.
 */
public class MapGeneratorJob implements Comparable<MapGeneratorJob>, Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The debug settings for this job.
	 */
	public final DebugSettings debugSettings;

	/**
	 * The rendering parameters for this job.
	 */
	public final JobParameters jobParameters;

	/**
	 * The tile which should be generated.
	 */
	public final MapTile tile;

	private transient int mHashCodeValue;
	private final MapGenerator mMapGenerator;
	private transient double mPriority;

	/**
	 * bitmap passed to renderer
	 */
	private Bitmap mBitmap;

	/**
	 * @return ...
	 */
	public Bitmap getBitmap() {
		return mBitmap;
	}

	/**
	 * @param bitmap
	 *            ..
	 */
	public void setBitmap(Bitmap bitmap) {
		mBitmap = bitmap;
	}

	private float mScale;

	/**
	 * @return scale the tile is rendered with
	 */
	public float getScale() {
		return mScale;
	}

	/**
	 * @param _scale
	 *            for the tile to be rendered
	 */
	public void setScale(float _scale) {
		mScale = _scale;
	}

	/**
	 * Creates a new job for a MapGenerator with the given parameters.
	 * 
	 * @param _tile
	 *            the tile which should be generated.
	 * @param mapGenerator
	 *            the MapGenerator for this job.
	 * @param _jobParameters
	 *            the rendering parameters for this job.
	 * @param _debugSettings
	 *            the debug settings for this job.
	 */
	public MapGeneratorJob(MapTile _tile, MapGenerator mapGenerator,
			JobParameters _jobParameters, DebugSettings _debugSettings) {
		tile = _tile;
		mMapGenerator = mapGenerator;
		jobParameters = _jobParameters;
		debugSettings = _debugSettings;
		calculateTransientValues();
	}

	@Override
	public int compareTo(MapGeneratorJob o) {
		if (mPriority < o.mPriority) {
			return -1;
		} else if (mPriority > o.mPriority) {
			return 1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MapGeneratorJob)) {
			return false;
		}
		MapGeneratorJob other = (MapGeneratorJob) obj;

		if (debugSettings == null) {
			if (other.debugSettings != null) {
				return false;
			}
		} else if (!debugSettings.equals(other.debugSettings)) {
			return false;
		}
		if (jobParameters == null) {
			if (other.jobParameters != null) {
				return false;
			}
		} else if (!jobParameters.equals(other.jobParameters)) {
			return false;
		}
		if (mMapGenerator != other.mMapGenerator) {
			return false;
		}
		if (tile == null) {
			if (other.tile != null) {
				return false;
			}
		} else if (!tile.equals(other.tile)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return mHashCodeValue;
	}

	/**
	 * @return the hash code of this object.
	 */
	private int calculateHashCode() {
		int result = 1;
		result = 31 * result + ((debugSettings == null) ? 0 : debugSettings.hashCode());
		result = 31 * result + ((jobParameters == null) ? 0 : jobParameters.hashCode());
		result = 31 * result + ((mMapGenerator == null) ? 0 : mMapGenerator.hashCode());
		result = 31 * result + ((tile == null) ? 0 : tile.hashCode());
		return result;
	}

	/**
	 * Calculates the values of some transient variables.
	 */
	private void calculateTransientValues() {
		mHashCodeValue = calculateHashCode();
	}

	private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
		objectInputStream.defaultReadObject();
		calculateTransientValues();
	}

	void setPriority(double priority) {
		mPriority = priority;
	}
}
