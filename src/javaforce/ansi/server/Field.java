package javaforce.ansi.server;

/**
 *
 * @author pquiring
 */

public class Field implements KeyEvents {
  public int x;
  public int y;
  public int width = 1;
  public int height = 1;
  public int cx;
  public int cy;
  public int wx;
  public int wy;
  public int dx = 1;
  public int dy = 0;

  public void gotoHomePos() {
    ANSI.gotoPos(x + dx, y + dy);
  }

  public void gotoCurrentPos() {
    int px = cx + dx - wx;
    if (px >= width) {
      px = width - 1;
    }
    int py = cy + dy - wy;
    if (py >= height) {
      py = height - 1;
    }
    ANSI.gotoPos(x + px, y + py);
  }

  public void keyPressed(int keyCode, int keyMods) {
  }

  public void keyTyped(char key) {
  }

  public void draw() {
    gotoCurrentPos();
  }
}
