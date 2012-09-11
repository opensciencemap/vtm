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
package org.oscim.theme;

import org.oscim.core.Tag;

class NegativeRule extends Rule {
	final AttributeMatcher mAttributeMatcher;

	NegativeRule(int element, int closed, byte zoomMin, byte zoomMax,
			AttributeMatcher attributeMatcher) {
		super(element, closed, zoomMin, zoomMax);

		mAttributeMatcher = attributeMatcher;
	}

	@Override
	boolean matchesNode(Tag[] tags, byte zoomLevel) {
		return mZoomMin <= zoomLevel && mZoomMax >= zoomLevel
				&& (mElement != Element.WAY)
				&& mAttributeMatcher.matches(tags);
	}

	@Override
	boolean matchesWay(Tag[] tags, byte zoomLevel, int closed) {
		return mZoomMin <= zoomLevel && mZoomMax >= zoomLevel
				&& (mElement != Element.NODE)
				&& (mClosed == closed || mClosed == Closed.ANY)
				&& mAttributeMatcher.matches(tags);
	}
}
