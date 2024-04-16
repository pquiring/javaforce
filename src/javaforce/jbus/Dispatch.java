package javaforce.jbus;

/** Message Dispatch interface.
 *
 * @author pquiring
 */

public interface Dispatch {
  public void onMessage(String cmd);
}
