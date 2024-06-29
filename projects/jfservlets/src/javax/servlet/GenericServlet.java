package javax.servlet;

/** GenericServlet
 *
 */

public abstract class GenericServlet implements Servlet, ServletConfig {
  public void destroy() {}
  public void init() {}
  public void init(ServletConfig cfg) {}
  public abstract void service(ServletRequest req, ServletResponse res);
}
