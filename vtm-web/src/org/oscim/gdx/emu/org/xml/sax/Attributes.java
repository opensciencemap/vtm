package org.xml.sax;

public abstract interface Attributes {
    public abstract int getLength();

    public abstract String getURI(int paramInt);

    public abstract String getLocalName(int paramInt);

    public abstract String getQName(int paramInt);

    public abstract String getType(int paramInt);

    public abstract String getValue(int paramInt);

    public abstract int getIndex(String paramString1, String paramString2);

    public abstract int getIndex(String paramString);

    public abstract String getType(String paramString1, String paramString2);

    public abstract String getType(String paramString);

    public abstract String getValue(String paramString1, String paramString2);

    public abstract String getValue(String paramString);
}
