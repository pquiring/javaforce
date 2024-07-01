package javax.servlet.http;

/** HttpServletRequest
 *
 * @author peter.quiring
 */

import javax.servlet.*;

public interface HttpServletRequest extends ServletRequest {
  public String getRequestURI();
  public StringBuffer getRequestURL();
  public String getMethod();
}
