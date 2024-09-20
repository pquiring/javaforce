package javaforce.awt;

/** JTrayIcon
 *
 * TrayIcon that uses swing JPopupMenu instead of awt PopupMenu
 *
 * Fixes graalvm issues with TrayIcon PopupMenu distorted.
 * https://github.com/oracle/graal/issues/7037
 *
 * @author peter.quiring
 */

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import javaforce.*;

public class JTrayIcon extends TrayIcon {

  private static final boolean debug = false;

  private int count;

  public JTrayIcon(Image image, String tooltip, JPopupMenu menu) {
    super(image, tooltip);
    this.addMouseListener(new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
          count = 0;
          menu.show(null, e.getXOnScreen(), e.getYOnScreen());
        }
      }
    });
    menu.addMouseListener(new MouseAdapter() {
      public void mouseExited(MouseEvent e) {
        count--;
        if (debug) JFLog.log("menu exited:" + count);
        if (count == 0 && !inside(menu, e.getXOnScreen(), e.getYOnScreen())) {
          menu.setVisible(false);
        }
      }
      public void mouseEntered(MouseEvent e) {
        if (debug) JFLog.log("menu entered");
        count++;
      }
    });
    int cnt = menu.getComponentCount();
    for(int a=0;a<cnt;a++) {
      Component child = menu.getComponent(a);
      child.addMouseListener(new MouseAdapter() {
        public void mouseExited(MouseEvent e) {
          count--;
          if (debug) JFLog.log("item exited:" + count);
          if (count == 0 && !inside(menu, e.getXOnScreen(), e.getYOnScreen())) {
            menu.setVisible(false);
          }
        }
        public void mouseEntered(MouseEvent e) {
          if (debug) JFLog.log("item entered");
          count++;
        }
      });
    }
  }

  private boolean inside(JPopupMenu menu, int x, int y) {
    Point pt = menu.getLocationOnScreen();
    Rectangle r = menu.getBounds();
    int x1 = pt.x;
    int x2 = x1 + r.width;
    int y1 = pt.y;
    int y2 = y1 + r.height;
    if (x < x1 || x > x2) return false;
    if (y < y1 || y > y2) return false;
    return true;
  }

  public static void main(String[] args) {
    JFImage img = new JFImage();
    img.loadPNG(JF.class.getResourceAsStream("/javaforce/icons/javaforce.png"));
    JFImage img16 = new JFImage(16, 16);
    img16.putJFImageScale(img, 0, 0, 15, 15);

    JPopupMenu menu = new JPopupMenu();
    JMenuItem item1 = new JMenuItem("item1");
    item1.addActionListener((e) -> {
      JFLog.log("item1");
    });
    menu.add(item1);
    JMenuItem item2 = new JMenuItem("item2");
    item2.addActionListener((e) -> {
      JFLog.log("item2");
    });
    menu.add(item2);
    JMenuItem item3 = new JMenuItem("item3");
    item3.addActionListener((e) -> {
      JFLog.log("item3");
    });
    menu.add(item3);

    JTrayIcon icon = new JTrayIcon(img16.getImage(), "test", menu);
    try {
      SystemTray.getSystemTray().add(icon);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
