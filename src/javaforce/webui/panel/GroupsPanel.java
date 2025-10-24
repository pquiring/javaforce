package javaforce.webui.panel;

/** Groups Panel
 *
 * @author pquiring
 */

import javaforce.access.*;
import javaforce.webui.*;

public class GroupsPanel extends Panel {
  private Table table;
  private Groups groups;
  private Group[] list;

  private static final int col_height = 25;

  public GroupsPanel(WebUIClient client) {
    groups = client.getAccessControl().getGroupsList();
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
        list = groups.getGroups();
        for(Group group : list) {
          table.addRow(new Component[] {new Label(group.name), new Label(group.desc)});
        }
      }
    };

    init.run();

    refresh.addClickListener((event, cmp) -> {
      init.run();
    });
    create.addClickListener((event, cmp) -> {
      client.new_group_panel.clear();
      client.new_group_panel.setVisible(true);
    });
    edit.addClickListener((event, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      client.edit_group_panel.set(list[idx]);
      client.edit_group_panel.setVisible(true);
    });
    delete.addClickListener((event, cmp) -> {
      int idx = table.getSelectedRow();
      if (idx == -1) return;
      client.confirm_panel.set(
        "Delete group " + list[idx].name + "?"
        , "Delete"
        , new Runnable() {
          public void run() {
            Group group = list[idx];
            groups.removeGroup(group.name);
            init.run();
          }
      });
      client.confirm_panel.setVisible(true);
    });
  }
}
