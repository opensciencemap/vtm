/*
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
package org.oscim.theme;

import org.oscim.backend.CanvasAdapter;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.InputStream;

public class ThemeLoader {
    static final Logger log = LoggerFactory.getLogger(ThemeLoader.class);

    /**
     * Load theme from XML file.
     *
     * @throws FileNotFoundException
     * @throws ThemeException
     */
    public static IRenderTheme load(String renderThemePath) throws ThemeException,
            FileNotFoundException {
        return load(new ExternalRenderTheme(renderThemePath));
    }

    public static IRenderTheme load(ThemeFile theme) throws ThemeException {

        try {
            InputStream is = theme.getRenderThemeAsStream();
            return load(theme.getRelativePathPrefix(), is);
        } catch (FileNotFoundException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public static IRenderTheme load(InputStream inputStream) throws ThemeException {
        return load("", inputStream);
    }

    public static IRenderTheme load(String relativePathPrefix, InputStream inputStream) throws ThemeException {

        try {
            IRenderTheme t = XmlThemeBuilder.read(relativePathPrefix, inputStream);
            if (t != null)
                t.scaleTextSize(CanvasAdapter.textScale + (CanvasAdapter.dpi / 240 - 1) * 0.5f);
            return t;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
