package javaforce.ansi.server;

/** Menu
 *
 * @author pquiring
 */

import java.awt.event.KeyEvent;

public class Menu implements Dialog {
  private boolean closed = false;
  private ANSI ansi;
  private String opts[];
  private int x,y;
  private int selected = 0;
  private String action = "";

  public Menu(ANSI ansi, String opts[], int x, int y) {
    this.ansi = ansi;
    this.opts = opts;
    this.x = x;
    this.y = y;
  }

  public void draw() {
    ansi.drawMenu(x,y,opts, selected);
  }

  public void keyPressed(int keyCode, int keyMods) {
    switch (keyMods) {
      case 0:
        switch (keyCode) {
          case KeyEvent.VK_ESCAPE:
            closed = true;
            break;
          case KeyEvent.VK_UP:
            selected--;
            if (selected == -1) {
              selected = opts.length - 1;
            }
            draw();
            break;
          case KeyEvent.VK_DOWN:
            selected++;
            if (selected == opts.length) {
              selected = 0;
            }
            draw();
            break;
          case KeyEvent.VK_LEFT:
            closed = true;
            action = "@Left";
            break;
          case KeyEvent.VK_RIGHT:
            closed = true;
            action = "@Right";
            break;
        }
        break;
    }
  }

  public void keyTyped(char key) {
    switch (key) {
      case 10:
        closed = true;
        action = opts[selected];
        break;
    }
  }

  public boolean isClosed() {
    return closed;
  }

  public void setClosed(boolean closed) {
    this.closed = closed;
  }

  public String getAction() {
    return action;
  }
}
