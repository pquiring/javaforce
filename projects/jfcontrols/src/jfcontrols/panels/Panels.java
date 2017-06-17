package jfcontrols.panels;

/** Panels
 *
 * @author pquiring
 */

import jfcontrols.logic.Logic;
import java.util.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.controls.*;
import jfcontrols.functions.FunctionService;

import jfcontrols.sql.*;
import jfcontrols.tags.*;
import jfcontrols.images.*;

public class Panels {
  public static int cellWidth = 32;
  public static int cellHeight = 32;
  public static PopupPanel getPopupPanel(WebUIClient client, String title, String name) {
    PopupPanel panel = (PopupPanel)buildPanel(createPopupPanel(title, name), name, client);
    panel.setModal(true);
    return panel;
  }
  private static PopupPanel createPopupPanel(String title, String name) {
    PopupPanel pp = new PopupPanel(title);
    pp.setTitleBarSize(cellHeight);
    pp.setName(name);
    return pp;
  }
  //...
  public static Panel getPanel(String pname, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    context.clear();
    return buildPanel(new Panel(), pname, client);
  }
  public static Panel buildPanel(Panel panel, String pname, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String pid = sql.select1value("select id from panels where name=" + SQL.quote(pname));
    if (pid == null) {
      JFLog.log("Error:Unable to find panel:" + pname);
      return null;
    }
    String display = sql.select1value("select display from panels where name=" + SQL.quote(pname));
    String popup = sql.select1value("select popup from panels where id=" + pid);
    String cells[][] = sql.select("select x,y,w,h,comp,name,text,tag,func,arg,style,events from cells where pid=" + pid);
    Table table = new Table(cellWidth,cellHeight,1,1);
    panel.add(table);
    buildTable(table, panel, cells, client, -1, -1, null);
    if (popup.equals("true")) return panel;
    //add top components
    Button menu = getButton(new String[] {null, null, null, null, "button", null, "!image:menu", null, "showMenu", null, null});
    menu.setProperty("func", "showMenu");
    setCellSize(menu, new Rectangle(0,0,1,1));
    table.add(menu, 0, 0);
    Label alarms = getLabel(new String[] {null, null, null, null, "label", null, "0", null, "setPanel", "jfc_alarms", null});
    alarms.setProperty("func", "setPanel");
    alarms.setProperty("arg", "jfc_alarms");
    alarms.setBorder(true);
    setCellSize(alarms, new Rectangle(1,0,1,1));
    table.add(alarms, 1, 0);
    Label title = getLabel(new String[] {null, null, null, null, "label", "jfc_title", display, null, null, null, null});
    title.setName("jfc_title");
    setCellSize(title, new Rectangle(2,0,16,1));
    table.add(title, 2, 0, 16, 1);
    TagAddr ta = new TagAddr();
    ta.name = "alarms";
    TagBase tag = context.getTag(ta);
    context.addListener(tag, alarms, (_tag, _oldValue, _newValue, _cmp) -> {
      updateAlarmCount(alarms, client);
    });
    updateAlarmCount(alarms, client);
    panel.add(getPopupPanel(client, "Login", "jfc_login"));
    panel.add(getPopupPanel(client, "Menu", "jfc_menu"));
    panel.add(getPopupPanel(client, "Confirm", "jfc_confirm"));
    panel.add(getPopupPanel(client, "Error", "jfc_error"));
    panel.add(getPopupPanel(client, "Error", "jfc_error_textarea"));
    if (pname.equals("jfc_panel_editor")) {
      panel.add(getPopupPanel(client, "Properties", "jfc_panel_props"));
    }
    return panel;
  }
  //x,y,w,h,comp,name,text,tag,func,arg,style,events
  private final static int X = 0;
  private final static int Y = 1;
  private final static int W = 2;
  private final static int H = 3;
  private final static int COMP = 4;
  private final static int NAME = 5;
  private final static int TEXT = 6;
  private final static int TAG = 7;
  private final static int FUNC = 8;
  private final static int ARG = 9;
  private final static int STYLE = 10;
  private final static int EVENTS = 11;
  public static Table buildTable(Table table, Container container, String cells[][], WebUIClient client, int ix, int iy, Node nodes[]) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    int mx = table.getColumns();
    if (ix != -1) mx = ix;
    int my = table.getRows();
    if (iy != -1) my = iy;
    Component cs[] = new Component[cells.length];
    Rectangle rs[] = new Rectangle[cells.length];
    for(int a=0;a<cells.length;a++) {
      Rectangle r = new Rectangle();
      rs[a] = r;
      r.x = Integer.valueOf(cells[a][X]);
      r.y = Integer.valueOf(cells[a][Y]);
      r.width = Integer.valueOf(cells[a][W]);
      r.height = Integer.valueOf(cells[a][H]);
      String compType = cells[a][COMP];
      String tagName = cells[a][TAG];
      if (tagName != null && !tagName.startsWith("jfc_") && tagName.length() > 0) {
        TagBase tag = context.getTag(tagName);
        if (tag == null) {
          JFLog.log("Error:Tag not found:" + tagName);
        } else {
          cells[a][TEXT] = tag.getValue();
        }
      }
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
      String cellTag = cells[a][TAG];
      if (cellTag != null) {
        c.setProperty("tag", cellTag);
        if (!cellTag.startsWith("jfc_")) {
          TagAddr ta = context.decode(cellTag);
          context.addListener((MonitoredTag)context.getTag(ta), c, (tag, oldValue, newValue, cmp) -> {
            if (cmp instanceof Label) {
              Label lbl = (Label)cmp;
              lbl.setText(newValue);
            }
          });
        }
      }
      c.setProperty("func", cells[a][FUNC]);
      c.setProperty("arg", cells[a][ARG]);
      c.setProperty("events", cells[a][EVENTS]);
      c.setName(cells[a][NAME]);
      if (nodes != null && nodes.length > a) {
        c.setProperty("node", nodes[a]);
        nodes[a].comp = c;
        c.addClickListener((me, comp) -> {
//          WebUIClient client = comp.getClient();
          Component focus = (Component)client.getProperty("focus");
          if (focus != null) {
            focus.setBorderColor("#eee");
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
            c.setDisabled(true);
          } else if (styles[b].equals("disabled")) {
            c.setDisabled(true);
          } else if (styles[b].equals("border")) {
            c.setBorder(true);
          } else if (styles[b].equals("smallfont")) {
            c.setStyle("font-size", "8pt");
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
      Component cmp = cs[a];
      if (cmp instanceof AutoScrollPanel) {
        container.add(cs[a]);
      } else {
        if (rs[a].width == 1 && rs[a].height == 1)
          table.add(cmp, rs[a].x, rs[a].y);
        else
          table.add(cmp, rs[a].x, rs[a].y, rs[a].width, rs[a].height);
      }
    }
    return table;
  }
  public static Component setCellSize(Component c, Rectangle r) {
    if (r.width > 0 && r.height > 0) {
      c.setSize(cellWidth * r.width, cellHeight * r.height);
    }
    c.setProperty("rect", r);
    return c;
  }
  public static Component getCell(String name, Container container, String v[], Rectangle r, WebUIClient client) {
    switch (name) {
      case "label": return getLabel(v);
      case "button": return getButton(v);
      case "link": return getLink(v);
      case "textfield": return getTextField(v, client);
      case "dual": return getDual(v, client);
      case "textarea": return getTextArea(v, client);
      case "combobox": return getComboBox(v, client);
      case "checkbox": return getCheckBox(v, client);
      case "table": return getTable(v, container, r, client);
      case "overlay": return getOverlay(v);
      case "image": return getImage(v);
      case "autoscroll": return getAutoScroll(v, container, client);
      default: JFLog.log("Unknown component:" + name); break;
    }
    return null;
  }
  private static Label getLabel(String v[]) {
    Label lbl = new Label(v[TEXT]);
    lbl.addClickListener((me, c) -> {
      Events.click(c);
    });
    lbl.addMouseDownListener((c) -> {
      Events.press(c);
    });
    lbl.addMouseUpListener((c) -> {
      Events.release(c);
    });
    return lbl;
  }
  private static Button getButton(String v[]) {
    String text = v[TEXT];
    Button b = null;
    if (text.startsWith("!image:")) {
      b = new Button(Images.getImage(text.substring(7)));
    } else {
      b = new Button(v[TEXT]);
    }
    b.addClickListener((me, c) -> {
      Events.click(c);
    });
    b.addMouseDownListener((c) -> {
      Events.press(c);
    });
    b.addMouseUpListener((c) -> {
      Events.release(c);
    });
    return b;
  }
  private static Button getLink(String v[]) {
    String text = v[TEXT];
    Button b = null;
    if (text.startsWith("!image:")) {
      b = new Button(Images.getImage(text.substring(7)));
    } else {
      b = new Button(v[TEXT]);
    }
    b.setURL("http://jfcontrols.sourceforge.net/help_" + v[ARG] + ".php");
    return b;
  }
  private static Component getDual(String v[], WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    Table table = new Table(cellWidth, cellHeight/2, 3, 2);
    TagBase tag = context.getTag(context.decode(v[TAG]));
    Label comment = new Label(tag.getComment());
    comment.setName("tc_" + context.debug_tv_idx);
    table.add(comment, 0, 0, 3, 1);
    Label value = new Label("");
    value.setName("tv_" + context.debug_tv_idx);
    context.debug_tv_idx++;
    table.add(value, 0, 1, 3, 1);
    return table;
  }
  private static TextField getTextField(String v[], WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tag = v[TAG];
    String text = v[TEXT];
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        if (table.equals("config")) {
          id = "\'" + id + "\'";
        }
        text = sql.select1value("select " + col + " from " + table + " where id=" + id);
      } else {
        text = context.read(tag);
      }
    }
    if (text == null) text = "";
    TextField b = new TextField(text);
    b.addChangedListener((c) -> {
      Events.edit((TextField)c);
    });
    return b;
  }
  private static TextArea getTextArea(String v[], WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tag = v[TAG];
    String text = v[TEXT];
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        if (table.equals("config")) {
          id = "\'" + id + "\'";
        }
        text = sql.select1value("select " + col + " from " + table + " where id=" + id);
      } else {
        text = context.read(tag);
      }
    }
    if (text == null) text = "";
    TextArea ta = new TextArea(text);
    ta.addChangedListener((c) -> {
      Events.edit((TextArea)c);
    });
    return ta;
  }
  private static ComboBox getComboBox(String v[], WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    ComboBox cb = new ComboBox();
    String name = v[NAME];
    String tag = v[TAG];
    String arg = v[ARG];
    String value = v[TEXT];
    String pairs[][];
    if (arg.equals("jfc_function")) {
      pairs = sql.select("select id, name from funcs");
    } else if (arg.equals("jfc_logic_groups")) {
      pairs = sql.select("select gid,gid from logics group by gid order by gid");
      value = "bit";
    } else if (arg.equals("jfc_tag_type_udt")) {
      arg = "jfc_tag_type";
      String lid = sql.select1value("select id from lists where name=" + SQL.quote(arg));
      String basic[][] = sql.select("select value, text from listdata where lid=" + lid);
      String udts[][] = sql.select("select uid, name from udts");
      pairs = new String[basic.length + udts.length][];
      int pos = 0;
      for(int a=0;a<basic.length;a++) {
        pairs[pos++] = basic[a];
      }
      for(int a=0;a<udts.length;a++) {
        pairs[pos++] = udts[a];
      }
    } else {
      String lid = sql.select1value("select id from lists where name=" + SQL.quote(arg));
      pairs = sql.select("select value, text from listdata where lid=" + lid);
    }
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_type_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        if (table.equals("config")) {
          id = "\'" + id + "\'";
        }
        value = sql.select1value("select " + col + " from " + table + " where id=" + id);
      } else {
        value = context.read(tag);
      }
    }
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
      cb.addChangedListener((c) -> {
        Events.changed((ComboBox)c);
      });
    }
    if (name != null) {
      switch (name) {
        case "group_type":
          cb.addChangedListener((c) -> {
            ComboBox groups = (ComboBox)c;
            TabPanel tabs = (TabPanel)client.getProperty("groups");
            tabs.setTabIndex(groups.getSelectedIndex());
          });
          break;
        case "jfc_function":
          cb.addChangedListener((c) -> {
            ComboBox funcs = (ComboBox)c;
            Node node = (Node)c.getProperty("node");
            node.parent.tags[1] = funcs.getSelectedValue();
          });
          break;
      }
    }
    return cb;
  }
  private static CheckBox getCheckBox(String v[], WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tag = v[TAG];
    String text = v[TEXT];
    String value = "0";
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        if (table.equals("config")) {
          id = "\'" + id + "\'";
        }
        value = sql.select1value("select " + col + " from " + table + " where id=" + id);
        if (value != null) {
          value = value.equals("false") ? "0" : "1";
        }
      } else {
        value = context.read(tag);
      }
    }
    if (text == null) text = "???";
    if (value == null) value = "0";
    CheckBox cb = new CheckBox(text);
    if (!value.equals("0")) cb.setSelected(true);
    cb.addChangedListener((cmpnt) -> {
      Events.changed(cb);
    });
    return cb;
  }
  private static Image getImage(String v[]) {
    Image img = new Image(Images.getImage(v[ARG]));
    img.addClickListener((me, c) -> {
      Events.click(c);
    });
    img.addMouseDownListener((c) -> {
      Events.press(c);
    });
    img.addMouseUpListener((c) -> {
      Events.release(c);
    });
    return img;
  }
  private static String[] createCell(int x, int y, int w, int h, String comp, String name, String text, String tag, String func, String arg, String style /*, String events */) {
    String cell[] = new String[12];
    cell[0] = Integer.toString(x);
    cell[1] = Integer.toString(y);
    cell[2] = Integer.toString(w);
    cell[3] = Integer.toString(h);
    cell[4] = comp;
    cell[5] = name;
    cell[6] = text;
    cell[7] = tag;
    cell[8] = func;
    cell[9] = arg;
    cell[10] = style;
    //cell[11] = events;
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
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String name = v[NAME];
    String arg = v[ARG];
    ArrayList<String[]> cells = new ArrayList<String[]>();
    ArrayList<Node> nodes = new ArrayList<Node>();
    Table table;
    switch (name) {
      case "jfc_ctrls" : {
        String data[][] = sql.select("select id,cid,ip,type from ctrls");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          String style = data[a][1].equals("0") ? "disabled" : null;
          cells.add(createCell(0, a, 1, 1, "textfield", null, data[a][1], "jfc_ctrls_cid_int_" + data[a][0], null, null, style));
          cells.add(createCell(1, a, 3, 1, "textfield", null, data[a][2], "jfc_ctrls_ip_str_" + data[a][0], null, null, style));
          cells.add(createCell(4, a, 2, 1, "combobox", null, null, "jfc_ctrls_type_int_" + data[a][0], null, "jfc_ctrl_type", style));
          cells.add(createCell(6, a, 2, 1, "combobox", null, null, "jfc_ctrls_speed_int_" + data[a][0], null, "jfc_ctrl_speed", style));
          cells.add(createCell(9, a, 2, 1, "button", null, "Tags", null, "jfc_ctrl_tags", data[a][1], null));
          if (style == null) {
            cells.add(createCell(12, a, 2, 1, "button", null, "Delete", null, "jfc_ctrl_delete", data[a][0], null));
          }
        }
        break;
      }
      case "jfc_tags": {
        String cid = (String)client.getProperty("ctrl");
        String tag_types;
        String tag_type;
        if (cid.equals("0")) {
          tag_types = "jfc_tag_type_udt";
          tag_type = "tagid";
        } else {
          //TODO : support remote UDT if controller = JFC
          tag_types = "jfc_tag_type";
          tag_type = "tag";
        }
        String data[][] = sql.select("select id,cid,name,type,builtin from tags where cid=" + cid);
        if (data == null) data = new String[0][0];
        String style;
        for(int a=0;a<data.length;a++) {
          if (data[a][4].equals("true")) {
            style = "readonly";
          } else {
            style = null;
          }
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_tags_name_" + tag_type + "_" + data[a][0], null, null, style));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_tags_type_int_" + data[a][0], null, tag_types, style));
          cells.add(createCell(10, a, 3, 1, "checkbox", null, "Unsigned", "jfc_tags_unsigned_boolean_" + data[a][0], null, null, style));
          cells.add(createCell(14, a, 6, 1, "textfield", null, null, "jfc_tags_comment_str_" + data[a][0], null, null, style));
          if (cid.equals("0")) {
            cells.add(createCell(21, a, 3, 1, "checkbox", null, "Array", "jfc_tags_array_boolean_" + data[a][0], null, null, style));
          }
          cells.add(createCell(25, a, 2, 1, "button", null, "Delete", null, "jfc_tags_delete", data[a][0], style));
        }
        break;
      }
      case "jfc_udts": {
        String data[][] = sql.select("select id,uid,name from udts where uid >= " + IDs.uid_user);
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udts_name_tagid_" + data[a][0], null, null, null));
          cells.add(createCell(8, a, 2, 1, "button", null, "Edit", null, "jfc_udts_edit", data[a][1], null));
          cells.add(createCell(11, a, 2, 1, "button", null, "Delete", null, "jfc_udts_delete", data[a][0], null));
        }
        break;
      }
      case "jfc_udt_editor": {
        String uid = (String)client.getProperty("udt");
        String data[][] = sql.select("select id,uid,name,type,mid,builtin from udtmems where uid=" + uid);
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udtmems_name_tagid_" + data[a][0], null, null, null));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_udtmems_type_int_" + data[a][0], null, "jfc_tag_type", null));
          cells.add(createCell(10, a, 3, 1, "checkbox", null, "Unsigned", "jfc_udtmems_unsigned_boolean_" + data[a][0], null, null, null));
          cells.add(createCell(14, a, 3, 1, "checkbox", null, "Array", "jfc_udtmems_array_boolean_" + data[a][0], null, null, null));
          cells.add(createCell(18, a, 6, 1, "textfield", null, null, "jfc_udtmems_comment_str_" + data[a][0], null, null, null));
          if (data[a][5].equals("0")) {
            cells.add(createCell(25, a, 2, 1, "button", null, "Delete", null, "jfc_udts_editor_delete", data[a][0], null));
          }
        }
        break;
      }
      case "jfc_sdts": {
        String data[][] = sql.select("select id,uid,name from udts where uid < " + IDs.uid_user);
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udts_name_tagid_" + data[a][0], null, null, "readonly"));
          cells.add(createCell(8, a, 2, 1, "button", null, "View", null, "jfc_sdts_edit", data[a][1], null));
        }
        break;
      }
      case "jfc_sdt_editor": {
        String uid = (String)client.getProperty("udt");
        String data[][] = sql.select("select id,uid,name,type,mid from udtmems where uid=" + uid);
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udtmems_name_tagid_" + data[a][0], null, null, "readonly"));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_udtmems_type_int_" + data[a][0], null, "jfc_tag_type", "readonly"));
          cells.add(createCell(10, a, 3, 1, "checkbox", null, "Unsigned", "jfc_udtmems_unsigned_boolean_" + data[a][0], null, null, "readonly"));
          cells.add(createCell(14, a, 3, 1, "checkbox", null, "Array", "jfc_udtmems_array_boolean_" + data[a][0], null, null, "readonly"));
        }
        break;
      }
      case "jfc_panels": {
        String data[][] = sql.select("select id,name from panels where builtin=false");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          String style = data[a][1].equals("main") ? "disabled" : null;
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_panels_display_str_" + data[a][0], null, null, style));
          cells.add(createCell(7, a, 2, 1, "button", null, "Edit", null, "jfc_panels_edit", data[a][0], null));
          if (style == null) {
            cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_panels_delete", data[a][0], null));
          }
        }
        break;
      }
      case "jfc_panel_editor": {
        String pid = (String)client.getProperty("panel");
        String data[][] = sql.select("select x,y,w,h,comp,name,text,tag,func,arg,style,events from cells where pid=" + pid);
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
              cells.add(createCell(x, y, 1, 1, "overlay", null, null, null, null, null, null));
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
          String fid = data[a][0];
          String funcname = data[a][1];
          String style = funcname.equals("main") || funcname.equals("init") ? "disabled" : null;
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_funcs_name_tagid_" + fid, null, null, style));
          cells.add(createCell(7, a, 2, 1, "button", null, "Edit", null, "jfc_funcs_edit", fid, null));
          if (style == null) {
            cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_funcs_delete", fid, null));
          }
        }
        break;
      }
      case "jfc_rung_args": {
        cells.add(createCell(0, 0, 1, 1, "button", null, "UP", null, "jfc_rung_args_up", null, null));
        cells.add(createCell(0, 1, 1, 1, "button", null, "+", null, "jfc_rung_args_add", null, null));
        cells.add(createCell(0, 2, 1, 1, "button", null, "-", null, "jfc_rung_args_del", null, null));
        cells.add(createCell(0, 3, 1, 1, "button", null, "DN", null, "jfc_rung_args_dn", null, null));
        for(int a=0;a<4;a++) {
          cells.add(createCell(1, a, 6, 1, "textfield", null, null, null, null, null, null));
          cells.add(createCell(7, a, 3, 1, "combobox", null, null, null, null, "jfc_tag_type_udt", null));
        }
        break;
      }
      case "jfc_logic_groups": {
        TabPanel tabs = new TabPanel();
        tabs.setTabsVisible(false);
        tabs.setBorders(false);
        String groups[] = sql.select1col("select gid from logics group by gid order by gid");
        int idx = -1;
        for(int a=0;a<groups.length;a++) {
          tabs.add(wrapPanel(getTable(createCell(r.x, r.y, r.width, r.height, "table", "jfc_logics", null, null, null, groups[a], null), null, new Rectangle(r), client)), "");
          if (groups[a].equals("bit")) idx = a;
        }
        if (idx != -1) tabs.setTabIndex(idx);
        setCellSize(tabs, r);
        client.setProperty("groups", tabs);
        return tabs;
      }
      case "jfc_logics": {
        String items[][] = sql.select("select name,shortname from logics where gid=" + SQL.quote(arg));
        for(int a=0;a<items.length;a++) {
          String desc = items[a][0];
          String shortname = items[a][1];
          if (shortname != null) {
            desc = shortname;
          }
          String style = "border";
          if (Images.getImage(desc) != null) {
            desc = "!image:" + desc;
          } else {
            if (desc.length() > 3) {
              style += ";smallfont";
            }
            desc = desc.replaceAll("_", "<br/>");
          }
          cells.add(createCell(a, 0, 1, 1, "button", items[a][0], desc, null, "jfc_rung_editor_add", null, style));
        }
        break;
      }
      case "jfc_rung_viewer": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + arg);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.rungs.add(buildRung(data, cells, nodes, client, true, fid));
        break;
      }
      case "jfc_rung_viewer_end": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        cells.add(createCell(0, 0, 5, 1, "label", null, "End of Function", null, null, null, null));
        nodes.add(new NodeRoot(fid, -1));
        break;
      }
      case "jfc_config_iocomments": {
        String data[][] = sql.select("select id,mid,idx,comment from iocomments order by mid,idx");
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          int mid = Integer.valueOf(data[a][1]);
          String idx = data[a][2];
          String comment = data[a][3];
          String io = null;
          switch (mid) {
            case IDs.io_mid_di: io = "di"; break;
            case IDs.io_mid_do: io = "do"; break;
            case IDs.io_mid_ai: io = "ai"; break;
            case IDs.io_mid_ao: io = "ao"; break;
          }
          cells.add(createCell(0, a, 4, 1, "label", null, "io." + io + "[" + idx + "]", null, null, null, null));
          cells.add(createCell(4, a, 6, 1, "textfield", null, comment, "jfc_iocomments_comment_str_" + data[a][0], null, null, null));
        }
        break;
      }
      case "jfc_alarm_editor": {
        String tid = sql.select1value("select id from tags where name='alarms'");
        String data[][] = sql.select("select id,tid,idx,value from tagvalues where mid=" + IDs.alarm_mid_name + " and tid=" + tid);
        if (data == null) data = new String[0][0];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 2, 1, "label", null, data[a][2], null, null, null, null));
          cells.add(createCell(2, a, 6, 1, "textfield", null, null, "jfc_tagvalues_value_str_" + data[a][0], null, null, null));
          cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_alarms_editor_delete", data[a][2], null));
        }
        break;
      }
      case "jfc_alarm": {
        String tid = sql.select1value("select id from tags where name='alarms'");
        String alarmname = sql.select1value("select value from tagvalues where mid=" + IDs.alarm_mid_name + " and tid=" + tid + " and idx=" + arg);
        String alarmack = sql.select1value("select value from tagvalues where mid=" + IDs.alarm_mid_ack + " and tid=" + tid + " and idx=" + arg);
        if (alarmack == null) alarmack = "0";
        cells.add(createCell(2, 0, 2, 1, "label", null, alarmack.equals("1") ? "X" : "", null, null, null, null));
        cells.add(createCell(4, 0, 10, 1, "label", null, arg + ":" + alarmname, null, null, null, null));
        break;
      }
      case "jfc_alarm_history": {
        String data[][] = sql.select("select id,idx,when from alarmhistory where id=" + arg);
        if (data == null) data = new String[0][];
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(2, a, 4, 1, "label", null, data[a][2], null, null, null, null));  //when
          cells.add(createCell(6, a, 10, 1, "label", null, arg + ":" + data[a][1], null, null, null, null));  //name
        }
        break;
      }
      default: {
        JFLog.log("Unknown table:" + name);
      }
    }
    table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new String[cells.size()][]), client, -1, -1, nodes.toArray(new Node[nodes.size()]));
    r.width = table.getColumns();
    r.height = table.getRows();
    switch (name) {
      case "jfc_rung_viewer": {
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.rungs.get(rungs.rungs.size() - 1).table = table;
        break;
      }
    }
    return table;
  }
  private static Component getAutoScroll(String v[], Container container, WebUIClient client) {
    //auto scroll components are placed below the main table
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String name = v[NAME];
    AutoScrollPanel panel = new AutoScrollPanel();
    JFLog.log("client.height=" + client.getHeight());
    panel.setHeight(client.getHeight() - (cellHeight * 2));
    client.addResizedListener((cmp, width, height) -> {
      panel.setHeight(client.getHeight() - (cellHeight * 2));
    });
    switch (name) {
      case "jfc_rungs_viewer": {
        String fid = (String)client.getProperty("func");
        String data[][] = sql.select("select rid,logic,comment from rungs where fid=" + fid + " order by rid");
        client.setProperty("rungs", new Rungs());
        context.debug_en_idx = 0;
        context.debug_tv_idx = 0;
        for(int rung=0;rung<data.length;rung++) {
          ArrayList<String[]> cells = new ArrayList<String[]>();
          cells.add(createCell(0, 0, 1, 1, "table", "jfc_rung_viewer", null, null, null, data[rung][0], null));
          Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new String[cells.size()][]), client, -1, -1, null);
          panel.add(table);
        }
        ArrayList<String[]> cells = new ArrayList<String[]>();
        cells.add(createCell(0, 0, 1, 1, "table", "jfc_rung_viewer_end", null, null, null, null, null));
        Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new String[cells.size()][]), client, -1, -1, null);
        panel.add(table);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.panel = panel;
        break;
      }
      case "jfc_rung_editor": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        int rid = Integer.valueOf((String)client.getProperty("rung"));
        ArrayList<String[]> cells = new ArrayList<String[]>();
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + rid);
        ArrayList<Node> nodes = new ArrayList<Node>();
        Rung rung = buildRung(data, cells, nodes, client, false, fid);
        client.setProperty("rungObj", rung);
        Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new String[cells.size()][]), client, -1, -1, nodes.toArray(new Node[nodes.size()]));
        table.setName(name + "_table");
        panel.add(table);
        break;
      }
      case "jfc_alarms": {
        //view current alarms
        context.alarms.clear();
        updateAlarms(panel, client);
        TagAddr ta = new TagAddr();
        ta.name = "alarms";
        TagBase tag = context.getTag(ta);
        context.addListener(tag, panel, (_tag, _oldValue, _newValue, _cmp) -> {
          updateAlarms(panel, panel.getClient());
        });
        break;
      }
      case "jfc_alarm_history": {
        //view alarm history
        context.lastAlarmID = 0;
        updateAlarmHistory(panel, client);
        TagAddr ta = new TagAddr();
        ta.name = "alarms";
        TagBase tag = context.getTag(ta);
        context.addListener(tag, panel, (_tag, _oldValue, _newValue, _cmp) -> {
          updateAlarmHistory(panel, panel.getClient());
        });
        client.setProperty("history", panel);
        break;
      }
    }
    return panel;
  }
  private static void updateAlarmCount(Label label, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tid = sql.select1value("select id from tags where name='alarms'");
    String count = sql.select1value("select count(idx) from tagvalues where tid=" + tid + " and mid=1 and value='1'");
    label.setText(count);
    if (count.equals("0")) {
      label.setBackColor("#fff");
    } else {
      label.setBackColor("#f00");
    }
  }
  private static void updateAlarms(Panel panel, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tid = sql.select1value("select id from tags where name='alarms'");
    String data[][] = sql.select("select id,idx,value,mid from tagvalues where mid=1 and tid=" + tid + " order by idx,mid");
    if (data == null) data = new String[0][0];
    for(int a=0;a<data.length;a++) {
      String idx = data[a][1];
      if (data[a][2].equals("0")) {
        //not active
        if (context.alarms.containsKey(idx)) {
          //remove inactived alarm
          Table table = (Table)context.alarms.remove(idx);
          panel.remove(table);
        }
        continue;
      }
      if (context.alarms.containsKey(idx)) continue;
      ArrayList<String[]> cells = new ArrayList<String[]>();
      cells.add(createCell(0, 0, 1, 1, "table", "jfc_alarm", null, null, null, data[a][1], null));
      Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, -1, -1, null);
      panel.add(table);
      context.alarms.put(idx, table);
    }
  }
  private static void updateAlarmHistory(Panel panel, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    Calendar cal = Calendar.getInstance();
    int year = cal.get(Calendar.YEAR);
    int month = cal.get(Calendar.MONTH + 1);
    int day = cal.get(Calendar.DAY_OF_MONTH);
    String today = String.format("%04d/%02d/%02d%%", year, month, day);
    String data[][] = sql.select("select id,idx,when from alarmhistory where when like '" + today + "' order by when");
    if (data == null) data = new String[0][0];
    for(int a=0;a<data.length;a++) {
      int id = Integer.valueOf(data[a][0]);
      if (id < context.lastAlarmID) continue;
      context.lastAlarmID = id;
      ArrayList<String[]> cells = new ArrayList<String[]>();
      cells.add(createCell(0, 0, 1, 1, "table", "jfc_alarm_history", null, null, null, data[a][0], null));
      Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), panel.getClient(), -1, -1, null);
      panel.add(table);
    }
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
  public static Component getOverlay(int x,int y) {
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
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
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
    String pid = (String)client.getProperty("panel");
    sql.execute("update cells set x=" + (fr.x + deltax) + ",y=" + (fr.y + deltay) + " where x=" + fr.x + " and y=" + fr.y + " and pid=" + pid);
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
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
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
    String pid = (String)client.getProperty("panel");
    sql.execute("update cells set w=" + (fr.width + deltax) + ",h=" + (fr.height + deltay) + " where x=" + fr.x + " and y=" + fr.y + " and pid=" + pid);
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
  public static Rung buildRung(String data[], ArrayList<String[]> cells, ArrayList<Node> objs, WebUIClient client, boolean readonly, int fid) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    int x = 0;
    int y = 0;
    int rid = Integer.valueOf(data[0]);
    Rung rung = new Rung();
    String logic = data[1];
    String comment = data[2];
    String parts[] = logic.split("[|]");
    String blocks[][] = sql.select("select bid,name,tags from blocks where fid=" + fid + " and rid=" + rid);
    ArrayList<Node> nodes = new ArrayList<Node>();
    NodeRoot root = new NodeRoot(fid, rid);
    JFLog.log("new NodeRoot() " + root);

    //add rung title / comment
    String style = readonly ? "readonly" : null;
    String field = readonly ? "label" : "textfield";
    cells.add(createCell(x, y, 3, 1, "label", null, "Rung " + (rid+1), null, null, null, null));
    objs.add(root);
    x += 3;
    cells.add(createCell(x, y, 12, 1, field, "comment" + rid, comment, null, null, null, style));
    objs.add(root);
    x = 0;
    y++;

    Node node = root;
    for(int p=0;p<parts.length;p++) {
      String part = parts[p];
      if (part.length() == 0) continue;
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
          y = upper.getSegmentMaxY(node) + 1;
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
          y = upper.getSegmentMaxY(node) + 1;
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
    buildNodes(root, null, cells, objs, client, rid, readonly);
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
  public static void buildNodes(NodeRoot root, Table logic, ArrayList<String[]> newCells, ArrayList<Node> newNodes, WebUIClient client, int rid, boolean readonly) {
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    int x = 0;
    int y = 1;
    Node node = root.next;
    JFLog.log("buildNodes:" + root);
    boolean create;
    String style = readonly ? "readonly" : null;
    String textfield = readonly ? "label" : "textfield";
    String combobox = readonly ? "label" : "combobox";
    int x2, y2;
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
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_h", null));
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
          y2 = node.upper.getSegmentMaxY(node) + 1;
          cnt = node.childs.size();
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.x != x || child.y != y || child.moved) {
              moveNode(logic, child, x, y, 1);
            }
            y++;
          }
          while (y < y2) {
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_v", null));
            newNodes.add(node.addChild('v', x, y));
            y++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_" + node.type, null));
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
          x2 = node.upper.x;
          if (node.lower != null) {
            if (node.lower.x > x2) {
              x2 = node.lower.x;
            }
          }
          while (x < x2) {
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_h", null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (x > x2) {
            JFLog.log("Error:buildNodes() failed to adjust upper node (see Node.adjustX())");
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_" + node.type, null));
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
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_v", null));
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
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_h", null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "w_t", null));
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
          childIdx = 0;
          int tagIdx = 1;
          if (!blk.isBlock()) {
            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", "en_0_" + context.debug_en_idx, null, null, null, "w_h", null));
              newNodes.add(node.addChild('h', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            if (blk.getTagsCount() == 1) {
              String tag = node.tags[tagIdx++].substring(1);
              //show tag
              x--;
              y++;
              if (create) {
                newCells.add(createCell(x, y, 3, 1, textfield, "tag_" + context.debug_tv_idx, tag, null, null, null, style));
                newNodes.add(node.addChild('T', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 3);
                }
              }

              //show tag comment/value
              y++;
              if (create) {
                newCells.add(createCell(x, y, 3, 1, "dual", null, tag, null, null, null, style));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 3);
                }
              }
              tagIdx++;

              y -= 2;
              x++;
            }

            x++;

            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", "en_1_" + context.debug_en_idx++, null, null, null, "w_h", null));
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
              newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, blk.getImage(), null));
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
              newCells.add(createCell(x, y, 1, 1, "image", "en_0_" + context.debug_en_idx, null, null, null, "b7", null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            if (create) {
              newCells.add(createCell(x, y, 3, 1, "label", null, blk.getDesc(), null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y || child.moved) {
                moveNode(logic, child, x, y, 3);
              }
            }

            for(int a=0;a<3;a++) {
              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b8", null));
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

            //output tags
            int tagcnt = blk.getTagsCount();
            for(int a=0;a<tagcnt;a++) {
              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b4", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              y++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b4", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              y--;

              if (create) {
                String name = blk.getTagName(a + 1);
                if (name == null) name = "";
                newCells.add(createCell(x, y, 1, 1, "label", null, name, null, null, null, name.length() > 3 ? "smallfont" : null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x++;

              String tag = node.tags[tagIdx].substring(1);
              if (create) {
                if (blk.getTagType(a) == TagType.function) {
                  if (readonly) {
                    tag = sql.select1value("select name from funcs where id=" + tag);
                  }
                  newCells.add(createCell(x, y, 3, 1, combobox, "jfc_function", tag, null, null, "jfc_function", style));
                  newNodes.add(node.addChild('C', x, y));
                } else {
                  newCells.add(createCell(x, y, 3, 1, textfield, "tag_" + context.debug_tv_idx, tag, null, null, null, style));
                  newNodes.add(node.addChild('T', x, y));
                }
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 3);
                }
              }
              y++;

              if (blk.getTagType(a) != TagType.function) {
                if (create) {
                  newCells.add(createCell(x, y, 3, 1, "dual", null, tag, null, null, null, style));
                  newNodes.add(node.addChild('x', x, y));
                } else {
                  child = node.childs.get(childIdx++);
                  if (child.x != x || child.y != y || child.moved) {
                    moveNode(logic, child, x, y, 3);
                  }
                }
              }
              y--;
              x += 3;
              tagIdx++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b6", null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y || child.moved) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              y++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b6", null));
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
              newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b1", null));
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
                newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b2", null));
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
              newCells.add(createCell(x, y, 1, 1, "image", null, null, null, null, "b3", null));
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
              newCells.add(createCell(x, y, 1, 1, "image", "en_1_" + context.debug_en_idx++, null, null, null, "b9", null));
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

  public static void layoutNodes(NodeRoot root, Table logic, WebUIClient client) {
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
      buildNodes(root, logic, newCells, newNodes, client, root.rid, false);
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
  public static void showError(WebUIClient client, String msg) {
    Label lbl = (Label)client.getPanel().getComponent("jfc_error_msg");
    lbl.setText(msg);
    PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_error");
    panel.setVisible(true);
  }
  public static void showErrorText(WebUIClient client, String msg, String text) {
    Label lbl = (Label)client.getPanel().getComponent("jfc_error_textarea_msg");
    lbl.setText(msg);
    TextArea ta = (TextArea)client.getPanel().getComponent("jfc_error_textarea_textarea");
    ta.setText(text);
    PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_error_textarea");
    panel.setVisible(true);
  }
  public static void confirm(WebUIClient client, String msg, String action) {
    Label lbl = (Label)client.getPanel().getComponent("jfc_confirm_msg");
    lbl.setText(msg);
    PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_confirm");
    panel.setVisible(true);
    client.setProperty("action", action);
  }
}
