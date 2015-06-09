package javaforce.linux;

/**
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
}
