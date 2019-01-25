package javaforce.ansi;

/** TextField
 *
 * @author pquiring
 */

import java.awt.event.KeyEvent;

public class TextField extends Field {
  public StringBuilder text = new StringBuilder();

  public void keyPressed(int keyCode, int keyMods) {
    if (keyMods != 0) {
      return;
    }
    switch (keyCode) {
      case KeyEvent.VK_UP:
        break;
      case KeyEvent.VK_DOWN:
        break;
      case KeyEvent.VK_LEFT:
        if (cx > 0) cx--;
        gotoCurrentPos();
        break;
      case KeyEvent.VK_RIGHT:
        if (cx < text.length()) cx++;
        gotoCurrentPos();
        break;
      case KeyEvent.VK_DELETE:
        if (cx >= text.length()) return;
        text.deleteCharAt(cx);
        draw();
        break;
    }
  }

  public void keyTyped(char key) {
    switch (key) {
      case 8:
      case 127:
        if (text.length() == 0) return;
        if (cx == 0) return;
        cx--;
        text.deleteCharAt(cx);
        break;
      default:
        if (key < 32) return;
        text.append(key);
        cx++;
        break;
    }
    draw();
  }

  public void draw() {
    gotoHomePos();
    ANSI.setFieldColor();
    int x1 = cx - width;
    if (x1 < 0) x1 = 0;
    System.out.print(ANSI.pad(text.substring(x1), width));
    gotoCurrentPos();
  }

  public void setText(String in) {
    text.setLength(0);
    text.append(in);
    cx = 0;
  }

  public String getText() {
    return text.toString();
  }
}
