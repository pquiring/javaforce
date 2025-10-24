package javaforce.webui.panel;

/** Select From List Popup Panel
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

public class SelectFromListPanel extends PopupPanel {

  private ListBox list;
  private Runnable action;

  public SelectFromListPanel(String title) {
    super(title);
    setModal(true);

    Row row;
    row = new Row();
    add(row);
    row.add(new Label("Make a selection"));

    row = new Row();
    add(row);
    list = new ListBox();
    list.setWidth(256);
    list.setHeight(256);
    row.add(list);

    ToolBar tools = new ToolBar();
    add(tools);
    tools.add(new FlexBox());
    Button accept = new Button("Select");
    tools.add(accept);
    Button cancel = new Button("Cancel");
    tools.add(cancel);

    accept.addClickListener((event, cmp) -> {
      action.run();
      setVisible(false);
    });
    cancel.addClickListener((event, cmp) -> {
      setVisible(false);
    });
  }

  public int getSelectedIndex() {
    return list.getSelectedIndex();
  }

  public String getSelectedText() {
    return list.getSelectedItem();
  }

  /** Setup this panel. */
  public void set(Name[] options, String title, Runnable action) {
    list.removeAll();
    for(Name opt : options) {
      list.add(opt.getName());
    }
    setTitle(title);
    this.action = action;
  }
}
