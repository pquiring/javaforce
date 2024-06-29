package javax.servlet;

/** ServletInputStream
 *
 * @author peter.quiring
 */

import java.io.*;

public class ServletInputStream extends InputStream {
  private InputStream is;
  public ServletInputStream(InputStream is) {
    this.is = is;
  }

  public int read() throws IOException {
    return is.read();
  }

  public int read(byte[] b) throws IOException {
    return read(b, 0, b.length);
  }

  public int read(byte[] b, int off, int len) throws IOException {
    return is.read(b, off, len);
  }

  public int available() throws IOException {
    return is.available();
  }

  public void close() throws IOException {
    is.close();
  }

  public void mark(int readlimit) {
    is.mark(readlimit);
  }

  public void reset() throws IOException {
    is.reset();
  }

  public boolean markSupported() {
    return is.markSupported();
  }
}
