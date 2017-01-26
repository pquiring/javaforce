package javaforce.webui;

/** GetTextPopup
 *
 *  Shows a popup dialog to input text with OK / Cancel buttons.
 *
 * @author pquiring
 */

public class GetTextPopup extends PopupPanel {
  public GetTextPopup(String title, String msg, String initText) {
    super(title);
    lbl = new Label(msg);
    add(lbl);
    text = new TextField(initText);
    add(text);
    Row row = new Row();
    add(row);
    ok = new Button("OK");
    row.add(ok);
    cancel = new Button("Cancel");
    row.add(cancel);
    ok.addClickListener((e, c) -> {
      if (!validate()) return;
      setVisible(false);
      action();
    });
    cancel.addClickListener((e, c) -> {
      setVisible(false);
    });
  }
  public String getText() {
    return text.getText();
  }
  private Label lbl;
  private TextField text;
  private Button ok, cancel;
}
