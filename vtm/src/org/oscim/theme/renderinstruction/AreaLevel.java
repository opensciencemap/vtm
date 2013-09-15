/*
 * Copyright 2013 Hannes Janetzek
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

public class AreaLevel extends RenderInstruction {
	private final Area area;
	private final int level;

	public AreaLevel(Area area, int level) {
		this.area = area;
		this.level = level;
	}

	@Override
	public void renderWay(Callback renderCallback) {
		renderCallback.renderArea(this.area, level);
	}
}
