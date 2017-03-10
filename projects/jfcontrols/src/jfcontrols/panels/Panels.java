package jfcontrols.panels;

/** Panels
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;

public class Panels {
  public static String cellWidth = "64";
  public static String cellHeight = "64";
  public static PopupPanel getLoginPanel(WebUIClient client) {
    PopupPanel panel = (PopupPanel)getPanel(new PopupPanel("Login"), "_jfc_login", client);
    client.setProperty("popup_panel", panel);
    return panel;
  }
  public static PopupPanel getMenuPanel(WebUIClient client) {
    return null;
  }
  public static Panel getTagsPanel(WebUIClient client) {
    return null;
  }
  public static Panel getPanelsPanel(WebUIClient client) {
    return null;
  }
  //...
  public static Panel getPanel(String pname, WebUIClient client) {
    return getPanel(new Panel(), pname, client);
  }
  public static Panel getPanel(Panel panel, String pname, WebUIClient client) {
    SQL sql = SQLService.getSQL();
    String pid = sql.select1value("select id from panels where name=" + SQL.quote(pname));
    if (pid == null) {
      JFLog.log("Error:Unable to find panel:" + pname);
      return null;
    }
    String builtin = sql.select1value("select builtin from panels where id=" + pid);
    String cells[][] = sql.select("select id,x,y,w,h,name,text,tag,func,arg,style from cells where pid=" + pid);
    sql.close();
    panel.add(getTable(cells, builtin.equals("true")));
    if (builtin.equals("true")) return panel;
    if (client.getProperty("user") == null) {
      panel.add(getLoginPanel(client));
    } else {
      panel.add(getMenuPanel(client));
    }
    return panel;
  }
  private static Table getTable(String cells[][], boolean builtin) {
    int mx = 1;
    int my = 1;
    for(int a=0;a<cells.length;a++) {
      int x = Integer.valueOf(cells[a][1]);
      int y = Integer.valueOf(cells[a][2]);
      int w = Integer.valueOf(cells[a][3]);
      int h = Integer.valueOf(cells[a][4]);
      if (x + w > mx) {
        mx = x + w;
      }
      if (y + h > my) {
        my = y + h;
      }
    }
    Table table = new Table(Integer.valueOf(cellWidth),Integer.valueOf(cellHeight),mx,my);
    for(int a=0;a<cells.length;a++) {
      String cid = cells[a][0];
      int x = Integer.valueOf(cells[a][1]);
      int y = Integer.valueOf(cells[a][2]);
      int w = Integer.valueOf(cells[a][3]);
      int h = Integer.valueOf(cells[a][4]);
      String cname = cells[a][5];
      Component c = getCell(cname, cid, cells[a]);
      c.setWidth(cellWidth);
      c.setHeight(cellHeight);
      if (w == 1 && h == 1)
        table.add(c, x, y);
      else
        table.add(c, x, y, w, h);
    }
    return table;
  }
  public static Component getCell(String name, String cid, String v[]) {
    switch (name) {
      case "label": return getLabel(cid, v);
      case "button": return getButton(cid, v);
      case "textfield": return getTextField(cid, v);
    }
    return null;
  }
  //id,x,y,w,h,name,text,tag,func,arg,style
  private final static int X = 1;
  private final static int Y = 2;
  private final static int W = 3;
  private final static int H = 4;
  private final static int NAME = 5;
  private final static int TEXT = 6;
  private final static int TAG = 7;
  private final static int FUNC = 8;
  private final static int ARG = 9;
  private final static int STYLE = 10;
  private static Label getLabel(String cid, String v[]) {
    Label b = new Label(v[TEXT]);
    return b;
  }
  private static Button getButton(String cid, String v[]) {
    Button b = new Button(v[TEXT]);
    b.setProperty("func", v[FUNC]);
    b.setProperty("arg", v[ARG]);
    b.addClickListener((me, c) -> {
      Events.click(c);
    });
    return b;
  }
  private static TextField getTextField(String cid, String v[]) {
    TextField b = new TextField(v[TEXT]);
    return b;
  }
}
