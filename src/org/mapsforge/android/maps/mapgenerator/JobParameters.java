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
package org.mapsforge.android.maps.mapgenerator;

import java.io.Serializable;

/**
 * A JobParameters instance is a simple DTO to store the rendering parameters for a job.
 */
public class JobParameters implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * The render theme which should be used.
	 */
	public final JobTheme jobTheme;

	/**
	 * The text scale factor which should applied to the render theme.
	 */
	public final float textScale;

	private final int mHashCodeValue;

	/**
	 * @param jobTheme
	 *            render theme which should be used.
	 * @param textScale
	 *            the text scale factor which should applied to the render theme.
	 */
	public JobParameters(JobTheme jobTheme, float textScale) {
		this.jobTheme = jobTheme;
		this.textScale = textScale;
		mHashCodeValue = calculateHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof JobParameters)) {
			return false;
		}
		JobParameters other = (JobParameters) obj;
		if (jobTheme == null) {
			if (other.jobTheme != null) {
				return false;
			}
		} else if (!jobTheme.equals(other.jobTheme)) {
			return false;
		}
		if (Float.floatToIntBits(textScale) != Float.floatToIntBits(other.textScale)) {
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
		int result = 7;
		result = 31 * result + ((jobTheme == null) ? 0 : jobTheme.hashCode());
		result = 31 * result + Float.floatToIntBits(textScale);
		return result;
	}
}
