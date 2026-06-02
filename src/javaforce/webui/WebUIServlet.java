package javaforce.webui;

/** WebUI Servlet context
 *
 * @author pquiring
 */

public interface WebUIServlet extends WebUIHandler {
  /** Perform init for WebUIHandler. */
  public void init();
  /** Perform cleanup for WebUIHandler. */
  public void destroy();
  /** Returns the name of the Servlet. */
  public String getName();
}
