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
package org.oscim.theme.styles;

import org.oscim.theme.IRenderTheme.Callback;

/**
 * A RenderInstruction is a basic graphical primitive to draw a map.
 */
public abstract class RenderStyle {

	public interface StyleBuilder {
		RenderStyle build();

		StyleBuilder level(int level);
	}

	RenderStyle mCurrent = this;
	RenderStyle mNext;
	boolean update;

	public void set(RenderStyle next) {
		update = true;
		mNext = next;
	}

	public void unsetOverride() {
		update = true;
		mNext = null;
	}

	/**
	 * Destroys this RenderInstruction and cleans up all its internal resources.
	 */
	public void dispose() {
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
	 * Scales the text size of this RenderInstruction by the given factor.
	 * 
	 * @param scaleFactor
	 *            the factor by which the text size should be scaled.
	 */
	public void scaleTextSize(float scaleFactor) {
	}

	public void update() {
		if (update) {
			update = false;
			mCurrent = mNext;
		}
	}

	public abstract RenderStyle current();
}
