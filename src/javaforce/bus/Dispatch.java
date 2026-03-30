package javaforce.bus;

/** Message Dispatch interface.
 *
 * @author pquiring
 */

public interface Dispatch {
  public Object onMessage(String method, Object[] args);
}
