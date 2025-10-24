package javaforce.webui.panel;

/** Reset Password Panel
 *
 * @author pquiring
 */

import javaforce.access.*;
import javaforce.webui.*;

public class ResetPasswordPanel extends PopupPanel {
  private TextField pass1;
  private TextField pass2;

  private User user;

  public ResetPasswordPanel(String title, WebUIClient client) {
    super(title);
    setModal(true);

    Row row;

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    add(grid);

    grid.addRow(new Component[] {new Label("Password"), pass1 = new TextField("")});
    grid.addRow(new Component[] {new Label("Confirm Password"), pass2 = new TextField("")});
    pass1.setPassword(true);
    pass2.setPassword(true);

    row = new Row();
    add(row);
    Label msg = new Label("");
    msg.setColor(Color.red);
    row.add(msg);

    row = new Row();
    add(row);
    Button accept = new Button("Reset");
    row.add(accept);
    Button cancel = new Button("Cancel");
    row.add(cancel);

    accept.addClickListener((event, cmp) -> {
      String p1 = pass1.getText();
      String p2 = pass2.getText();
      if (p1.length() < 4) {
        msg.setText("Password too short");
        return;
      }
      if (!p1.equals(p2)) {
        msg.setText("Passwords do not match");
        return;
      }
      client.getAccessControl().setUserPassword(user.name, p1);
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void set(User user) {
    this.user = user;
    pass1.setText("");
    pass2.setText("");
  }
}
