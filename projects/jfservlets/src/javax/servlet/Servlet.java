package javax.servlet;

/** Servlet
 *
 * https://docs.oracle.com/javaee/7/api/index.html
 *
 * @author peter.quiring
 *
 */

public interface Servlet {
  void destroy();
  ServletConfig getServletConfig();
  String getServletInfo();
  void init(ServletConfig cfg);
  void service(ServletRequest request, ServletResponse response);
}
