package javax.servlet;

/** ServletRequest
 *
 * @author peter.quiring
 */

import java.io.*;

public interface ServletRequest {
  public int getLocalPort();
  public String getLocalAddr();
  public boolean isSecure();
  public int getServerPort();
  public String getServerName();
  public int getRemotePort();
  public String getRemoteAddr();
  public String getRemoteHost();
  public String getScheme();
  public String getProtocol();
  public String getParameter(String name);

  public int getContentLength();
  public String getContentType();
  public ServletInputStream getInputStream();
  public BufferedReader getReader();
}
