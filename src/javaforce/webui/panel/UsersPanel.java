package javaforce.webui.panel;

/** Users Panel
 *
 * @author pquiring
 */

import javaforce.access.*;
import javaforce.webui.*;

public class UsersPanel extends Panel {
  private Table table;
  private Users users;
  private User[] list;

  private static final int col_height = 25;

  public UsersPanel(WebUIClient client) {
    users = client.getAccessControl().getUsersList();
    ToolBar toolbar = new ToolBar();
    this.add(toolbar);

    Button refresh = new Button("Refresh");
    toolbar.add(refresh);
    Button create = new Button("Create");
    toolbar.add(create);
    Button edit = new Button("Edit");
    toolbar.add(edit);
    Button delete = new Button("Delete");
    toolbar.add(delete);

    table = new Table(new int[] {150, 250}, col_height, 4, 0);
    add(table);
    table.setSelectionMode(Table.SELECT_ROW);
    table.setBorder(true);

    Runnable init = new Runnable() {
      public void run() {
        table.removeAll();
        list = users.getUsers();
        for(User user : list) {
          table.addRow(new Component[] {new Label(user.name), new Label(user.desc)});
        }
      }
    };

    init.run();

    refresh.addClickListener((event, cmp) -> {
      init.run();
    });
    create.addClickListener((event, cmp) -> {
      client.new_user_panel.clear();
      client.new_user_panel.setVisible(true);
    });
    edit.addClickListener((event, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      client.edit_user_panel.set(list[idx]);
      client.edit_user_panel.setVisible(true);
    });
    delete.addClickListener((event, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      client.confirm_panel.set(
        "Delete user " + list[idx].name + "?"
        , "Delete"
        , new Runnable() {
          public void run() {
            User user = list[idx];
            users.removeUser(user.name);
            init.run();
          }
      });
      client.confirm_panel.setVisible(true);
    });
  }
}
