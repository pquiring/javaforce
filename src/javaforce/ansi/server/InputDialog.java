package javaforce.ansi.server;

/** InputDialog
 *
 * @author Peter Quiring
 */

import java.awt.event.KeyEvent;

public class InputDialog implements Dialog {
  private ANSI ansi;
  private boolean closed = false;
  private boolean cancel = false;
  private Field[] fields;
  private int field = 0;
  private String action;
  private String title, msg[], initValues[], opts;

  public final static String ENTER_ESC = "Press <Enter> to accept or <ESC> to cancel";

  /** InputDialog
   * ansi = ANSI object
   * title = window title (todo)
   * msg[] = messages for each input field
   * initValue[] = init values for each input field (optional) (if not null length must equal msg length)
   * opts = buttons (ie: see ENTER_ESC)
   */
  public InputDialog(ANSI ansi, String title, String[] msg, String[] initValue, String opts) {
    this.ansi = ansi;
    this.msg = msg;
    this.title = title;
    this.initValues = initValue;
    this.opts = opts;
  }
  public void draw() {
    int w = 48;
    int h = msg.length * 2 + 1;
    for(int a=0;a<msg.length;a++) {
      if (msg[a].startsWith("{checkbox}") || msg[a].startsWith("<")) {
        h--;
      }
    }
    int x = (ansi.width - w)/2;
    int y = (ansi.height - h)/2;
    String[] lines = new String[msg.length * 2 + 1];
    int pos = 0;
    for(int a=0;a<msg.length;a++) {
      if (msg[a].startsWith("{checkbox}")) {
        lines[pos++] = msg[a] + ANSI.repeat(48-2-msg[a].length()+9, ' ');
      } else if (msg[a].startsWith("<")) {
        lines[pos++] = msg[a] + ANSI.repeat(48-2-msg[a].length(), ' ');
      } else {
        lines[pos++] = msg[a];
        lines[pos++] = "[" + ANSI.repeat(48-2, ' ') + "]";
      }
    }
    lines[pos] = opts;
    if (fields != null) {
      //preserve current values
      initValues = new String[msg.length];
      for(int a=0;a<msg.length;a++) {
        if (fields[a] instanceof TextField) {
          initValues[a] = ((TextField)fields[a]).getText();
        }
        if (fields[a] instanceof CheckBox) {
          initValues[a] = Boolean.toString(((CheckBox)fields[a]).isChecked());
        }
      }
    }
    fields = ansi.drawWindow(x, y, w+2, h+2, lines);
    if (initValues != null) {
      for(int a=0;a<initValues.length;a++) {
        if (initValues[a] == null) continue;
        if (fields[a] instanceof TextField) {
          TextField text = (TextField)fields[a];
          text.setText(initValues[a]);
          text.draw();
        }
        if (fields[a] instanceof CheckBox) {
          CheckBox cb = (CheckBox)fields[a];
          cb.setChecked(initValues[a].equals("true"));
          cb.draw();
        }
      }
      initValues = null;
    }
  }
  public void keyPressed(int keyCode, int keyMods) {
    switch (keyMods) {
      case 0:
        switch (keyCode) {
          case KeyEvent.VK_ESCAPE:
            closed = true;
            cancel = true;
            break;
          default:
            fields[field].keyPressed(keyCode, keyMods);
            break;
        }
        break;
    }
  }
  public void keyTyped(char key) {
    switch (key) {
      case 9:
        //tab
        field++;
        if (field == fields.length) field = 0;
        fields[field].gotoCurrentPos();
        break;
      case 10:
        //enter
        closed = true;
        if (fields[field] instanceof Button) {
          action = ((Button)fields[field]).action;
          if (action.equals("ESC")) {
            cancel = true;
          }
        }
        break;
      case ' ':
        if (fields[field] instanceof CheckBox) {
          CheckBox cb = (CheckBox)fields[field];
          cb.toggle();
          cb.draw();
        }
        break;
      default:
        fields[field].keyTyped(key);
        break;
    }
  }
  public boolean isClosed() {
    return closed;
  }
  public void setClosed(boolean closed) {
    this.closed = closed;
  }
  public boolean isCancelled() {
    return cancel;
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
  public void setAction(String action) {
    this.action = action;
  }
  public String getText(int idx) {
    if (cancel) return null;
    return ((TextField)fields[idx]).getText();
  }
  public boolean isChecked(int idx) {
    if (cancel) return false;
    return ((CheckBox)fields[idx]).isChecked();
  }
  public void setOptions(String ops) {
    this.opts = opts;
    //TODO
  }
}
