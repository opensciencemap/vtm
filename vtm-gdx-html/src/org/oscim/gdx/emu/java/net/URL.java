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
		mPath = "http://" +hostName +"/" + path;
	}

	public URL(String url) throws MalformedURLException {
		mPath = url;
	}
}
