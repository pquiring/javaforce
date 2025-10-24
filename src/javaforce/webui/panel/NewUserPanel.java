package javaforce.webui.panel;

/** New User Panel
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.access.*;
import javaforce.webui.*;

public class NewUserPanel extends PopupPanel {
  private TextField name;
  private TextField full;
  private TextField desc;
  private TextField pass1;
  private TextField pass2;

  private AccessControl access;

  public NewUserPanel(String title, WebUIClient client) {
    super(title);
    setModal(true);

    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    add(grid);

    grid.addRow(new Component[] {new Label("Username"), name = new TextField("")});
    grid.addRow(new Component[] {new Label("Full Name"), full = new TextField("")});
    grid.addRow(new Component[] {new Label("Description"), desc = new TextField("")});
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
    Button accept = new Button("Create");
    row.add(accept);
    Button cancel = new Button("Cancel");
    row.add(cancel);

    accept.addClickListener((event, cmp) -> {
      String new_name = JF.filter(name.getText(), JF.filter_id);
      if (new_name.length() == 0) {
        msg.setText("Username too short");
        return;
      }
      String new_full = full.getText();
      String new_desc = desc.getText();
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
      User user = new User(new_name, p1, new_desc);
      user.full = new_full;
      client.getAccessControl().addUser(user);
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void clear() {
    name.setText("");
    full.setText("");
    desc.setText("");
    pass1.setText("");
    pass2.setText("");
  }
}
