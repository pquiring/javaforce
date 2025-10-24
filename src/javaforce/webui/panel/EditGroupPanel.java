package javaforce.webui.panel;

/** Edit Group Panel
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.access.*;
import javaforce.webui.*;

public class EditGroupPanel extends PopupPanel {
  private TextField name;
  private TextField desc;

  private ListBox list;
  private ArrayList<String> removes;

  private Group group;
  private AccessControl access;

  public EditGroupPanel(String title, WebUIClient client) {
    super(title);
    setModal(true);

    this.access = client.getAccessControl();

    ToolBar tools;
    TabPanel tabs = new TabPanel();
    add(tabs);

    //general tab
    Panel general = new Panel();

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    general.add(grid);
    grid.addRow(new Component[] {new Label("Name"), name = new TextField("")});
    grid.addRow(new Component[] {new Label("Description"), desc = new TextField("")});
    name.setReadonly(true);

    tabs.addTab(general, "Account");

    //members tab
    Panel membership = new Panel();

    list = new ListBox();
    list.setWidth(256);
    list.setHeight(256);
    membership.add(list);

    tools = new ToolBar();
    membership.add(tools);
    Button add = new Button("Add");
    tools.add(add);
    Button remove = new Button("Remove");
    tools.add(remove);

    tabs.addTab(membership, "Members");

    tools = new ToolBar();
    add(tools);
    tools.add(new FlexBox());
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //buttons
    add.addClickListener((event, cmp) -> {
      //add user
      SelectFromListPanel select = client.getSelectFromListPanel();
      select.set(access.getUsers(), "Add User", () -> {
        String user = select.getSelectedText();
        if (list.contains(user)) return;
        list.add(user);
        if (removes.contains(user)) {
          //added user back
          removes.remove(user);
        }
      });
      select.setVisible(true);
    });
    remove.addClickListener((event, cmp) -> {
      int idx = list.getSelectedIndex();
      if (idx == -1) return;
      String user = list.getSelectedItem();
      list.remove(idx);
      removes.add(user);
    });
    accept.addClickListener((event, cmp) -> {
      synchronized (access.lock) {
        //update desc
        group.desc = desc.getText();
        //update members
        int cnt = list.getCount();
        for(int i=0;i<cnt;i++) {
          String user = list.getItem(i);
          if (!group.contains(user)) {
            group.addUser(user);
          }
        }
        for(String user : removes) {
          group.removeUser(user);
        }
        access.saveGroups();
      }
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void set(Group group) {
    this.group = group;

    name.setText(group.name);
    desc.setText(group.desc);

    list.removeAll();
    String[] users = group.getUsers();
    for(String name : users) {
      list.add(name);
    }
    removes = new ArrayList<>();
  }
}
