package javaforce.webui.panel;

/** Message Panel.
 *
 * Presents a message with OK.
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class MessagePanel extends PopupPanel {

  public Label msg;
  public Button button_okay;

  public MessagePanel(String title, String msg) {
    super(title);
    init(msg, null);
  }

  public MessagePanel(String title, String msg, Component cmp) {
    super(title);
    init(msg, cmp);
  }

  private void init(String text, Component cmpt) {
    setModal(true);
    Row row;

    row = new Row();
    add(row);
    msg = new Label(text);
    row.add(msg);

    if (cmpt != null) {
      row = new Row();
      add(row);
      row.add(cmpt);
    }

    button_okay = new Button("Okay");
    row = new Row();
    add(row);
    row.add(button_okay);

    button_okay.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void set(String msg) {
    this.msg.setText(msg);
  }

  public void set(String msg, String button_text) {
    this.msg.setText(msg);
    button_okay.setText(button_text);
  }
}
