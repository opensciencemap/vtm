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

/**
 * A JobParameters instance is a simple DTO to store the rendering parameters for a job.
 */
public class JobParameters {

	/**
	 * The render theme which should be used.
	 */
	public final Theme theme;

	/**
	 * The text scale factor which should applied to the render theme.
	 */
	public final float textScale;

	private final int mHashCodeValue;

	/**
	 * @param theme
	 *            render theme which should be used.
	 * @param textScale
	 *            the text scale factor which should applied to the render theme.
	 */
	public JobParameters(Theme theme, float textScale) {
		this.theme = theme;
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
		if (theme == null) {
			if (other.theme != null) {
				return false;
			}
		} else if (!theme.equals(other.theme)) {
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
		result = 31 * result + ((theme == null) ? 0 : theme.hashCode());
		result = 31 * result + Float.floatToIntBits(textScale);
		return result;
	}
}
