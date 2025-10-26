package javaforce.webui.panel;

/** Confirm Action Panel.
 *
 * Presents a message with OK or CANCEL buttons.
 *
 * @author pquiring
 */

import javaforce.webui.*;

public class ConfirmPanel extends PopupPanel {

  public Runnable action;

  public Label msg;
  public Button button_action;
  public Button button_cancel;

  public ConfirmPanel(String title, WebUIClient client) {
    super("Confirm Action");
    setModal(true);
    Row row;

    row = new Row();
    add(row);
    msg = new Label("");
    row.add(msg);

    row = new Row();
    add(row);
    Label popup_label = new Label("Are you sure?");
    row.add(popup_label);

    button_action = new Button("Action");
    button_cancel = new Button("Cancel");
    row = new Row();
    add(row);
    row.add(button_action);
    row.add(button_cancel);

    button_action.addClickListener((event, cmp) -> {
      action.run();
      setVisible(false);
    });
    button_cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void set(String msg, String button_text, Runnable action) {
    this.msg.setText(msg);
    button_action.setText(button_text);
    this.action = action;
  }
}
