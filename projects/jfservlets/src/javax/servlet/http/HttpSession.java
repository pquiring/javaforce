package javax.servlet.http;

/** HttpSession
 *
 * @author peter.quiring
 */

public interface HttpSession {
  public Object getAttribute(String name);
  public void setAttribute(String name, Object value);
  public void removeAttribute(String name);
}
