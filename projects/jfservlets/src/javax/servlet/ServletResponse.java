package javax.servlet;

/** ServletResponse
 *
 * @author peter.quiring
 */

import java.io.*;

public interface ServletResponse {
  public void setContentType(String mime);
  public void setContentLength(int length);

  public ServletOutputStream getOutputStream();
  public PrintWriter getWriter();
  public String getContentType();
  public void flushBuffer();
}
