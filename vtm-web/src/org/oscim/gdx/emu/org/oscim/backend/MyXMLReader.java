package org.oscim.backend;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Node;
import com.google.gwt.xml.client.NodeList;
import com.google.gwt.xml.client.XMLParser;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;

public class MyXMLReader {
    public void parse(InputStream is) throws SAXException {
        StringBuilder sb = new StringBuilder();
        byte[] buf = new byte[8192];
        int read;
        try {
            while ((read = is.read(buf)) >= 0) {
                if (read > 0)
                    sb.append(new String(buf, 0, read));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Document d = XMLParser.parse(sb.toString());
        handleElement(d.getFirstChild());
        mHandler.endDocument();
    }

    int level = 0;

    void handleElement(Node n) throws SAXException {
        if (n == null) {
            return;
        }
        if (n.getNodeType() == Node.ELEMENT_NODE) {

            String localName = n.getNodeName();
            mHandler.startElement(null, localName, null, new MyAttributes(n));

            if (n.hasChildNodes()) {
                NodeList l = n.getChildNodes();
                for (int i = 0, len = l.getLength(); i < len; i++) {
                    handleElement(l.item(i));
                }
            }
            mHandler.endElement(null, localName, null);
        }

    }

    private DefaultHandler mHandler;

    public void setContentHandler(DefaultHandler handler) {
        mHandler = handler;
    }

}
