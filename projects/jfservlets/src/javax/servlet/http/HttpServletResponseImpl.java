package javax.servlet.http;

/** HttpServletReponse
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javax.servlet.*;

public class HttpServletResponseImpl implements HttpServletResponse {
  private HashMap<String, Object> res;

  public HttpServletResponseImpl(HashMap<String, Object> res) {
    this.res = res;
  }

  public void setContentType(String mime) {
    res.put("ContentType", mime);
  }

  public void setContentLength(int length) {
    res.put("ContentLength", length);
  }

  public ServletOutputStream getOutputStream() {
    return new ServletOutputStream((OutputStream)res.get("OutputStream"));
  }

  public PrintWriter getWriter() {
    return (PrintWriter)res.get("Writer");
  }

  public String getContentType() {
    return (String)res.get("ContentType");
  }

  public void flushBuffer() {
    //TODO
  }
}
