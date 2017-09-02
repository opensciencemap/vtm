/*
 * Copyright 2017 Longri
 * Copyright 2017 devemux86
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

import org.oscim.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.SAXParserFactory;

/**
 * A utility class with theme specific helper methods.
 */
public final class ThemeUtils {

    private static final Logger log = LoggerFactory.getLogger(ThemeUtils.class);

    /**
     * Check if the given InputStream is a Mapsforge render theme.
     */
    public static boolean isMapsforgeTheme(InputStream is) {
        try {
            final AtomicBoolean isMapsforgeTheme = new AtomicBoolean(false);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XMLReader xmlReader = factory.newSAXParser().getXMLReader();
            xmlReader.setContentHandler(new DefaultHandler() {
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    if (localName.equals("rendertheme")) {
                        isMapsforgeTheme.set(uri.equals("http://mapsforge.org/renderTheme"));
                        // We have all info, break parsing
                        throw new SAXTerminationException();
                    }
                }
            });
            try {
                xmlReader.parse(new InputSource(is));
            } catch (SAXTerminationException e) {
                // Do nothing
            }
            return isMapsforgeTheme.get();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    private ThemeUtils() {
    }
}
