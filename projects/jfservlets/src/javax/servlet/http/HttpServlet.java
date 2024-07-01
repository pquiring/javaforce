package javax.servlet.http;

/** HttpServlet
 *
 * @author peter.quiring
 */

import javax.servlet.*;

public abstract class HttpServlet extends GenericServlet {
  private static final long serialVersionUID = 1L;
  public HttpServlet() {}
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doHead(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) {}
  protected void doTrace(HttpServletRequest req, HttpServletResponse resp) {}
  protected long getLastModified(HttpServletRequest req) {return 0;}
  protected void service(HttpServletRequest req, HttpServletResponse resp) {}
  public void service(ServletRequest req, ServletResponse res) {
    try {
      HttpServletRequest http_req = (HttpServletRequest)req;
      HttpServletResponse http_res = (HttpServletResponse)res;
      String method = http_req.getMethod();
      switch (method) {
        case "GET": doGet(http_req, http_res); break;
        case "POST": doPost(http_req, http_res); break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
