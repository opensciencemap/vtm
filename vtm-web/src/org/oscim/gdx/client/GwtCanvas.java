/*
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
package org.oscim.gdx.client;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Paint;

import com.google.gwt.canvas.dom.client.Context2d;
import com.google.gwt.canvas.dom.client.Context2d.LineJoin;

public class GwtCanvas implements org.oscim.backend.canvas.Canvas {
	GwtBitmap bitmap;

	public GwtCanvas() {
		// canvas comes with gdx pixmap
	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		this.bitmap = (GwtBitmap) bitmap;
		Context2d ctx = this.bitmap.pixmap.getContext();

		ctx.clearRect(0, 0, this.bitmap.getWidth(), this.bitmap.getHeight());
		ctx.setLineJoin(LineJoin.BEVEL);
	}

	@Override
	public void drawText(String string, float x, float y, Paint fill, Paint stroke) {
		if (bitmap == null) {
			//log.debug("no bitmap set");
			return;
		}

		GwtPaint p = (GwtPaint) fill;

		if (p.stroke && GwtGdxGraphics.NO_STROKE_TEXT)
			return;

		Context2d ctx = bitmap.pixmap.getContext();
		ctx.setFont(p.font);

		if (p.stroke) {
			ctx.setLineWidth(p.strokeWidth);
			ctx.setStrokeStyle(p.color);
			ctx.strokeText(string, (int) (x + 1), (int) (y + 1));
		} else {
			ctx.setFillStyle(p.color);
			ctx.fillText(string, (int) (x + 1), (int) (y + 1));
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
	}
}
