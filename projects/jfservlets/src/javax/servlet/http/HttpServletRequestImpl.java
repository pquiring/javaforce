package javax.servlet.http;

/** HttpServletRequest
 *
 * @author peter.quiring
 */

import java.io.*;

import javax.servlet.*;

import javaforce.service.*;

public class HttpServletRequestImpl implements HttpServletRequest {
  private WebRequest req;

  public HttpServletRequestImpl(WebRequest req) {
    this.req = req;
  }

  public int getLocalPort() {
    return req.getLocalPort();
  }

  public String getLocalAddr() {
    return req.getLocalAddr();
  }

  public boolean isSecure() {
    return req.isSecure();
  }

  public int getServerPort() {
    return req.getLocalPort();
  }

  public String getServerName() {
    return req.getLocalAddr();
  }

  public int getRemotePort() {
    return req.getRemotePort();
  }

  public String getRemoteAddr() {
    return req.getRemoteAddr();
  }

  public String getRemoteHost() {
    return req.getRemoteAddr();
  }

  public String getScheme() {
    return isSecure() ? "https" : "http";
  }

  public String getProtocol() {
    return "HTTP/1.0";  //TODO : 2.0?
  }

  public String getParameter(String name) {
    return req.getParameter(name);
  }

  public int getContentLength() {
    return req.getContentLength();
  }

  public String getContentType() {
    return req.getContentType();
  }

  public ServletInputStream getInputStream() {
    return new ServletInputStream(req.getInputStream());
  }

  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  public String getRequestURI() {
    return req.getURL();
  }

  public StringBuffer getRequestURL() {
    StringBuffer str = new StringBuffer();
    str.append(getScheme());
    str.append("://");
    str.append(getServerName());
    str.append(":");
    str.append(Integer.toString(getServerPort()));
    str.append(req.getURL());
    return str;
  }
}
