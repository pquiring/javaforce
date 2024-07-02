package javax.servlet.http;

/** HttpServletRequest
 *
 * @author peter.quiring
 */

import java.io.*;
import java.util.*;

import javax.servlet.*;


public class HttpServletRequestImpl implements HttpServletRequest {
  private HashMap<String, Object> req;
  private HashMap<String, String> params;
  private HttpSessionImpl session;

  @SuppressWarnings("unchecked")
  public HttpServletRequestImpl(HashMap<String, Object> req) {
    this.req = req;
    params = (HashMap<String, String>)req.get("params");
    session = new HttpSessionImpl((HashMap<String, Object>)req.get("session"));
  }

  public int getLocalPort() {
    return (Integer)req.get("LocalPort");
  }

  public String getLocalAddr() {
    return (String)req.get("LocalAddr");
  }

  public boolean isSecure() {
    return (Boolean)req.get("isSecure");
  }

  public int getServerPort() {
    return (Integer)req.get("LocalPort");
  }

  public String getServerName() {
    return (String)req.get("LocalAddr");
  }

  public int getRemotePort() {
    return (Integer)req.get("RemotePort");
  }

  public String getRemoteAddr() {
    return (String)req.get("RemoteAddr");
  }

  public String getRemoteHost() {
    return (String)req.get("RemoteAddr");
  }

  public String getScheme() {
    return isSecure() ? "https" : "http";
  }

  public String getProtocol() {
    return "HTTP/1.1";  //TODO : 2.0?
  }

  public String getParameter(String name) {
    return params.get(name);
  }

  public int getContentLength() {
    return (Integer)req.get("ContentLength");
  }

  public String getContentType() {
    return (String)req.get("ContentType");
  }

  public ServletInputStream getInputStream() {
    return new ServletInputStream((InputStream)req.get("InputStream"));
  }

  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream()));
  }

  public String getRequestURI() {
    return (String)req.get("URL");
  }

  public StringBuffer getRequestURL() {
    StringBuffer str = new StringBuffer();
    str.append(getScheme());
    str.append("://");
    str.append(getServerName());
    str.append(":");
    str.append(Integer.toString(getServerPort()));
    str.append(getRequestURI());
    return str;
  }

  public String getMethod() {
    return (String)req.get("Method");
  }

  public HttpSession getSession() {
    return session;
  }
}
