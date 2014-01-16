/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
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
package org.oscim.theme.renderinstruction;

import org.oscim.theme.IRenderTheme.Callback;

/**
 * Represents an icon along a polyline on the map.
 */
public final class LineSymbol extends RenderInstruction {

	public final boolean alignCenter;
	// public final Bitmap bitmap;
	public final boolean repeat;
	public final String bitmap;

	public LineSymbol(String src, boolean alignCenter, boolean repeat) {
		super();

		this.bitmap = src;
		// this.bitmap = BitmapUtils.createBitmap(src);
		this.alignCenter = alignCenter;
		this.repeat = repeat;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderWaySymbol(this);
	}
}
