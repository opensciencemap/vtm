package org.xml.sax;

public class SAXParseException extends SAXException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public SAXParseException(String str) {
        super(str);
    }

    public SAXParseException(String str, Throwable throwable) {
        super(str);
    }

}
