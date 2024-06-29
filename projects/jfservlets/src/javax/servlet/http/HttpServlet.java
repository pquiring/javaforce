package javax.servlet.http;

/** HttpServlet
 *
 * @author peter.quiring
 */

import javax.servlet.*;

public class HttpServlet {
  public String name;
  public String url;
  public void doDelete(HttpServletRequest req, HttpServletResponse resp) {}
  public void doGet(HttpServletRequest req, HttpServletResponse resp) {}
  public void doHead(HttpServletRequest req, HttpServletResponse resp) {}
  public void doOptions(HttpServletRequest req, HttpServletResponse resp) {}
  public void doPost(HttpServletRequest req, HttpServletResponse resp) {}
  public void doPut(HttpServletRequest req, HttpServletResponse resp) {}
  public void doTrace(HttpServletRequest req, HttpServletResponse resp) {}
  public long getLastModified(HttpServletRequest req) {return 0;}
  public void service(HttpServletRequest req, HttpServletResponse resp) {}
  public void service(ServletRequest req, ServletResponse res) {}
}
