package javaforce.webui;

/** KeyPad for TextComponent
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class KeyPad extends PopupPanel implements Click {
  private TextComponent field;
  private Table table;

  private static char[][] cols = {
    {'7', '8', '9', '-'},
    {'4', '5', '6', '+'},
    {'3', '2', '1', 'E'},
    {'0',   0, '.',   0},
  };

  public KeyPad(String title, int px) {
    super(title);
    table = new Table(px, px, 4, 4);
    add(table);
    for(int col=0;col<cols.length;col++) {
      char rowchs[] = cols[col];
      for(int row=0;row<rowchs.length;row++) {
        char ch = rowchs[row];
        if (ch == 0) continue;
        Button b = new Button(Character.toString(rowchs[row]));
        b.addClickListener(this);
        if (ch == '0') {
          b.setSize(px*2, px);
          table.add(b, row, col, 2, 1);
        } else if (ch == 'E') {
          b.setSize(px, px*2);
          table.add(b, row, col, 1, 2);
        } else {
          b.setSize(px, px);
          table.add(b, row, col);
        }
      }
    }
  }

  public void show(TextComponent field) {
    this.field = field;
    field.requestPos();
    field.addMovedListener((comp, x, y) -> {
      setPosition(field.x, field.y + field.height);
      setVisible(true);
    });
  }

  public void onClick(MouseEvent e, Component c) {
    Button b = (Button)c;
    String txt = b.getText();
    if (txt.equals("E")) {
      setVisible(false);
    }
    String newtxt = field.getText() + txt;
    field.setText(newtxt);
  }
}
