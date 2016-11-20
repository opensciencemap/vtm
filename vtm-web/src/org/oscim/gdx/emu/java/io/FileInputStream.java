package java.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileInputStream extends InputStream {

    static final Logger log = LoggerFactory.getLogger(FileInputStream.class);

    public FileInputStream(File f) {

    }

    public FileInputStream(String s) throws FileNotFoundException {
        log.debug("FileInputStream {}", s);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }
}
