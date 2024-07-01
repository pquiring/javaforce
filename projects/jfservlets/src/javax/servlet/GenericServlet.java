package javax.servlet;

/** GenericServlet
 *
 */

import java.io.*;

public abstract class GenericServlet implements Servlet, ServletConfig, Serializable {
  public void destroy() {}
  public void init() {}
  public void init(ServletConfig cfg) {}
  public abstract void service(ServletRequest req, ServletResponse res);
}
