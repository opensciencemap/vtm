package org.xml.sax;

import java.io.IOException;

public class SAXException extends IOException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SAXException(String str) {
        super(str);
    }

    public SAXException(String str, Throwable throwable) {
        super(str);
    }
}
