package javaforce.webui;

/** Message Popup
 *
 *  Shows a popup message with OK / Cancel buttons.
 *
 * @author pquiring
 */

public class MessagePopup extends PopupPanel {
  public MessagePopup(String title, String msg, boolean showCancel) {
    super(title);
    lbl = new Label(msg);
    add(lbl);
    Row row = new Row();
    add(row);
    ok = new Button("OK");
    row.add(ok);
    if (showCancel) {
      cancel = new Button("Cancel");
      row.add(cancel);
    }
    ok.addClickListener((e, c) -> {
      setVisible(false);
      action();
    });
    if (showCancel) {
      cancel.addClickListener((e, c) -> {
        setVisible(false);
      });
    }
  }
  private Label lbl;
  private Button ok, cancel;
}
