package javax.servlet.http;

/** HttpSession
 *
 * @author peter.quiring
 */

import java.util.*;

public class HttpSessionImpl implements HttpSession {
  private HashMap<String, Object> props;

  public HttpSessionImpl(HashMap<String, Object> map) {
    props = map;
  }

  public Object getAttribute(String name) {
    return props.get(name);
  }

  public void setAttribute(String name, Object value) {
    props.put(name, value);
  }
}
