package javax.servlet;

/** Servlet
 *
 * https://docs.oracle.com/javaee/7/api/index.html
 *
 * @author peter.quiring
 *
 */

public interface Servlet {
  public void destroy();
  public ServletConfig getServletConfig();
  public String getServletInfo();
  public void init(ServletConfig cfg);
  public void service(ServletRequest request, ServletResponse response);
}
