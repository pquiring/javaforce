package javax.servlet.http;

/** HttpSession
 *
 * @author peter.quiring
 */

import java.util.*;

public class HttpSession {
  private HashMap<String, Object> props = new HashMap<>();

  public Object getAttribute(String name) {
    return props.get(name);
  }

  public void setAttribute(String name, Object value) {
    props.put(name, value);
  }
}
