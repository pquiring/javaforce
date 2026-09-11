package javaforce.linux.wl;

/** Method Notification.
 *
 * @author pquiring
 */

public interface WLNotify {
  /** Callback after a wayland event has fired. */
  public void onEvent(String cls, String method, Object[] args);
}
