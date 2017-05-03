package jfcontrols.panels;

/** Panels
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;
import jfcontrols.tags.*;

public class Panels {
  public static int cellWidth = 32;
  public static int cellHeight = 32;
  public static PopupPanel getLoginPanel(WebUIClient client) {
    PopupPanel panel = (PopupPanel)getPanel(createPopupPanel("Login"), "jfc_login", client);
    panel.setName("login_panel");
    return panel;
  }
  public static PopupPanel getMenuPanel(WebUIClient client) {
    PopupPanel panel = (PopupPanel)getPanel(createPopupPanel("Menu"), "jfc_main", client);
    panel.setName("menu_panel");
    return panel;
  }
  public static Panel getTagsPanel(WebUIClient client) {
    return null;
  }
  public static Panel getPanelsPanel(WebUIClient client) {
    return null;
  }
  private static PopupPanel createPopupPanel(String title) {
    PopupPanel pp = new PopupPanel(title);
    pp.setTitleBarSize(cellHeight + "px");
    return pp;
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
    String popup = sql.select1value("select popup from panels where id=" + pid);
    String cells[][] = sql.select("select id,x,y,w,h,comp,name,text,tag,func,arg,style from cells where pid=" + pid);
    sql.close();
    panel.add(getTable(cells, popup.equals("true"), client));
    if (popup.equals("true")) return panel;
    panel.add(getLoginPanel(client));
    panel.add(getMenuPanel(client));
    return panel;
  }
  //id,x,y,w,h,name,text,tag,func,arg,style
  private final static int ID = 0;
  private final static int X = 1;
  private final static int Y = 2;
  private final static int W = 3;
  private final static int H = 4;
  private final static int COMP = 5;
  private final static int NAME = 6;
  private final static int TEXT = 7;
  private final static int TAG = 8;
  private final static int FUNC = 9;
  private final static int ARG = 10;
  private final static int STYLE = 11;
  private static Table getTable(String cells[][], boolean popup, WebUIClient client) {
    int mx = 1;
    int my = 1;
    Component cs[] = new Component[cells.length];
    Rectangle rs[] = new Rectangle[cells.length];
    for(int a=0;a<cells.length;a++) {
      Rectangle r = new Rectangle();
      rs[a] = r;
      r.x = Integer.valueOf(cells[a][X]);
      r.y = Integer.valueOf(cells[a][Y]);
      r.width = Integer.valueOf(cells[a][W]);
      r.height = Integer.valueOf(cells[a][H]);
      String comp = cells[a][COMP];
      Component c = getCell(comp, cells[a], rs[a], client);
      cs[a] = c;
      int x2 = rs[a].x + rs[a].width;
      if (x2 > mx) {
        mx = x2;
      }
      int y2 = rs[a].y + rs[a].height;
      if (y2 > my) {
        my = y2;
      }
      setCellSize(c, rs[a].width, rs[a].height);
      c.setProperty("id", cells[a][ID]);
      c.setName(cells[a][NAME]);
      String style = cells[a][STYLE];
      if (style != null) {
        String styles[] = style.split(";");
        for(int b=0;b<styles.length;b++) {
          if (styles[b].equals("readonly")) {
            c.setReadonly(true);
          } else if (styles[b].equals("disabled")) {
            c.setDisabled(true);
          } else {
            String f[] = styles[b].split("=");
            c.setStyle(f[0], f[1]);
          }
        }
      }
    }
    Table table = new Table(cellWidth,cellHeight,mx,my);
    for(int a=0;a<cells.length;a++) {
      if (rs[a].width == 1 && rs[a].height == 1)
        table.add(cs[a], rs[a].x, rs[a].y);
      else
        table.add(cs[a], rs[a].x, rs[a].y, rs[a].width, rs[a].height);
    }
    if (!popup) {
      //add top components
      Button x = getButton(new String[] {null, null, null, null, null, "button", null, "X", null, "showMenu", null});
      setCellSize(x, 1, 1);
      table.add(x, 0, 0);
      //TODO : [alarm status] : [title]
    }
    return table;
  }
  private static void setCellSize(Component c, int w, int h) {
    c.setWidth(Integer.toString(cellWidth * w));
    c.setHeight(Integer.toString(cellHeight * h));
  }
  public static Component getCell(String name, String v[], Rectangle r, WebUIClient client) {
    switch (name) {
      case "label": return getLabel(v);
      case "button": return getButton(v);
      case "textfield": return getTextField(v);
      case "combobox": return getComboBox(v);
      case "table": return getTable(v, r, client);
    }
    return null;
  }
  private static Label getLabel(String v[]) {
    Label b = new Label(v[TEXT]);
    return b;
  }
  private static Button getButton(String v[]) {
    Button b = new Button(v[TEXT]);
    b.setProperty("func", v[FUNC]);
    b.setProperty("arg", v[ARG]);
    b.addClickListener((me, c) -> {
      Events.click(c);
    });
    return b;
  }
  private static TextField getTextField(String v[]) {
    SQL sql = SQLService.getSQL();
    String tag = v[TAG];
    String text = null;
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_");
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        text = sql.select1value("select " + col + " from " + table + " where id=" + id);
      } else {
        text = TagsService.read(tag);
      }
    }
    if (text == null) text = "";
    TextField b = new TextField(text);
    b.setProperty("tag", tag);
    b.addChangedListener((c) -> {
      Events.edit((TextField)c);
    });
    sql.close();
    return b;
  }
  private static ComboBox getComboBox(String v[]) {
    ComboBox cb = new ComboBox();
    String tag = v[TAG];
    String arg = v[ARG];
    SQL sql = SQLService.getSQL();
    String lid = sql.select1value("select id from lists where name=" + SQL.quote(arg));
    String pairs[][] = sql.select("select value, text from listdata where lid=" + lid);
    String value = null;
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_");
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        value = sql.select1value("select " + col + " from " + table + " where id=" + id);
      } else {
        value = TagsService.read(tag);
      }
    }
    sql.close();
    int selidx = -1;
    if (pairs != null) {
      for(int a=0;a<pairs.length;a++) {
        cb.add(pairs[a][0], pairs[a][1]);
        if (value != null && pairs[a][0].equals(value)) {
          selidx = a;
        }
      }
    }
    if (selidx != -1) {
      cb.setSelectedIndex(selidx);
    }
    cb.setProperty("tag", tag);
    cb.addChangedListener((c) -> {
      Events.changed((ComboBox)c);
    });
    return cb;
  }
  private static String[] createCell(String id, int x, int y, int w, int h, String comp, String name, String text, String tag, String func, String arg, String style) {
    String cell[] = new String[12];
    cell[0] = id;
    cell[1] = Integer.toString(x);
    cell[2] = Integer.toString(y);
    cell[3] = Integer.toString(w);
    cell[4] = Integer.toString(h);
    cell[5] = comp;
    cell[6] = name;
    cell[7] = text;
    cell[8] = tag;
    cell[9] = func;
    cell[10] = arg;
    cell[11] = style;
    return cell;
  }
//   cells[][] = "id,x,y,w,h,comp,name,text,tag,func,arg,style"
  private static Table getTable(String v[], Rectangle r, WebUIClient client) {
    String name = v[NAME];
    SQL sql = SQLService.getSQL();
    ArrayList<String[]> cells = new ArrayList<String[]>();
    switch (name) {
      case "jfc_ctrls" : {
        r.width = 14;
        String data[][] = sql.select("select id,num,ip,type from ctrls");
        if (data == null) data = new String[0][0];
        r.height = data.length;
        for(int a=0;a<data.length;a++) {
          String style = data[a][1].equals("0") ? "disabled" : null;
          cells.add(createCell("", 0, a, 1, 1, "textfield", null, data[a][1], "jfc_ctrls_num_int_" + data[a][0], null, null, style));
          cells.add(createCell("", 1, a, 3, 1, "textfield", null, data[a][2], "jfc_ctrls_ip_str_" + data[a][0], null, null, style));
          cells.add(createCell("", 4, a, 2, 1, "combobox", null, null, "jfc_ctrls_type_int_" + data[a][0], null, "jfc_ctrl_type", style));
          cells.add(createCell("", 6, a, 2, 1, "combobox", null, null, "jfc_ctrls_speed_int_" + data[a][0], null, "jfc_ctrl_speed", style));
          cells.add(createCell("", 9, a, 2, 1, "button", null, "Tags", null, "jfc_ctrl_tags", data[a][0], null));
          cells.add(createCell("", 12, a, 2, 1, "button", null, "Delete", null, "jfc_ctrl_delete", data[a][1], null));
        }
        break;
      }
      case "jfc_tags": {
        r.width = 12;
        String data[][] = sql.select("select id,cid,name,type from tags where cid=" + client.getProperty("ctrl"));
        if (data == null) data = new String[0][0];
        r.height = data.length;
        for(int a=0;a<data.length;a++) {
          cells.add(createCell("", 0, a, 6, 1, "textfield", null, null, "jfc_tags_name_str_" + data[a][0], null, null, null));
          cells.add(createCell("", 6, a, 3, 1, "combobox", null, null, "jfc_tags_type_int_" + data[a][0], null, "jfc_tag_type", null));
          cells.add(createCell("", 10, a, 2, 1, "button", null, "Delete", null, "jfc_tags_delete", data[a][0], null));
        }
        break;
      }
    }
    sql.close();
    return getTable(cells.toArray(new String[cells.size()][]), true, client);
  }
}
