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

package org.oscim.theme;

import java.io.FileNotFoundException;
import java.io.InputStream;

import org.oscim.backend.CanvasAdapter;
import org.oscim.theme.IRenderTheme.ThemeException;
import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		InputStream inputStream = null;
		try {
			inputStream = theme.getRenderThemeAsStream();
			IRenderTheme t = XmlThemeBuilder.read(inputStream);

			if (t != null)
				t.scaleTextSize(CanvasAdapter.textScale + (CanvasAdapter.dpi / 240 - 1) * 0.5f);

			return t;
		} catch (FileNotFoundException e) {
			log.error(e.getMessage());
		} finally {
			IOUtils.closeQuietly(inputStream);
		}

		return null;
	}

}
