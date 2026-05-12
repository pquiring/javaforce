package javax.servlet.http;

/** HttpServletRequest
 *
 * @author peter.quiring
 */

import java.util.*;

import javax.servlet.*;

public interface HttpServletRequest extends ServletRequest {
  public String getRequestURI();
  public StringBuffer getRequestURL();
  public String getMethod();
  public HttpSession getSession();
  public Part getPart(String name);
  public Collection<Part> getParts();
}
