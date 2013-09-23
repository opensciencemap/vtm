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
package org.oscim.theme.renderinstruction;

import org.oscim.theme.IRenderTheme.Callback;

/**
 * A RenderInstruction is a basic graphical primitive to draw a map.
 */
public abstract class RenderInstruction {
	/**
	 * Destroys this RenderInstruction and cleans up all its internal resources.
	 */
	public void destroy() {
	}

	/**
	 * @param renderCallback
	 *            a reference to the receiver of all render callbacks.
	 */
	public void renderNode(Callback renderCallback) {
	}

	/**
	 * @param renderCallback
	 *            a reference to the receiver of all render callbacks.
	 */
	public void renderWay(Callback renderCallback) {
	}

	/**
	 * Scales the stroke width of this RenderInstruction by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the stroke width should be scaled.
	 */
	public void scaleStrokeWidth(float scaleFactor) {
	}

	/**
	 * Scales the text size of this RenderInstruction by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public void scaleTextSize(float scaleFactor) {
	}
}
