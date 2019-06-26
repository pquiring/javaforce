package javaforce.ansi.server;

/** MessageDialog
 *
 * @author Peter Quiring
 */

import java.awt.event.KeyEvent;

public class MessageDialog implements Dialog {
  private String lines[];
  private ANSI ansi;
  private boolean closed = false;
  private int maxlen;
  private Field fields[];
  private int field = 0;
  private String action;

  public MessageDialog(ANSI ansi, String lines[]) {
    this.ansi = ansi;
    this.lines = lines;
    for(int a=0;a<lines.length;a++) {
      int len = lines[a].length();
      if (len > maxlen) {
        maxlen = len;
      }
    }
    maxlen += 10;
  }
  public void draw() {
    int x = (ansi.width - maxlen)/2;
    int y = (ansi.height - lines.length)/2;
    fields = ansi.drawWindow(x, y, maxlen, lines.length+2, lines);
  }
  public void keyPressed(int keyCode, int keyMods) {
    if (keyMods != 0) return;
    switch (keyCode) {
      case KeyEvent.VK_ESCAPE:
        closed = true;
        break;
    }
  }
  public void keyTyped(char key) {
    switch (key) {
      case 9:
        //tab to next button/field
        nextField();
        break;
      case 10:
        //enter
        if (fields[field] instanceof Button) {
          action = ((Button)fields[field]).action;
        }
        closed = true;
        break;
    }
  }
  public boolean isClosed() {
    return closed;
  }
  public void setClosed(boolean closed) {
    this.closed = closed;
  }
  public void nextField() {
    if (fields.length < 2) return;
    field++;
    if (field == fields.length) field = 0;
    fields[field].gotoCurrentPos();
  }
  public String getAction() {
    return action;
  }
}
