/*
 * Copyright 2010, 2011, 2012 mapsforge.org
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
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

import org.oscim.backend.canvas.Bitmap;
import org.oscim.renderer.atlas.TextureRegion;

/**
 * Represents an icon on the map.
 */
public final class SymbolStyle extends RenderStyle<SymbolStyle> {

    public final Bitmap bitmap;
    public final TextureRegion texture;

    public SymbolStyle(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.texture = null;
    }

    public SymbolStyle(TextureRegion texture) {
        this.bitmap = null;
        this.texture = texture;
    }

    public SymbolStyle(SymbolBuilder<?> b) {
        this.bitmap = b.bitmap;
        this.texture = b.texture;
    }

    @Override
    public SymbolStyle current() {
        return (SymbolStyle) mCurrent;
    }

    @Override
    public void dispose() {
        if (bitmap != null)
            bitmap.recycle();
    }

    @Override
    public void renderNode(Callback cb) {
        cb.renderSymbol(this);
    }

    @Override
    public void renderWay(Callback cb) {
        cb.renderSymbol(this);
    }

    public static class SymbolBuilder<T extends SymbolBuilder<T>> extends StyleBuilder<T> {

        public Bitmap bitmap;
        public TextureRegion texture;

        public SymbolBuilder() {
        }

        public T set(SymbolStyle symbol) {
            if (symbol == null)
                return reset();

            this.bitmap = symbol.bitmap;
            this.texture = symbol.texture;

            return self();
        }

        public T bitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
            return self();
        }

        public T texture(TextureRegion texture) {
            this.texture = texture;
            return self();
        }

        public T reset() {
            bitmap = null;
            texture = null;
            return self();
        }

        public SymbolStyle build() {
            return new SymbolStyle(this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static SymbolBuilder<?> builder() {
        return new SymbolBuilder();
    }
}
