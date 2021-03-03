package javaforce;

/** System Tray icon
 *
 * Fixes Java Bug JDK-8255439 where icons become blurry after screen scaling or resolution changes.
 *
 * @author pquiring
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class JFIcon extends TimerTask {
  private Timer timer;
  private double scaling;
  private int width,height;
  private SystemTray tray;
  private TrayIcon icon;
  private PopupMenu menu;
  private JFImage image;
  private JFImage scaled;
  private ActionListener listener;
  private String tooltip;

  public boolean create(JFImage image, PopupMenu menu, ActionListener listener, String tooltip) {
    timer = new Timer();
    timer.schedule(this, 1000, 1000);
    scaling = JFAWT.getScaling();
    width = JFAWT.getWidth();
    height = JFAWT.getHeight();
    this.image = image;
    this.menu = menu;
    this.listener = listener;
    this.tooltip = tooltip;
    create();
    return true;
  }

  public void destroy() {
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    remove();
  }

  private void remove() {
    if (tray != null) {
      if (icon != null) {
        tray.remove(icon);
        icon.setPopupMenu(null);
        icon = null;
      }
      tray = null;
    }
  }

  private void create() {
    tray = SystemTray.getSystemTray();
    Dimension size = tray.getTrayIconSize();
//    JFLog.log("TrayIconSize=" + size.width + "x" + size.height);
    scaled = new JFImage(size.width, size.height);
    scaled.fill(0, 0, size.width, size.height, 0x00000000, true);  //fill with alpha transparent
    if (false) {
      //scaled image (looks bad sometimes)
      scaled.getGraphics().drawImage(image.getImage()
        , 0, 0, size.width, size.height
        , 0, 0, image.getWidth(), image.getHeight()
        , null);
    } else {
      //center image
      scaled.getGraphics().drawImage(image.getImage()
        , (size.width - image.getWidth()) / 2
        , (size.height - image.getHeight()) / 2
        , null);
    }
    //create tray icon
    icon = new TrayIcon(scaled.getImage(), tooltip, menu);
    icon.setImageAutoSize(true);
    icon.addActionListener(listener);
    try { tray.add(icon); } catch (Exception e) { JFLog.log(e); }
  }

  public void run() {
    double scaling_now = JFAWT.getScaling();
    int width_now = JFAWT.getWidth();
    int height_now = JFAWT.getHeight();
    if ((scaling_now == scaling) && (width_now == width) && (height_now == height)) return;
    scaling = scaling_now;
    width = width_now;
    height = height_now;
    java.awt.EventQueue.invokeLater(new Runnable() {
      public void run() {
        if (true) {
          icon.setImage(scaled.getImage());
        } else {
          destroy();
          create();
        }
      }
    });
  }
}
