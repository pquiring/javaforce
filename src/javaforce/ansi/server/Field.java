package javaforce.ansi.server;

/**
 *
 * @author pquiring
 */

public class Field implements KeyEvents {
  //position
  public int x;
  public int y;
  //size
  public int width = 1;
  public int height = 1;
  //cursr pos
  public int cx;
  public int cy;
  //window offset
  public int wx;
  public int wy;
  //delta offset
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

  public void setPos(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public void setSize(int w, int h) {
    width = w;
    height = h;
  }

  public void setWidth(int w) {
    width = w;
  }

  public void setHeight(int h) {
    height = h;
  }

  public void setCursorPos(int x, int y) {
    cx = x;
    cy = y;
  }

  public void setWindowOffset(int x, int y) {
    wx = x;
    wy = y;
  }

  public void setDeltaOffset(int x, int y) {
    dx = x;
    dy = y;
  }
}
