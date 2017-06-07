package javaforce.webui;

/** TextField Popup
 *
 * Shows a popup message with a TextField and OK / Cancel buttons.
 *
 * Use addActionListener() to respond to OK button.
 *
 * @author pquiring
 */

public class TextFieldPopup extends PopupPanel {
  public TextFieldPopup(String title, String msg, String text, boolean showCancel) {
    super(title);
    Row row;
    lbl = new Label(msg);
    row = new Row();
    row.add(lbl);
    add(row);
    ta = new TextField(text);
    row = new Row();
    row.add(ta);
    add(row);
    ok = new Button("OK");
    row = new Row();
    add(row);
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
  private TextField ta;
  private Button ok, cancel;
  public void setMessage(String msg) {
    lbl.setText(msg);
  }
  public void setText(String text) {
    ta.setText(text);
  }
  public void setTextFieldSize(int width, int height) {
    ta.setSize(width, height);
  }
  public void setTextFieldWidth(int width) {
    ta.setWidth(width);
  }
  public void setTextFieldHeight(int height) {
    ta.setHeight(height);
  }
}
