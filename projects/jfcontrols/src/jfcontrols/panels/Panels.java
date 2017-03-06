package jfcontrols.panels;

/** Panel
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;

public class Panels {
  public static String cellWidth = "64";
  public static String cellHeight = "64";
  public static Panel getLoginPanel() {
    return null;
  }
  public static Panel getMainPanel() {
    return null;
  }
  public static Panel getTagsPanel() {
    return null;
  }
  public static Panel getPanelsPanel() {
    return null;
  }
  //...
  public static Panel getPanel(String pname) {
    SQL sql = SQLService.getSQL();
    String pid = sql.select1value("select id from panels where name=" + SQL.quote(pname));
    if (pid == null) return null;
    String cells[][] = sql.select("select id,x,y,w,h,name,text,tag,func,arg,style from cells where pid=" + SQL.quote(pid));
    Panel panel = new Panel();
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
    panel.add(table);
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
      table.add(c, x, y);
    }
    return panel;
  }
  public static Component getCell(String name, String cid, String v[]) {
    switch (name) {
      case "button": return getButton(cid, v);
    }
    return null;
  }
  //id,x,y,w,h,name,text,tag,func,arg,style
  private static Button getButton(String cid, String v[]) {
    Button b = new Button(v[6]);
    b.setProperty("func", v[8]);
    b.setProperty("arg", v[9]);
    b.addClickListener((me, c) -> {
      Events.click(c);
    });
    return b;
  }
}
