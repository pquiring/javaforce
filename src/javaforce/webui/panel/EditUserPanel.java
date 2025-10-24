package javaforce.webui.panel;

/** Edit User Panel
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.access.*;
import javaforce.webui.*;

public class EditUserPanel extends PopupPanel {
  private TextField name;
  private TextField full;
  private TextField desc;

  private ListBox list;
  private ArrayList<String> removes;

  private User user;
  private AccessControl access;

  public EditUserPanel(String title, WebUIClient client) {
    super(title);
    setModal(true);

    this.access = client.getAccessControl();

    ToolBar tools;
    TabPanel tabs = new TabPanel();
    add(tabs);

    //account tab
    Panel account = new Panel();

    GridLayout grid = new GridLayout(2, 0, new int[] {RIGHT, LEFT});
    account.add(grid);
    grid.addRow(new Component[] {new Label("Username"), name = new TextField("")});
    grid.addRow(new Component[] {new Label("Full Name"), full = new TextField("")});
    grid.addRow(new Component[] {new Label("Description"), desc = new TextField("")});
    name.setReadonly(true);

    tools = new ToolBar();
    account.add(tools);
    Button reset_password = new Button("Reset Password");
    tools.add(reset_password);

    tabs.addTab(account, "Account");

    //membership tab
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

    tabs.addTab(membership, "Membership");

    tools = new ToolBar();
    add(tools);
    tools.add(new FlexBox());
    Button accept = new Button("Accept");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    //buttons
    reset_password.addClickListener((event, cmp) -> {
      ResetPasswordPanel reset = client.getResetPasswordPanel();
      reset.set(user);
      reset.setVisible(true);
    });

    add.addClickListener((event, cmp) -> {
      //add group
      SelectFromListPanel select = client.getSelectFromListPanel();
      select.set(access.getGroups(), "Add Group", () -> {
        String group = select.getSelectedText();
        if (list.contains(group)) return;
        list.add(group);
        if (removes.contains(group)) {
          //added user back
          removes.remove(group);
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
        //update basic fields
        user.full = full.getText();
        user.desc = desc.getText();
        //update membership
        int cnt = list.getCount();
        Groups groups = access.getGroupsList();
        for(int i=0;i<cnt;i++) {
          String groupName = list.getItem(i);
          Group group = groups.getGroup(groupName);
          if (!group.contains(user.name)) {
            group.addUser(user.name);
          }
        }
        for(String groupName : removes) {
          Group group = groups.getGroup(groupName);
          if (group.contains(user.name)) {
            group.removeUser(user.name);
          }
        }
        access.saveUsers();
      }
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public void set(User user) {
    this.user = user;
    name.setText(user.name);
    full.setText(user.full);
    desc.setText(user.desc);

    list.removeAll();
    Group[] gps = access.getGroups();
    for(Group group : gps) {
      if (group.contains(user.name)) {
        list.add(group.name);
      }
    }
    removes = new ArrayList<>();
  }
}
