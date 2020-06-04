package javaforce.ansi.server;

/** List
 *
 * @author pquiring
 */

import java.awt.event.KeyEvent;

public class List extends Field {
  public String[] items;
  public void keyPressed(int keyCode, int keyMods) {
    if (keyMods != 0) {
      return;
    }
    int y1 = wy;
    int y2 = y1 + height - 3;
    int cnt = items.length;
    switch (keyCode) {
      case KeyEvent.VK_UP:
        if (cy > 0) cy--;
        if (cy < y1 && wy > 0) wy--;
        break;
      case KeyEvent.VK_DOWN:
        if (cy < cnt-1) cy++;
        if (cy > y2) wy++;
        break;
      case KeyEvent.VK_LEFT:
        break;
      case KeyEvent.VK_RIGHT:
        break;
      case KeyEvent.VK_DELETE:
        break;
    }
    draw();
  }
  public void draw() {
    ANSI.drawList(x, y, width, height, items, wy, cy);
    gotoCurrentPos();
  }
  public String getItem() {
    return items[cy];
  }
}
