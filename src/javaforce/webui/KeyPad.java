package javaforce.webui;

/** KeyPad for TextComponent
 *
 * @author pquiring
 */

import javaforce.webui.event.*;

public class KeyPad extends PopupPanel implements Click {
  private TextComponent field;
  private Table table;

  private static String cols[][] = {
    {"<", "/", "*", null},
    {"7", "8", "9", "-"},
    {"4", "5", "6", "+"},
    {"3", "2", "1", "Ent"},
    {"0",null, ".",null},
  };

  public KeyPad(String title, int px) {
    super(title);
    table = new Table(px, px, 4, 5);
    setTitleBarSize(px);
    add(table);
    for(int col=0;col<cols.length;col++) {
      String rowchs[] = cols[col];
      for(int row=0;row<rowchs.length;row++) {
        String key = rowchs[row];
        if (key == null) continue;
        Button b = new Button(rowchs[row]);
        b.addClickListener(this);
        if (key.equals("0")) {
          b.setSize(px*2, px);
          table.add(b, row, col, 2, 1);
        } else if (key.equals("Ent")) {
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
    if (txt.equals("Ent")) {
      setVisible(false);
      return;
    }
    if (txt.equals("<")) {
      String oldtxt = field.getText();
      if (oldtxt.length() == 0) return;
      field.setText(oldtxt.substring(0, oldtxt.length() - 1));
      return;
    }
    String newtxt = field.getText() + txt;
    field.setText(newtxt);
  }
}
