package javax.servlet.http;

/** HttpServletReponse
 *
 * @author peter.quiring
 */

import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.*;

import javaforce.service.*;

public class HttpServletResponseImpl implements HttpServletResponse {
  private WebResponse res;

  public HttpServletResponseImpl(WebResponse res) {
    this.res = res;
  }

  public void setContentType(String mime) {
    res.setContentType(mime);
  }

  public void setContentLength(int length) {
    res.setContentLength(length);
  }

  public ServletOutputStream getOutputStream() {
    return new ServletOutputStream(res.getOutputStream());
  }

  public PrintWriter getWriter() {
    return new PrintWriter(getOutputStream());
  }

  public String getContentType() {
    return res.getContentType();
  }

  public void flushBuffer() {
    try {res.flush();} catch (Exception e) {}
  }
}
