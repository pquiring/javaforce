package javaforce.linux.wl;

/** Method Notification.
 *
 * @author pquiring
 */

public interface WLNotify {
  /** Callback after a wayland event has fired. */
  public void onEvent(String cls, String method, Object[] args);
  /** Callback after a wayland request has fired. */
  public void onRequest(String cls, String method, Object[] args);
}
