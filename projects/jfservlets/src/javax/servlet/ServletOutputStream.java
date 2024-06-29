package javax.servlet;

/** ServletOutputStream
 *
 * @author peter.quiring
 */

import java.io.*;

public class ServletOutputStream extends OutputStream {
  private OutputStream os;
  public ServletOutputStream(OutputStream os) {
    this.os = os;
  }

  public void write(int v) throws IOException {
    os.write(v);
  }

  public void write(byte[] b) throws IOException {
    write(b, 0, b.length);
  }

  public void write(byte[] b, int off, int len) throws IOException {
    os.write(b, off, len);
  }

  public void flush() throws IOException {
    os.flush();
  }

  public void close() throws IOException {
    os.close();
  }
}
