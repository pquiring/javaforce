package jfcontrols.webui;

/** Panels
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;
import jfcontrols.logic.Service;

public class Panels extends Panel {
  public Panels() {
    panel = new ComboBox();
    panel.addChangedListener((c) -> {
    });
    loadPanels();
    add(panel);
    Row row = new Row();
    add(row);
    view = new Button("View");
    view.setFontSize(56);
    view.addClickListener((me, c) -> {
      String id = panel.getSelectedValue();
      if (id == null) return;
      c.client.setPanel(new ViewPanel(id));
    });
    row.add(view);
    delete = new Button("Delete");
    delete.setFontSize(56);
    delete.addClickListener((me, c) -> {
      //TODO
    });
    row.add(delete);
    row = new Row();
    add(row);
    create = new Button("Create");
    create.setFontSize(56);
    create.addClickListener((me, c) -> {
      String str = name.getText();
      if (str.length() == 0) return;
      if (str.length() > 16) str = str.substring(0,14);
      str = SQL.quote(str);
      String q = "insert into panels (name) values (" + str + ")";
      SQL sql = new SQL();
      sql.connect(Service.derbyURI);
      sql.execute(q);
      sql.close();
      loadPanels();
    });
    row.add(create);
    name = new TextField("");
    row.add(name);
    back = new Button("Back");
    back.setFontSize(56);
    back.addClickListener((me, c) -> {
      c.client.setPanel(new MainPanel());
    });
    add(back);
  }
  public void loadPanels() {
    SQL sql = new SQL();
    sql.connect(Service.derbyURI);
    String panels[][] = sql.select("select id,name from panels");
    panel.clear();
    for(int a=0;a<panels.length;a++) {
      panel.add(panels[a][0], panels[a][1]);
    }
    sql.close();
  }
  ComboBox panel;
  Button view;
  Button delete;
  Button create;
  TextField name;
  Button back;
}
