/*
 * Copyright 2010, 2011, 2012, 2013 mapsforge.org
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
package org.oscim.awt;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Canvas;
import org.oscim.backend.canvas.Paint;

public class AwtCanvas implements Canvas {

	Graphics2D canvas;

	public AwtCanvas() {

	}

	@Override
	public void setBitmap(Bitmap bitmap) {
		if (canvas != null)
			canvas.dispose();

		AwtBitmap awtBitamp = (AwtBitmap) bitmap;

		canvas = awtBitamp.bitmap.createGraphics();

		canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0));
		canvas.fillRect(0, 0, bitmap.getWidth(), bitmap.getHeight());

		canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

		//canvas.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
		//                        RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		canvas.setRenderingHint(RenderingHints.KEY_RENDERING,
		                        RenderingHints.VALUE_RENDER_QUALITY);
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		                        RenderingHints.VALUE_ANTIALIAS_ON);

	}

	private final AffineTransform tx = new AffineTransform();

	@Override
	public void drawText(String text, float x, float y, Paint fill, Paint stroke) {

		AwtPaint fillPaint = (AwtPaint) fill;

		if (stroke == null) {
			canvas.setColor(fillPaint.color);
			canvas.setFont(fillPaint.font);
			canvas.drawString(text, x + AwtPaint.TEXT_OFFSET, y);
		} else {
			AwtPaint strokePaint = (AwtPaint) stroke;

			canvas.setColor(strokePaint.color);
			canvas.setStroke(strokePaint.stroke);

			TextLayout tl = new TextLayout(text, fillPaint.font,
			                               canvas.getFontRenderContext());
			tx.setToIdentity();
			tx.translate(x, y);

			Shape s = tl.getOutline(tx);

			canvas.draw(s);
			canvas.setColor(fillPaint.color);
			canvas.fill(s);
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y) {
		throw new UnknownError("not implemented");
	}
}
