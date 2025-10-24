package javaforce.webui.panel;

/** New Group Panel
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.access.*;
import javaforce.webui.*;

public class NewGroupPanel extends PopupPanel {
  private TextField name;
  private TextField desc;

  public NewGroupPanel(String title, WebUIClient client) {
    super(title);
    setModal(true);

    Row row;
    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    add(grid);

    grid.addRow(new Component[] {new Label("Name"), name = new TextField("")});
    grid.addRow(new Component[] {new Label("Description"), desc = new TextField("")});

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
      String new_desc = desc.getText();
      client.getAccessControl().addGroup(new_name, new_desc);
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void clear() {
    name.setText("");
    desc.setText("");
  }
}
