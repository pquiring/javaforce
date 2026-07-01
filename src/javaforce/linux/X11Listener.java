package javaforce.linux;

/**
 * X11Listener.
 *
 * Created : Aug 8, 2012
 *
 * @author pquiring

*/
public interface X11Listener {
  /** Triggered when new tray icons are added. count = new # of icons in the tray */
  public void trayIconAdded(int count);
  /** Triggered when tray icons are removed. count = new # of icons in the tray */
  public void trayIconRemoved(int count);
  /** Triggered when a new top-level window has changed */
  public void windowsChanged();
  /** Provide new window added event. */
  public void windowAdded(long xid, int pid, String title, String name, String res_name, String res_class);
  /** Provide window deleted event. */
  public void windowDeleted(long xid);
}
