package java.net;

public class URL {
    @Override
    public String toString() {
        return mPath;
    }

    String mProtocol;
    String mHostname;
    int mPort;
    String mPath;

    public URL(String protocol, String hostName, int port, String path) {
        //mPath = "http://" +hostName +"/" + path;
        mPath = path;
    }

    public URL(String path) throws MalformedURLException {
        mPath = path;
    }
}
