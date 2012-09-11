/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.view.glrenderer;

import org.oscim.theme.renderinstruction.Caption;
import org.oscim.theme.renderinstruction.PathText;

public class TextItem {
	TextItem next;

	final float x, y;
	final String text;
	final Caption caption;
	final PathText path;
	final float width;

	short x1, y1, x2, y2;

	public TextItem(float x, float y, String text, Caption caption) {
		this.x = x;
		this.y = y;
		this.text = text;
		this.caption = caption;
		this.width = caption.paint.measureText(text);
		this.path = null;
	}

	public TextItem(float x, float y, String text, PathText pathText, float width) {
		this.x = x;
		this.y = y;
		this.text = text;
		this.path = pathText;
		this.caption = null;
		this.width = width;
	}

}
