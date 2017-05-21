package jfcontrols.panels;

/** Panels
 *
 * @author pquiring
 */

import jfcontrols.logic.Logic;
import java.util.*;

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;
import jfcontrols.tags.*;
import jfcontrols.images.*;

public class Panels {
  public static int cellWidth = 32;
  public static int cellHeight = 32;
  public static PopupPanel getLoginPanel(WebUIClient client) {
    PopupPanel panel = (PopupPanel)buildPanel(createPopupPanel("Login"), "jfc_login", client);
    panel.setName("login_panel");
    return panel;
  }
  public static PopupPanel getMenuPanel(WebUIClient client) {
    PopupPanel panel = (PopupPanel)buildPanel(createPopupPanel("Menu"), "jfc_main", client);
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
    pp.setTitleBarSize(cellHeight);
    return pp;
  }
  //...
  public static Panel getPanel(String pname, WebUIClient client) {
    return buildPanel(new Panel(), pname, client);
  }
  public static Panel buildPanel(Panel panel, String pname, WebUIClient client) {
    SQL sql = SQLService.getSQL();
    String pid = sql.select1value("select id from panels where name=" + SQL.quote(pname));
    if (pid == null) {
      JFLog.log("Error:Unable to find panel:" + pname);
      return null;
    }
    String popup = sql.select1value("select popup from panels where id=" + pid);
    String cells[][] = sql.select("select id,x,y,w,h,comp,name,text,tag,func,arg,style from cells where pid=" + pid);
    sql.close();
    Table table = new Table(cellWidth,cellHeight,1,1);
    panel.add(table);
    buildTable(table, panel, cells, client, -1, -1, null);
    if (popup.equals("true")) return panel;
    //add top components
    Button x = getButton(new String[] {null, null, null, null, null, "button", null, "!image:menu", null, "showMenu", null});
    setCellSize(x, new Rectangle(0,0,1,1));
    table.add(x, 0, 0);
    //TODO : [alarm status] : [title]
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
  public static Table buildTable(Table table, Container container, String cells[][], WebUIClient client, int ix, int iy, Node nodes[]) {
    int mx = table.getColumns();
    if (ix != -1) mx = ix;
    int my = table.getRows();
    if (iy != -1) my = iy;
    Component cs[] = new Component[cells.length];
    Rectangle rs[] = new Rectangle[cells.length];
    boolean flow[] = new boolean[cells.length];
    for(int a=0;a<cells.length;a++) {
      Rectangle r = new Rectangle();
      rs[a] = r;
      r.x = Integer.valueOf(cells[a][X]);
      r.y = Integer.valueOf(cells[a][Y]);
      r.width = Integer.valueOf(cells[a][W]);
      r.height = Integer.valueOf(cells[a][H]);
      String compType = cells[a][COMP];
      Component c = getCell(compType, container, cells[a], rs[a], client);
      if (c == null) {
        JFLog.log("Error:cell == null:" + compType);
        c = new Label("null");
      }
      cs[a] = c;
      int x2 = rs[a].x + rs[a].width;
      if (x2 > mx) {
        mx = x2;
      }
      int y2 = rs[a].y + rs[a].height;
      if (y2 > my) {
        my = y2;
      }
      setCellSize(c, rs[a]);
      c.setProperty("id", cells[a][ID]);
      c.setName(cells[a][NAME]);
      if (nodes != null && nodes.length > a) {
        c.setProperty("node", nodes[a]);
        nodes[a].comp = c;
        c.addClickListener((me, comp) -> {
//          WebUIClient client = comp.getClient();
          Component focus = (Component)client.getProperty("focus");
          if (focus != null) {
            focus.setBorder(false);
          }
          Node node = (Node)comp.getProperty("node");
          JFLog.log("node=" + node);
          comp.setBorderColor("#000");
          comp.setBorder(true);
          client.setProperty("focus", comp);
          Node src = (Node)client.getProperty("fork");
          if (src != null) {
            node.forkDest(client, table, src);
          }
        });
      }
      String style = cells[a][STYLE];
      if (style != null) {
        String styles[] = style.split(";");
        for(int b=0;b<styles.length;b++) {
          if (styles[b].equals("readonly")) {
            c.setReadonly(true);
          } else if (styles[b].equals("disabled")) {
            c.setDisabled(true);
          } else if (styles[b].equals("flow")) {
            flow[a] = true;
          } else {
            String f[] = styles[b].split("=");
            if (f.length == 2) {
              c.setStyle(f[0], f[1]);
            }
          }
        }
      }
    }
    table.setTableSize(mx, my);
    for(int a=0;a<cells.length;a++) {
      if (flow[a]) {
        JFLog.log("flow:" + cs[a]);
        container.add(cs[a]);
      } else {
        if (rs[a].width == 1 && rs[a].height == 1)
          table.add(cs[a], rs[a].x, rs[a].y);
        else
          table.add(cs[a], rs[a].x, rs[a].y, rs[a].width, rs[a].height);
      }
    }
    return table;
  }
  public static Component setCellSize(Component c, Rectangle r) {
    c.setSize(cellWidth * r.width, cellHeight * r.height);
    c.setProperty("rect", r);
    return c;
  }
  public static Component getCell(String name, Container container, String v[], Rectangle r, WebUIClient client) {
    switch (name) {
      case "label": return getLabel(v);
      case "button": return getButton(v);
      case "textfield": return getTextField(v);
      case "combobox": return getComboBox(v);
      case "table": return getTable(v, container, r, client);
      case "overlay": return getOverlay(v);
      case "image": return getImage(v);
      default: JFLog.log("Unknown component:" + name); break;
    }
    return null;
  }
  private static Label getLabel(String v[]) {
    Label b = new Label(v[TEXT]);
    return b;
  }
  private static Button getButton(String v[]) {
    String text = v[TEXT];
    Button b = null;
    if (text.startsWith("!image:")) {
      b = new Button(Images.getImage(text.substring(7)));
    } else {
      b = new Button(v[TEXT]);
    }
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
    String text = v[TEXT];
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
    String name = v[NAME];
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
    if (tag != null) {
      cb.setProperty("tag", tag);
      cb.addChangedListener((c) -> {
        Events.changed((ComboBox)c);
      });
    }
    if (name != null) {
      switch (name) {
        case "group_type":
          cb.addChangedListener((c) -> {
            ComboBox groups = (ComboBox)c;
            WebUIClient client = c.getClient();
            TabPanel tabs = (TabPanel)client.getProperty("groups");
            tabs.setTabIndex(groups.getSelectedIndex());
          });
          break;
      }
    }
    return cb;
  }
  private static Image getImage(String v[]) {
    String arg = v[ARG];
    return new Image(Images.getImage(arg));
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
  private static boolean empty(String [][] cells, int cx, int cy) {
    int cnt = cells.length;
    for(int a=0;a<cnt;a++) {
      String cell[] = cells[a];
      int x = Integer.valueOf(cell[X]);
      int y = Integer.valueOf(cell[Y]);
      int w = Integer.valueOf(cell[W]) - 1;
      int h = Integer.valueOf(cell[H]) - 1;
      if ( (cx >= x && cx <= x + w) && (cy >= y && cy <= y + h) ) {
        return false;
      }
    }
    return true;
  }
//   cells[][] = "id,x,y,w,h,comp,name,text,tag,func,arg,style"
  private static Component getTable(String v[], Container container, Rectangle r, WebUIClient client) {
    String name = v[NAME];
    String arg = v[ARG];
    SQL sql = SQLService.getSQL();
    ArrayList<String[]> cells = new ArrayList<String[]>();
    ArrayList<Node> objs = new ArrayList<Node>();
    Table table;
    switch (name) {
      case "jfc_ctrls" : {
        String data[][] = sql.select("select id,cid,ip,type from ctrls");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          String style = data[a][1].equals("0") ? "disabled" : null;
          cells.add(createCell("", 0, a, 1, 1, "textfield", null, data[a][1], "jfc_ctrls_cid_int_" + data[a][0], null, null, style));
          cells.add(createCell("", 1, a, 3, 1, "textfield", null, data[a][2], "jfc_ctrls_ip_str_" + data[a][0], null, null, style));
          cells.add(createCell("", 4, a, 2, 1, "combobox", null, null, "jfc_ctrls_type_int_" + data[a][0], null, "jfc_ctrl_type", style));
          cells.add(createCell("", 6, a, 2, 1, "combobox", null, null, "jfc_ctrls_speed_int_" + data[a][0], null, "jfc_ctrl_speed", style));
          cells.add(createCell("", 9, a, 2, 1, "button", null, "Tags", null, "jfc_ctrl_tags", data[a][0], null));
          if (style == null) {
            cells.add(createCell("", 12, a, 2, 1, "button", null, "Delete", null, "jfc_ctrl_delete", data[a][1], null));
          }
        }
        break;
      }
      case "jfc_tags": {
        String data[][] = sql.select("select id,cid,name,type from tags where cid=" + client.getProperty("ctrl"));
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell("", 0, a, 6, 1, "textfield", null, null, "jfc_tags_name_str_" + data[a][0], null, null, null));
          cells.add(createCell("", 6, a, 3, 1, "combobox", null, null, "jfc_tags_type_int_" + data[a][0], null, "jfc_tag_type", null));
          cells.add(createCell("", 10, a, 2, 1, "button", null, "Delete", null, "jfc_tags_delete", data[a][0], null));
        }
        break;
      }
      case "jfc_panels": {
        String data[][] = sql.select("select id,name from panels where builtin=false");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          String style = data[a][1].equals("main") ? "disabled" : null;
          cells.add(createCell("", 0, a, 6, 1, "textfield", null, null, "jfc_panels_name_str_" + data[a][0], null, null, style));
          cells.add(createCell("", 7, a, 2, 1, "button", null, "Edit", null, "jfc_panels_edit", data[a][0], null));
          if (style == null) {
            cells.add(createCell("", 10, a, 2, 1, "button", null, "Delete", null, "jfc_panels_delete", data[a][0], null));
          }
        }
        break;
      }
      case "jfc_panel_editor": {
        String pid = (String)client.getProperty("panel");
        String data[][] = sql.select("select id,x,y,w,h,comp,name,text,tag,func,arg,style from cells where pid=" + pid);
        sql.close();
        for(int a=0;a<data.length;a++) {
          cells.add(data[a]);
        }
        LayersPanel layers = new LayersPanel();
        table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, 64, 64, null);
        table.setName("t1");
        r.width = table.getColumns();
        r.height = table.getRows();
        layers.add(table);
        cells.clear();
        for(int a=0;a<data.length;a++) {
          String cell[] = data[a];
          cell[ID] = null;
          cell[COMP] = "overlay";
          cell[NAME] = "";
          cell[TEXT] = "";
          cell[TAG] = null;
          cell[FUNC] = null;
          cell[ARG] = null;
          cell[STYLE] = null;
          cells.add(data[a]);
        }
        String cellsArray[][] = cells.toArray(new String[cells.size()][]);
        for(int x=0;x<64;x++) {
          for(int y=0;y<64;y++) {
            if (empty(cellsArray,x,y)) {
              cells.add(createCell("", x, y, 1, 1, "overlay", null, null, null, null, null, null));
            }
          }
        }
        table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, 64, 64, null);
        table.setName("t2");
        layers.add(table);
        return layers;
      }
      case "jfc_funcs": {
        String data[][] = sql.select("select id,name from funcs");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          String funcname = data[a][1];
          String style = funcname.equals("main") || funcname.equals("init") ? "disabled" : null;
          cells.add(createCell("", 0, a, 6, 1, "textfield", null, null, "jfc_funcs_name_str_" + data[a][0], null, null, style));
          cells.add(createCell("", 7, a, 2, 1, "button", null, "Edit", null, "jfc_funcs_edit", data[a][0], null));
          if (style == null) {
            cells.add(createCell("", 10, a, 2, 1, "button", null, "Delete", null, "jfc_funcs_delete", data[a][0], null));
          }
        }
        break;
      }
      case "jfc_rung_groups": {
        TabPanel tabs = new TabPanel();
        tabs.setTabsVisible(false);
        tabs.setBorders(false);
        tabs.add(wrapPanel(getTable(createCell(null, r.x, r.y, r.width, r.height, "table", "jfc_rung_bits", null, null, null, null, null), null, new Rectangle(r), client)), "");
        tabs.add(wrapPanel(getTable(createCell(null, r.x, r.y, r.width, r.height, "table", "jfc_rung_math", null, null, null, null, null), null, new Rectangle(r), client)), "");
        setCellSize(tabs, r);
        client.setProperty("groups", tabs);
        return tabs;
      }
      case "jfc_rung_bits": {
        cells.add(createCell("", 0, 0, 1, 1, "button", "xon", "!image:xon", null, "jfc_rung_editor_add", null, null));
        cells.add(createCell("", 1, 0, 1, 1, "button", "xoff", "!image:xoff", null, "jfc_rung_editor_add", null, null));
        cells.add(createCell("", 2, 0, 1, 1, "button", "coil", "!image:coil", null, "jfc_rung_editor_add", null, null));
        break;
      }
      case "jfc_rung_math": {
        cells.add(createCell("", 0, 0, 1, 1, "button", "add", "Add", null, "jfc_rung_editor_add", null, null));
        cells.add(createCell("", 1, 0, 1, 1, "button", "sub", "Sub", null, "jfc_rung_editor_add", null, null));
        break;
      }
      case "jfc_rungs_viewer": {
        String fid = (String)client.getProperty("func");
        String data[][] = sql.select("select rid,logic,comment from rungs where fid=" + fid);
        client.setProperty("rungs", new Rungs());
        buildRungs(data, cells, sql);
        break;
      }
      case "jfc_rung_viewer": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + arg);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        r.y = rungs.y;
        rungs.rungs.add(buildRung(data, cells, objs, sql, true, fid));
        break;
      }
      case "jfc_rung_editor": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        int rid = Integer.valueOf((String)client.getProperty("rung"));
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + rid);
        buildRung(data, cells, objs, sql, false, fid);
        break;
      }
      default: {
        JFLog.log("Unknown table:" + name);
      }
    }
    sql.close();
    table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new String[cells.size()][]), client, -1, -1, objs.toArray(new Node[objs.size()]));
    r.width = table.getColumns();
    r.height = table.getRows();
    switch (name) {
      case "jfc_rungs_viewer": {
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.table = table;
        break;
      }
      case "jfc_rung_viewer": {
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.y += r.height;
        rungs.rungs.get(rungs.rungs.size() - 1).table = table;
        break;
      }
    }
    return table;
  }
  private static Component getOverlay(String v[]) {
    Block div = new Block();
    div.setBorder(true);
    div.setBorderColor("#000000");
    div.addClickListener((me, comp) -> {
      WebUIClient client = comp.getClient();
      Block focus = (Block)client.getProperty("focus");
      if (focus != null) {
        focus.setBorderColor("#000000");
      }
      comp.setBorderColor("#00ff00");
      client.setProperty("focus", comp);
    });
    return div;
  }
  private static Component getOverlay(int x,int y) {
    Component c = getOverlay(null);
    Rectangle r = new Rectangle(x,y,1,1);
    setCellSize(c, r);
    return c;
  }
  private static Panel wrapPanel(Component comp) {
    Panel p = new Panel();
    p.add(comp);
    return p;
  }
  public static void moveCell(WebUIClient client, int deltax, int deltay) {
    Block focus = (Block)client.getProperty("focus");
    if (focus == null) {
      JFLog.log("Error:no focus");
      return;
    }
    Rectangle fr = (Rectangle)focus.getProperty("rect");
    //calc new position
    int x1 = fr.x + deltax;
    int x2 = fr.x + fr.width + deltax - 1;
    int y1 = fr.y + deltay;
    int y2 = fr.y + fr.height + deltay - 1;
    if ((x1 < 0) || (x2 > 63) || (y1 < 0) || (y2 > 63)) return;  //off screen
    Table t1 = (Table)client.getPanel().getComponent("t1");  //components
    Component comp = t1.get(fr.x, fr.y, false);
    if (comp == null) {
      JFLog.log("Error:nothing there:" + fr.x + "," + fr.y);
      return;
    }
    Rectangle cr = (Rectangle)comp.getProperty("rect");
    Table t2 = (Table)client.getPanel().getComponent("t2");  //overlays
    for(int x=x1;x<=x2;x++) {
      for(int y=y1;y<=y2;y++) {
        Component cell = t1.get(x, y, true);
        if (cell == null) continue;
        if (cell.id == comp.id) continue;
        JFLog.log("Error: something in the way:" + x + "," + y + ":" + t1.get(x, y, true).id);
        return;
      }
    }
    moveComponent(t1, fr.x, fr.y, x1, y1, false);
    moveComponent(t2, fr.x, fr.y, x1, y1, true);
  }
  private static void moveComponent(Table tbl, int sx, int sy, int dx, int dy, boolean fillOverlay) {
    Component c = tbl.get(sx, sy, false);
    Rectangle r = (Rectangle)c.getProperty("rect");
    int x1 = r.x;
    int y1 = r.y;
    int x2 = x1 + r.width - 1;
    int y2 = y1 + r.height - 1;
    int x,y;
    //remove from src pos
    for(x = x1;x <= x2;x++) {
      for(y = y1;y <= y2;y++) {
        if (x == x1 && y == y1) {
          tbl.remove(x, y);
          if (fillOverlay) {
            tbl.add(getOverlay(x, y), x, y);
          }
        } else {
          if (fillOverlay) {
            tbl.add(getOverlay(x, y), x, y);
          }
        }
      }
    }
    int deltax = dx - sx;
    int deltay = dy - sy;
    x1 += deltax;
    x2 += deltax;
    y1 += deltay;
    y2 += deltay;
    r.x += deltax;
    r.y += deltay;
    //set to dest pos
    for(x = x1;x <= x2;x++) {
      for(y = y1;y <= y2;y++) {
        tbl.remove(x, y);
        if (x == x1 && y == y1) {
          tbl.add(c, x, y);
          tbl.setSpans(x, y, r.width, r.height);
        }
      }
    }
  }
  public static void resizeCell(WebUIClient client, int deltax, int deltay) {
    Block focus = (Block)client.getProperty("focus");
    if (focus == null) {
      JFLog.log("Error:no focus");
      return;
    }
    Rectangle fr = (Rectangle)focus.getProperty("rect");
    //calc new position
    int x1 = fr.x;
    int x2 = fr.x + fr.width + deltax - 1;
    int y1 = fr.y;
    int y2 = fr.y + fr.height + deltay - 1;
    if ((x1 < 0) || (x2 > 63) || (y1 < 0) || (y2 > 63)) return;  //off screen
    if (x2 < x1 || y2 < y1) return;  //too small
    Table t1 = (Table)client.getPanel().getComponent("t1");  //components
    Component comp = t1.get(fr.x, fr.y, false);
    if (comp == null) {
      JFLog.log("Error:nothing there:" + fr.x + "," + fr.y);
      return;
    }
    Rectangle cr = (Rectangle)comp.getProperty("rect");
    Table t2 = (Table)client.getPanel().getComponent("t2");  //overlays
    for(int x=x1;x<=x2;x++) {
      for(int y=y1;y<=y2;y++) {
        Component cell = t1.get(x, y, true);
        if (cell == null) continue;
        if (cell.id == comp.id) continue;
        JFLog.log("Error: something in the way:" + x + "," + y + ":" + t1.get(x, y, true).id);
        return;
      }
    }
    resizeComponent(t1, fr.x, fr.y, deltax, deltay, false);
    resizeComponent(t2, fr.x, fr.y, deltax, deltay, true);
  }
  private static void resizeComponent(Table tbl, int cx, int cy, int deltax, int deltay, boolean fillOverlay) {
    Component c = tbl.get(cx, cy, false);
    Rectangle r = (Rectangle)c.getProperty("rect");
    int x1 = r.x;
    int y1 = r.y;
    int x2 = x1 + r.width - 1;
    int y2 = y1 + r.height - 1;
    int x,y;
    //remove from src pos
    for(x = x1;x <= x2;x++) {
      for(y = y1;y <= y2;y++) {
        if (x == x1 && y == y1) {
          tbl.remove(x, y);
          if (fillOverlay) {
            tbl.add(getOverlay(x, y), x, y);
          }
        } else {
          if (fillOverlay) {
            tbl.add(getOverlay(x, y), x, y);
          }
        }
      }
    }
    x2 += deltax;
    y2 += deltay;
    r.width += deltax;
    r.height += deltay;
    //set to dest pos
    for(x = x1;x <= x2;x++) {
      for(y = y1;y <= y2;y++) {
        tbl.remove(x, y);
        if (x == x1 && y == y1) {
          setCellSize(c, r);
          tbl.add(c, x, y);
          tbl.setSpans(x, y, r.width, r.height);
        }
      }
    }
  }
  public static void buildRungs(String data[][], ArrayList<String[]> cells, SQL sql) {
    for(int rung=0;rung<data.length;rung++) {
      cells.add(createCell(null, 0, 0, 1, 1, "table", "jfc_rung_viewer", null, null, null, data[rung][0], "flow"));
    }
  }
  public static Rung buildRung(String data[], ArrayList<String[]> cells, ArrayList<Node> objs, SQL sql, boolean readonly, int fid) {
    int x = 0;
    int y = 0;
    int sh = 1;  //segment height
    int rid = Integer.valueOf(data[0]);
    Rung rung = new Rung();
    String logic = data[1];
    String comment = data[2];
    String parts[] = logic.split("|");
    String blocks[][] = sql.select("select bid,name,tags from blocks where fid=" + fid + " and rid=" + rid);
    ArrayList<Node> nodes = new ArrayList<Node>();
    NodeRoot root = new NodeRoot(fid, rid);

    //add rung title / comment
    String style = readonly ? "readonly" : null;
    String field = readonly ? "label" : "textfield";
    cells.add(createCell(null, x, y, 3, 1, "label", null, "Rung " + rid, null, null, null, null));
    objs.add(root);
    x += 3;
    cells.add(createCell(null, x, y, 12, 1, field, "comment" + rid, comment, null, null, null, style));
    objs.add(root);
    x = 0;
    y++;

    Node node = root;
    for(int p=0;p<parts.length;p++) {
      String part = parts[p];
      switch (part) {
        case "t": {
          nodes.add(node = node.insertNode('t', x, y));
          x++;
          break;
        }
        case "h":
          nodes.add(node = node.insertNode('h', x, y));
          x++;
          break;
        case "v":
          JFLog.log("Error:'v' found in logic");
          nodes.add(node = node.insertNode('v', x, y));
          y++;
          break;
        case "a": {
          //a can only be under t,a
          Node upper = Node.findFirstOpenNode(nodes, "ta");
          if (upper == null) {
            JFLog.log("Error:corrupt logic");
            return null;
          }
          x = upper.x;
          y = upper.y + sh;
          sh = 1;
          nodes.add(node = node.insertLinkUpper(upper, 'a', x, y));
          break;
        }
        case "b": {
          //b can only be under t,b
          Node upper = Node.findLastOpenNode(nodes, "tb");
          if (upper == null) {
            JFLog.log("Error:corrupt logic");
            return null;
          }
          if (upper.x < x) upper.x = x;
          if (upper.x > x) x = upper.x;
          sh = 1;
          nodes.add(node = node.insertLinkUpper(upper, 'b', x, y));
          break;
        }
        case "c": {
          //c can only be under t,a
          Node upper = Node.findFirstOpenNode(nodes, "ta");
          if (upper == null) {
            JFLog.log("Error:corrupt logic");
            return null;
          }
          x = upper.x;
          y = upper.y + sh;
          sh = 1;
          nodes.add(node = node.insertLinkUpper(upper, 'c', x, y));
          break;
        }
        case "d": {
          //d can only be under t,b
          Node upper = Node.findFirstOpenNode(nodes, "tb");
          if (upper == null) {
            JFLog.log("Error:corrupt logic");
            return null;
          }
          if (upper.x < x) upper.x = x;
          if (upper.x > x) x = upper.x;
          sh = 1;
          nodes.add(node = node.insertLinkUpper(upper, 'd', x, y));
          break;
        }
        default: {
          nodes.add(node = node.insertNode('h', x, y));
          x++;
          String name = null;
          String tags = null;
          for(int a=0;a<blocks.length;a++) {
            if (blocks[a][0].equals(part)) {
              name = blocks[a][1];
              tags = blocks[a][2];
              break;
            }
          }
          if (name == null) {
            JFLog.log("Error:Block not found:rid=" + rid + ":bid=" + part + ":name=");
            continue;
          }
          JFLog.log("name=" + name + ",tags=" + tags);
          Logic blk = null;
          try {
            Class cls = Class.forName("jfcontrols.logic." + name.toUpperCase());
            blk = (Logic)cls.newInstance();
          } catch (Exception e) {
            JFLog.log(e);
          }
          if (blk == null) {
            JFLog.log("Error:Block not found:rid=" + rid + ":bid=" + part);
            continue;
          }
          int by = 3 + blk.getTagsCount();
          if (by > sh) sh = by;
          nodes.add(node = node.insertLogic('#', x, y, blk, tags.split(",")));
          x+=3;
          break;
        }
      }
    }
    if (nodes.size() > 1) {
      nodes.add(node = node.insertNode('h', x, y));
    }
    rung.root = root;
    buildNodes(root, null, cells, objs, rid, readonly);
    return rung;
  }
  private static void moveNode(Table logic, Node node, int x, int y, int spanx) {
    if (!node.moved) logic.remove(node.x, node.y);
    node.x = x;
    node.y = y;
    for(int a=0;a<spanx;a++) {
      int xa = x + a;
      Component cmp = logic.remove(xa, y);
      if (cmp != null) {
        Node cmpNode = (Node)cmp.getProperty("node");
        cmpNode.moved = true;
      }
    }
    logic.add(node.comp, x, y, spanx, 1);
    node.moved = false;
    node.root.changed = true;
  }
  public static void buildNodes(NodeRoot root, Table logic, ArrayList<String[]> newCells, ArrayList<Node> newNodes, int rid, boolean readonly) {
    int x = 0;
    int y = 1;
    Node node = root.next;
    JFLog.log("buildNodes");
    boolean create;
    String style = readonly ? "readonly" : null;
    String field = readonly ? "label" : "textfield";
    int x2, y2;
    int sh = 1;  //segment height
    Node child;
    int childIdx;
    int cnt;
    while (node != null) {
      create = node.comp == null;
      char type;
      if (node.parent != null) {
        type = node.parent.type;
      } else {
        type = node.type;
      }
      switch (type) {
        case 'h':
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_h", null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y || node.moved) {
              moveNode(logic, node, x, y, 1);
            }
          }
          x++;
          break;
        case 'a':
        case 'c':
          x = node.upper.x;
          y = node.upper.y + 1;
          y2 = node.upper.y + sh;
          sh = 1;  //TODO : push sh
          cnt = node.childs.size();
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.x != x || child.y != y || child.moved) {
              moveNode(logic, child, x, y, 1);
            }
            y++;
          }
          while (y < y2) {
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_v", null));
            newNodes.add(node.addChild('v', x, y));
            y++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_" + node.type, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y || node.moved) {
              moveNode(logic, node, x, y, 1);
            }
          }
          x++;
          break;
        case 'b':
        case 'd':
          sh = 1;  //TODO : pop sh
          x2 = node.upper.x;
          if (node.lower != null) {
            if (node.lower.x > x2) {
              x2 = node.lower.x;
            }
          }
          while (x < x2) {
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_h", null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (x > x2) {
            JFLog.log("Error:buildNodes() failed to adjust upper node (see Node.adjustX())");
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_" + node.type, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y || node.moved) {
              moveNode(logic, node, x, y, 1);
            }
          }
          y--;
          cnt = node.childs.size();
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.x != x || child.y != y || child.moved) {
              moveNode(logic, child, x, y, 1);
            }
            y--;
          }
          while (y > node.upper.y) {
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_v", null));
            newNodes.add(node.addChildLower('v', x, y));
            y--;
          }
          if (node.type == 'd') {
            //move up to top
            child = node.upper;
            do {
              y = child.y;
              child = child.upper;
            } while (child != null);
          }
          x++;
          break;
        case 't':
          x2 = x;
          if (node.lower != null) {
            x2 = node.lower.x;
          }
          while (x < x2) {
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_h", null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_t", null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y || node.moved) {
              moveNode(logic, node, x, y, 1);
            }
          }
          x++;
          break;
        case '#': {
          //create cells for block
          //id,name,tags
          Logic blk = node.blk;
          int bh = node.getHeight();
          if (bh > sh) sh = bh;
          childIdx = 0;
          int tagIdx = 1;
          if (!blk.isBlock()) {
            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_h", null));
              newNodes.add(node.addChild('h', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            if (blk.getTagsCount() == 1) {
              //show tag
              x--;
              y++;
              if (create) {
                newCells.add(createCell(null, x, y, 3, 1, field, null, node.tags[tagIdx++], null, null, null, style));
                newNodes.add(node.addChild('T', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 3);
                }
              }
              y--;
              x++;
            }

            x++;

            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "w_h", null));
              newNodes.add(node.addChild('h', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }

            x--;

            if (create) {
              node.x = x;
              node.y = y;
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, blk.getImage(), null));
              newNodes.add(node);
            } else {
              if (node.x != x || node.y != y || node.moved) {
                moveNode(logic, node, x, y, 1);
              }
            }
            x += 2;

          } else {

            int bx = x;
            int by = y;
            //draw a box the size of the logic block
            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b7", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            for(int a=0;a<3;a++) {
              if (create) {
                newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b8", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x++;
            }

            //skip b9 (do it last)
            x -= 4;
            y++;

            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b4", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            if (create) {
              newCells.add(createCell(null, x, y, 3, 1, "label", null, blk.getName(), null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 3);
              }
            }
            x += 3;

            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b6", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x -= 4; y++;

            //output tags
            int tagcnt = blk.getTagsCount();
            JFLog.log("tagcnt=" + tagcnt);
            for(int a=0;a<tagcnt;a++) {
              if (create) {
                newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b4", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x++;

              if (create) {
                newCells.add(createCell(null, x, y, 3, 1, field, null, node.tags[tagIdx++], null, null, null, style));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 3);
                }
              }
              x += 3;

              if (create) {
                newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b6", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x -= 4; y++;
            }

            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b1", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            for(int a=0;a<3;a++) {
              if (create) {
                newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b2", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
                child = child.next;
              }
              x++;
            }

            if (create) {
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b3", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
                child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            y = by;

            if (create) {
              node.x = x;
              node.y = y;
              newCells.add(createCell(null, x, y, 1, 1, "image", null, null, null, null, "b9", null));
              newNodes.add(node);
            } else {
              if (node.x != x || node.y != y || node.moved) {
                moveNode(logic, node, x, y, 1);
              }
            }
            x++;
          }
          break;
        }
        default: {
          JFLog.log("Error:Unknown node type:" + node.type + ":" + node);
          break;
        }
      }
      node = node.next;
    }
  }

  public static void layoutNodes(NodeRoot root, Table logic) {
    if (logic == null) {
      JFLog.log("Error:unable to find logic table");
      return;
    }
    if (root == null) {
      JFLog.log("Error:unable to find root node");
      return;
    }
    do {
      root.changed = false;
      ArrayList<String[]> newCells = new ArrayList<String[]>();
      ArrayList<Node> newNodes = new ArrayList<Node>();
      buildNodes(root, logic, newCells, newNodes, root.rid, false);
      buildTable(logic, null, newCells.toArray(new String[newCells.size()][]), logic.getClient(), -1, -1, newNodes.toArray(new Node[newNodes.size()]));
    } while (root.changed);
    //calc max table size
    Node node = root;
    int x = 0;
    int y = 0;
    int mx = 1;
    int my = 1;
    int cnt = 0;
    while (node != null) {
      cnt++;
      x = node.x;
      y = node.y;
      if (x > mx) mx = x;
      if (y > my) my = y;
      node = node.next;
    }
    logic.setTableSize(mx, my);
  }
}
