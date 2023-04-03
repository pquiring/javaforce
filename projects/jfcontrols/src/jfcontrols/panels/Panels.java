package jfcontrols.panels;

/** Panels
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.controls.*;

import jfcontrols.tags.*;
import jfcontrols.images.*;
import jfcontrols.app.*;
import jfcontrols.functions.*;
import jfcontrols.logic.*;
import jfcontrols.db.*;

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
    JFLog.log("getPanel:" + pname);
    return buildPanel(new Panel(), pname, client);
  }
  public static Panel buildPanel(Panel panel, String pname, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    PanelRow pnl = Database.getPanelByName(pname);
    if (pnl == null) {
      JFLog.log("Error:Unable to find panel:" + pname);
      return null;
    }
    CellRow cells[] = Database.getCells(pnl.name);
    Table table = new Table(cellWidth,cellHeight,1,1);
    panel.add(table);
    buildTable(table, panel, cells, client, -1, -1, null);
    if (pnl.popup) return panel;
    //add top components
    int x = 0;
    int width = client.getWidth();
    if (width < 16) {
      width = 16;
    }

    Button menu = getButton(createCell(0, 0, 0, 0, "button", null, "!image:menu", null, "showMenu", null, null));
    menu.setProperty("func", "showMenu");
    setCellSize(menu, new Rectangle(x,0,1,1));
    table.add(menu, x, 0);
    x++; width -= cellWidth;

    Button home = getButton(createCell(0, 0, 0, 0, "button", null, "!image:home", null, "setPanel", "Main", null));
    home.setProperty("func", "setPanel");
    home.setProperty("arg", "Main");
    setCellSize(home, new Rectangle(x,0,1,1));
    table.add(home, x, 0);
    x++; width -= cellWidth;

    Label alarms = getLabel(createCell(0, 0, 0, 0, "label", null, "0", null, "setPanel", "jfc_alarms", null));
    alarms.setProperty("func", "setPanel");
    alarms.setProperty("arg", "jfc_alarms");
    alarms.setBorder(true);
    setCellSize(alarms, new Rectangle(x,0,1,1));
    table.add(alarms, x, 0);
    x++; width -= cellWidth;

    int xref = (Integer)client.getProperty("xref");
    if (xref != -1) {
      Button xrefBtn = getButton(createCell(0, 0, 0, 0, "button", null, "!image:ret_xref", null, "setPanel", "jfc_xref", null));
      xrefBtn.setProperty("func", "setPanel");
      xrefBtn.setProperty("arg", "jfc_xref");
      setCellSize(xrefBtn, new Rectangle(x,0,1,1));
      table.add(xrefBtn, x, 0);
      x++; width -= cellWidth;
      //client.setProperty("xref", -1);
    }

    Label title = getLabel(createCell(0, 0, 0, 0, "label", "jfc_title", getPanelName(pnl.name), null, null, null, null));
    title.setName("jfc_title");
    title.setStyle("background-color", "blue");
    title.setStyle("color", "white");
    title.setStyle("padding-left", "16px");
    title.setAlign(Component.LEFT);
    setCellSize(title, new Rectangle(x,0,width / cellWidth,1));
    table.add(title, x, 0, width / cellWidth, 1);

    int audio_init = (Integer)client.getProperty("audio-init");

    TagBase tag = context.getTag("alarms");
    context.addListener(tag, alarms, true, (_tag, _oldValue, _newValue, _cmp) -> {
      updateAlarmCount(alarms, client);
    });
    updateAlarmCount(alarms, client);

    panel.add(getPopupPanel(client, "Login", "jfc_login"));
    panel.add(getPopupPanel(client, "Menu", "jfc_menu"));
    panel.add(getPopupPanel(client, "Confirm", "jfc_confirm"));
    panel.add(getPopupPanel(client, "Error", "jfc_error"));
    panel.add(getPopupPanel(client, "Error", "jfc_error_textarea"));
    panel.add(getPopupPanel(client, "NewTag", "jfc_new_tag"));
    panel.add(getPopupPanel(client, "NewTag", "jfc_new_tag_udt"));
    ColorChooserPopup color = new ColorChooserPopup();
    color.setName("colorpanel");
    color.setTitleBarSize(cellHeight);
    color.setComponentsSize(cellWidth, cellHeight);
    color.addActionListener((cmpnt) -> {
      ColorChooserPopup cp = (ColorChooserPopup)cmpnt;
      Light light = (Light)cmpnt.getClient().getProperty("light");
      int clr = cp.getValue();
      light.setBackColor(cp.getValue());
    });
    panel.add(color);
    if (pname.equals("jfc_config")) {
      panel.add(getPopupPanel(client, "Change Password", "jfc_change_password"));
    }
    KeyPad keypad = new KeyPad("KeyPad", cellWidth);
    keypad.setName("keypad");
    panel.add(keypad);
    if (pname.equals("jfc_panel_editor")) {
      panel.add(getPopupPanel(client, "Properties", "jfc_panel_props"));
    }
    return panel;
  }
  private static String getPanelName(String name) {
    if (name.startsWith("usr_")) return name.substring(4);
    switch (name) {
      case "jfc_xref": return "Cross References";
      case "jfc_udts": return "User Data Types";
      case "jfc_udt_editor": return "User Data Type Editor";
      case "jfc_sdts": return "System Data Types";
      case "jfc_sdt_editor": return "System Data Type Editor";
      case "jfc_funcs": return "Functions";
      case "jfc_func_editor": return "Function Editor";
      default:
        name = name.substring(4).replaceAll("_", " ");
        //Camel Case String
        char[] ca = name.toCharArray();
        for(int a=0;a<ca.length;a++) {
          if (a == 0 || ca[a-1] == ' ') {
            ca[a] = Character.toUpperCase(ca[a]);
          }
        }
        return new String(ca);
    }
  }
  //x,y,w,h,comp,name,text,tag,func,arg,style,events
  public static Table buildTable(Table table, Container container, CellRow cells[], WebUIClient client, int ix, int iy, Node nodes[]) {
    ClientContext context = (ClientContext)client.getProperty("context");
    int mx = table.getColumns();
    if (ix != -1) mx = ix;
    int my = table.getRows();
    if (iy != -1) my = iy;
    Component cs[] = new Component[cells.length];
    Rectangle rs[] = new Rectangle[cells.length];
    for(int a=0;a<cells.length;a++) {
      Rectangle r = new Rectangle();
      rs[a] = r;
      r.x = cells[a].x;
      r.y = cells[a].y;
      r.width = cells[a].w;
      r.height = cells[a].h;
      String compType = cells[a].comp;
      String tagName = cells[a].tag;
      if (tagName != null && !tagName.startsWith("jfc_") && tagName.length() > 0) {
        TagBase tag = context.getTag(tagName);
        if (tag == null) {
          JFLog.log("Error:Tag not found:" + tagName);
        } else {
          cells[a].text = tag.getValue();
        }
      }
      Component c = getCell(compType, container, cells[a], rs[a], client, context);
      if (c == null) {
        JFLog.log("Error:cell == null:" + compType);
        c = new Label("null");
      }
      c.setProperty("name", compType);
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
      if (tagName != null) {
        c.setProperty("tag", tagName);
        if (!tagName.startsWith("jfc_")) {
          TagBase tag = context.decode(tagName);
          context.addListener(tag, c, false, (_tag, oldValue, newValue, cmp) -> {
            String type = Events.getComponentType(cmp);
//            JFLog.log("Event:" + type + ":" + tagName + ":" + oldValue + ":" + newValue);
            switch (type) {
              case "label":
                Label lbl = (Label)cmp;
                lbl.setText(newValue);
                break;
              case "button":
                Button b = (Button)cmp;
                b.setText(newValue);
                break;
              case "light":
                Light l = (Light)cmp;
                l.setColor(!newValue.equals("0"));
                break;
              case "light3":
                Light3 l3 = (Light3)cmp;
                l3.setColor(Integer.valueOf(newValue));
                break;
              case "togglebutton":
                ToggleButton tb = (ToggleButton)cmp;
                tb.setSelected(!newValue.equals("0"));
                break;
              case "progressbar":
                ProgressBar pb = (ProgressBar)cmp;
                pb.setValue(Float.valueOf(newValue));
                break;
              default:
                JFLog.log("Error:Component Type unknown:" + type);
                break;
            }
          });
        }
      }
      c.setProperty("func", cells[a].func);
      c.setProperty("arg", cells[a].arg);
      c.setProperty("events", cells[a].events);
      if (cells[a].name != null) {
        c.setName(cells[a].name);
      } else if (cells[a].tag != null) {
        c.setName(cells[a].tag);
      }
      if (nodes != null && nodes.length > a) {
        c.setProperty("node", nodes[a]);
        nodes[a].comp = c;
        c.addClickListener((me, comp) -> {
          Component focus = (Component)client.getProperty("focus");
          if (focus != null) {
            focus.setBorderColor(Color.grey);
            focus.setBorder(false);
          }
          Node node = (Node)comp.getProperty("node");
          comp.setBorderColor(Color.black);
          comp.setBorder(true);
          client.setProperty("focus", comp);
          Node src = (Node)client.getProperty("fork");
          if (src != null) {
            node.forkDest(client, table, src);
          }
        });
      }
      String style = cells[a].style;
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
          } else if (styles[b].equals("xsmallfont")) {
            c.setStyle("font-size", "6pt");
          } else if (styles[b].equals("smallfont")) {
            c.setStyle("font-size", "8pt");
          } else if (styles[b].equals("left")) {
            c.setAlign(Component.LEFT);
          } else if (styles[b].equals("right")) {
            c.setAlign(Component.RIGHT);
          } else {
            String f[] = styles[b].split(":");
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
      if (cmp instanceof ScrollPanel) {
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
  public static Component getCell(String name, Container container, CellRow v, Rectangle r, WebUIClient client, ClientContext context) {
    switch (name) {
      case "label": return getLabel(v);
      case "button": return getButton(v);
      case "togglebutton": return getToggleButton(v, client);
      case "link": return getLink(v);
      case "textfield": return getTextField(v, client, false);
      case "password": return getTextField(v, client, true);
      case "dual": return getDual(v, client);
      case "textarea": return getTextArea(v, client);
      case "combobox": return getComboBox(v, client);
      case "checkbox": return getCheckBox(v, client);
      case "table": return getTable(v, container, r, client);
      case "overlay": return getOverlay(v, r.y == 0);
      case "image": Image img = getImage(v); client.setProperty(v.text, img); return img;
      case "layers": LayersPanel panel = getLayersPanel(v, client); client.setProperty(v.text, panel); return panel;
      case "autoscroll": return getAutoScroll(v, container, client);
      case "light": return getLight(v, context);
      case "light3": return getLight3(v, context);
      case "progressbar": return getProgressBar(v, context);
      default: JFLog.log("Unknown component:" + name); break;
    }
    return null;
  }
  private static Label getLabel(CellRow v) {
    String text = v.text;
    Label lbl;
    if (text == null) {
      JFLog.log("Label.text == null:" + v.name);
    }
    if (text.startsWith("!image:")) {
      lbl = new Label(Images.getImage(text.substring(7)));
    } else {
      lbl = new Label(text);
    }
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
  private static Button getButton(CellRow v) {
    String text = v.text;
    Button b = null;
    if (text.startsWith("!image:")) {
      b = new Button(Images.getImage(text.substring(7)));
      b.setBorder(true);
    } else {
      b = new Button(v.text);
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
  private static Component getToggleButton(CellRow v, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String text = v.text;
    String style = v.style;
    if (style == null) style = "";
    String ss[] = style.split(";");
    String off = "ff0000";
    String on = "00ff00";
    for(int a=0;a<ss.length;a++) {
      String s = ss[a];
      int idx = s.indexOf("=");
      if (idx == -1) continue;
      String key = s.substring(0, idx);
      String value = s.substring(idx + 1);
      switch (key) {
        case "0": off = value; break;
        case "1": on = value; break;
      }
    }
    ToggleButton b = new ToggleButton(v.text, Integer.valueOf(off, 16), Integer.valueOf(on, 16));
    b.addClickListener((me, c) -> {
      Events.click(c);
    });
    b.addChangedListener((c) -> {
      Events.changed((ToggleButton)c);
    });
    b.addMouseDownListener((c) -> {
      Events.press(c);
    });
    b.addMouseUpListener((c) -> {
      Events.release(c);
    });
    String tag = v.tag;
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        text = Database.select(table, id, col, type);
        if (text == null) text = "false";
        b.setSelected(!text.equals("false"));
      } else {
        text = context.read(tag);
        if (text == null) text = "0";
        b.setSelected(!text.equals("0"));
      }
    }
    return b;
  }
  private static Button getLink(CellRow v) {
    String text = v.text;
    Button b = null;
    if (text.startsWith("!image:")) {
      b = new Button(Images.getImage(text.substring(7)));
    } else {
      b = new Button(v.text);
    }
    b.setURL("http://jfcontrols.sourceforge.net/help_" + v.arg + ".php");
    return b;
  }
  private static Component getDual(CellRow v, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    Table table = new Table(cellWidth, cellHeight/2, 3, 2);
    TagBase tag = context.getTag(v.tag);
    String tagcomment = "";
    if (tag != null) tagcomment = tag.getComment();
    Label comment = new Label(tagcomment);
    comment.setName("tc_" + context.debug_tv_idx);
    table.add(comment, 0, 0, 3, 1);
    Label value = new Label("");
    value.setName("tv_" + context.debug_tv_idx);
    context.debug_tv_idx++;
    table.add(value, 0, 1, 3, 1);
    return table;
  }
  private static TextField getTextField(CellRow v, WebUIClient client, boolean password) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = v.tag;
    String text = v.text;
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        text = Database.select(table, id, col, type);
      } else {
        text = context.read(tag);
      }
    }
    if (text == null) text = "";
    TextField tf = new TextField(text);
    tf.addChangedListener((c) -> {
      Events.edit((TextField)c);
    });
    if (password) {
      tf.setPassword(true);
    } else {
      tf.addClickListener((me, comp) -> {
        KeyPad keypad = (KeyPad)comp.getClient().getPanel().getComponent("keypad");
        keypad.show((TextField)comp);
      });
      tf.addKeyDownListener((ke, comp) -> {
        KeyPad keypad = (KeyPad)comp.getClient().getPanel().getComponent("keypad");
        if (keypad.isVisible()) {
          keypad.setVisible(false);
        }
      });
    }
    return tf;
  }
  private static TextArea getTextArea(CellRow v, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = v.tag;
    String text = v.text;
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        text = Database.select(table, id, col, type);
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
  private static ComboBox getComboBox(CellRow v, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    ComboBox cb = new ComboBox();
    String name = v.name;
    String tag = v.tag;
    String arg = v.arg;
    String value = v.text;
    String pairs[][];
    if (arg.equals("jfc_function")) {
      FunctionRow funcs[] = Database.funcs.getRows().toArray(new FunctionRow[0]);
      pairs = new String[funcs.length][2];
      for(int a=0;a<funcs.length;a++) {
        pairs[a][0] = Integer.toString(funcs[a].id);
        pairs[a][1] = funcs[a].name;
      }
    } else if (arg.equals("jfc_config_backups")) {
      File files[] = new File(Paths.backupPath).listFiles();
      if (files == null) files = new File[0];
      JFLog.log("# backups=" + files.length);
      pairs = new String[files.length][2];
      for(int a=0;a<files.length;a++) {
        String filename = files[a].getName();
        pairs[a][0] = filename;
        pairs[a][1] = filename;
      }
    } else if (arg.equals("jfc_logic_groups")) {
      String groups[] = Database.getLogicGroups();
      pairs = new String[groups.length][2];
      for(int a=0;a<groups.length;a++) {
        pairs[a][0] = groups[a];
        pairs[a][1] = groups[a];
      }
      value = "bit";
    } else if (arg.equals("jfc_tag_type_udt")) {
      javaforce.db.Table<ListRow> listTable = Database.getList("jfc_tag_type");
      ListRow basicTypes[] = (ListRow[])listTable.getRows().toArray(new ListRow[0]);
      String basic[][] = new String[basicTypes.length][2];
      for(int a=0;a<basicTypes.length;a++) {
        basic[a][0] = Integer.toString(basicTypes[a].idx);
        basic[a][1] = basicTypes[a].value;
      }
      UDT udtTypes[] = Database.udts.getRows().toArray(new UDT[0]);
      String udts[][] = new String[udtTypes.length][2];
      for(int a=0;a<udtTypes.length;a++) {
        udts[a][0] = Integer.toString(udtTypes[a].id);
        udts[a][1] = udtTypes[a].name;
      }
      pairs = new String[basic.length + udts.length][2];
      int pos = 0;
      for(int a=0;a<basic.length;a++) {
        pairs[pos++] = basic[a];
      }
      for(int a=0;a<udts.length;a++) {
        pairs[pos++] = udts[a];
      }
    } else {
      javaforce.db.Table<ListRow> listTable = Database.getList(arg);
      ListRow data[] = (ListRow[])listTable.getRows().toArray(new ListRow[0]);
      pairs = new String[data.length][];
      for(int a=0;a<data.length;a++) {
        pairs[a] = new String[2];
        pairs[a][0] = Integer.toString(data[a].idx);
        pairs[a][1] = data[a].value;
      }
    }
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_type_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        value = Database.select(table, id, col, type);
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
  private static CheckBox getCheckBox(CellRow v, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = v.tag;
    String text = v.text;
    String value = "0";
    if (tag != null) {
      if (tag.startsWith("jfc_")) {
        String f[] = tag.split("_", 5);
        //jfc_table_col_id
        String table = f[1];
        String col = f[2];
        String type = f[3];
        String id = f[4];
        value = Database.select(table, id, col, type);
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
  private static Image getImage(CellRow v) {
    Image img = new Image(Images.getImage(v.text));
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
  private static LayersPanel getLayersPanel(CellRow v, WebUIClient client) {
    LayersPanel panel = new LayersPanel();
    int pid = (Integer)client.getProperty("visionprogram");
    int sid = (Integer)client.getProperty("visionshot");
    VisionSystem.setupVisionImage(panel, pid, sid, -1);
    return panel;
  }
  private static CellRow createCell(int x, int y, int w, int h, String comp, String name, String text, String tag, String func, String arg, String style /*, String events */) {
    CellRow cell = new CellRow(-1,x,y,w,h,comp,name,text);
    cell.setTag(tag);
    cell.setFuncArg(func, arg);
    cell.setStyle(style);
//    cell.setEvents(events);
    return cell;
  }
  private static boolean empty(CellRow[] cells, int cx, int cy) {
    int cnt = cells.length;
    for(int a=0;a<cnt;a++) {
      CellRow cell = cells[a];
      int x1 = Integer.valueOf(cell.x);
      int y1 = Integer.valueOf(cell.y);
      int x2 = x1 + Integer.valueOf(cell.w) - 1;
      int y2 = y1 + Integer.valueOf(cell.h) - 1;
      if ( (cx >= x1 && cx <= x2) && (cy >= y1 && cy <= y2) ) {
        return false;
      }
    }
    return true;
  }
//   cells[][] = "id,x,y,w,h,comp,name,text,tag,func,arg,style"
  private static Component getTable(CellRow v, Container container, Rectangle r, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String name = v.name;
    String arg = v.arg;
    ArrayList<CellRow> cells = new ArrayList<CellRow>();
    ArrayList<Node> nodes = new ArrayList<Node>();
    Table table;
    JFLog.log("getTable:" + name);
    switch (name) {
      case "jfc_ctrls" : {
        ControllerRow[] ctrls = Database.controllers.getRows().toArray(new ControllerRow[0]);
        for(int a=0;a<ctrls.length;a++) {
          String style = ctrls[a].cid == 0 ? "disabled" : null;
          String cid = Integer.toString(ctrls[a].id);
          cells.add(createCell(0, a, 1, 1, "textfield", null, cid, "jfc_ctrls_cid_int_" + cid, null, null, style));
          cells.add(createCell(1, a, 3, 1, "textfield", null, ctrls[a].ip, "jfc_ctrls_ip_str_" + cid, null, null, style));
          cells.add(createCell(4, a, 2, 1, "combobox", null, null, "jfc_ctrls_type_int_" + cid, null, "jfc_ctrl_type", style));
          cells.add(createCell(6, a, 2, 1, "combobox", null, null, "jfc_ctrls_speed_int_" + cid, null, "jfc_ctrl_speed", style));
          cells.add(createCell(9, a, 2, 1, "button", null, "Tags", null, "jfc_ctrl_tags", cid, null));
          if (style == null) {
            cells.add(createCell(12, a, 2, 1, "button", null, "Delete", null, "jfc_ctrl_delete", cid, null));
          }
        }
        break;
      }
      case "jfc_tags": {
        client.setProperty("xref", -1);
        int id = (Integer)client.getProperty("ctrl");
        int cid = Database.getControllerCID(id);
        String tag_types;
        String tag_type;
        if (cid == 0) {
          tag_types = "jfc_tag_type_udt";
          tag_type = "tagid";
        } else {
          //TODO : support remote UDT if controller = JFC
          tag_types = "jfc_tag_type";
          tag_type = "tag";
        }
        TagRow tags[] = Database.getTagsByCid(Integer.valueOf(cid));
        String style;
        for(int a=0;a<tags.length;a++) {
          if (tags[a].builtin) {
            style = "readonly";
          } else {
            style = null;
          }
          String tagid = Integer.toString(tags[a].id);
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_tags_name_" + tag_type + "_" + tagid, null, null, style));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_tags_type_int_" + tagid, null, tag_types, "readonly"));
          if (cid == 0) {
            cells.add(createCell(9, a, 3, 1, "textfield", null, null, "jfc_tags_arraysize_int_" + tagid, null, null, "readonly"));
          }
          cells.add(createCell(12, a, 6, 1, "textfield", null, null, "jfc_tags_comment_str_" + tagid, null, null, style));
          if (style == null) {
            cells.add(createCell(19, a, 2, 1, "button", null, "Delete", null, "jfc_tags_delete", tagid, null));
          }
          cells.add(createCell(22, a, 2, 1, "button", null, "XRef", null, "jfc_tags_xref", tagid, null));
        }
        break;
      }
      case "jfc_xref": {
        int xref = (Integer)client.getProperty("xref");
        TagRow tagrow = Database.getTagById(xref);
        if (tagrow == null) {
          JFLog.log("Error:xref not found:" + xref);
          break;
        }
        String tag = tagrow.name;
        if (tagrow.cid > 0) {
          tag = "c" + tagrow.cid + "#" + tag;
        }
        int y = 0;
        cells.add(createCell(0,y++,6,1, "label", null, "Tag:" + tag, null, null, null, null));
        BlockRow data[] = Database.getBlocksUsingTagId(tagrow.id);
        if (data.length > 0) {
          cells.add(createCell(0, y, 6, 1, "label", null, "Function", null, null, null, null));
          cells.add(createCell(6, y, 3, 1, "label", null, "Rung", null, null, null, null));
        } else {
          cells.add(createCell(0, y, 6, 1, "label", null, "No Functions", null, null, null, null));
        }
        y++;
        for(int a=0;a<data.length;a++) {
          FunctionRow func = Database.getFunctionById(data[0].fid);
          cells.add(createCell(0, y, 6, 1, "label", null, func.name, null, null, null, null));
          cells.add(createCell(6, y, 3, 1, "label", null, "Rung " + (data[0].rid+1), null, null, null, null));
          cells.add(createCell(10, y, 2, 1, "button", null, "View", null, "jfc_xref_view_func", Integer.toString(data[a].id), null));
          y++;
        }
        PanelRow panels[] = Database.getPanelsUsingTagId(tagrow.id);
        if (panels.length > 0) {
          cells.add(createCell(0, y, 6, 1, "label", null, "Panel", null, null, null, null));
        } else {
          cells.add(createCell(0, y, 6, 1, "label", null, "No Panels", null, null, null, null));
        }
        y++;
        for(int a=0;a<panels.length;a++) {
          cells.add(createCell(0, y, 6, 1, "label", null, panels[a].name, null, null, null, null));
          cells.add(createCell(10, y, 2, 1, "button", null, "View", null, "jfc_xref_view_panel", Integer.toString(panels[a].id), null));
          y++;
        }
        break;
      }
      case "jfc_watch": {
        javaforce.db.Table data[] = Database.watches.getTables().toArray(new javaforce.db.Table[0]);
        for(int a=0;a<data.length;a++) {
          String wid = Integer.toString(data[a].id);
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_watch_name_str_" + data[a].id, null, null, null));
          cells.add(createCell(7, a, 2, 1, "button", null, "Edit", null, "jfc_watch_edit", wid, null));
          cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_watch_delete", wid, null));
        }
        break;
      }
      case "jfc_watch_tags": {
        int wid = (Integer)client.getProperty("watch");
        WatchRow data[] = Database.getWatchTagsById(wid);
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_watchtags_tag_tagid_" + wid + "_" + data[a].id, null, null, null));
          cells.add(createCell(7, a, 6, 1, "label", "tag_" + a, "", null, null, null, null));
          cells.add(createCell(14, a, 2, 1, "button", null, "Delete", null, "jfc_watch_tag_delete", Integer.toString(data[a].id), null));
        }
        break;
      }
      case "jfc_udts": {
        UDT data[] = Database.udts.getRows().toArray(new UDT[0]);
        int pos = 0;
        String style = null;
        for(int a=0;a<data.length;a++) {
          if (data[a].id < IDs.uid_user) continue;
          if (data[a].id == IDs.uid_alarms) style = "readonly"; else style = null;
          cells.add(createCell(0, pos, 6, 1, "textfield", null, null, "jfc_udts_name_tagid_" + data[a].id, null, null, style));
          cells.add(createCell(8, pos, 2, 1, "button", null, "Edit", null, "jfc_udts_edit", Integer.toString(data[a].id), null));
          if (data[a].id > IDs.uid_alarms) {
            cells.add(createCell(11, pos, 2, 1, "button", null, "Delete", null, "jfc_udts_delete", Integer.toString(data[a].id), null));
          }
          pos++;
        }
        break;
      }
      case "jfc_udt_editor": {
        int uid = (Integer)client.getProperty("udt");
        UDTMember data[] = Database.getUDTMembersById(uid);
        String style = null;
        for(int a=0;a<data.length;a++) {
          if (data[a].builtin) style = "readonly"; else style = null;
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udtmems_name_tagid_" + data[a].id, null, null, style));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_udtmems_type_int_" + data[a].id, null, "jfc_tag_type", style));
          cells.add(createCell(18, a, 6, 1, "textfield", null, null, "jfc_udtmems_comment_str_" + data[a].id, null, null, style));
          if (!data[a].builtin) {
            cells.add(createCell(25, a, 2, 1, "button", null, "Delete", null, "jfc_udt_editor_delete", Integer.toString(data[a].id), null));
          }
        }
        break;
      }
      case "jfc_sdts": {
        UDT data[] = Database.udts.getRows().toArray(new UDT[0]);
        int pos = 0;
        for(int a=0;a<data.length;a++) {
          if (data[a].id >= IDs.uid_user) continue;
          cells.add(createCell(0, pos, 6, 1, "textfield", null, null, "jfc_udts_name_tagid_" + data[a].id, null, null, "readonly"));
          cells.add(createCell(8, pos, 2, 1, "button", null, "View", null, "jfc_sdts_edit", Integer.toString(data[a].id), null));
          pos++;
        }
        break;
      }
      case "jfc_sdt_editor": {
        int uid = (Integer)client.getProperty("udt");
        UDTMember data[] = Database.getUDTMembersById(uid);
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(0, a, 6, 1, "textfield", null, null, "jfc_udtmems_name_tagid_" + data[a].id, null, null, "readonly"));
          cells.add(createCell(6, a, 3, 1, "combobox", null, null, "jfc_udtmems_type_int_" + data[a].id, null, "jfc_tag_type", "readonly"));
        }
        break;
      }
      case "jfc_panels": {
        PanelRow data[] = Database.panels.getRows().toArray(new PanelRow[0]);
        int pos = 0;
        for(int a=0;a<data.length;a++) {
          if (data[a].builtin) continue;
          String id = Integer.toString(data[a].id);
          String style = data[a].name.equals("usr_Main") ? "disabled" : null;
          cells.add(createCell(0, pos, 6, 1, "textfield", null, null, "jfc_panels_name_str_" + data[a].id, null, null, style));
          cells.add(createCell(7, pos, 2, 1, "button", null, "Edit", null, "jfc_panels_edit", id, null));
          if (style == null) {
            cells.add(createCell(10, pos, 2, 1, "button", null, "Delete", null, "jfc_panels_delete", id, null));
          }
          pos++;
        }
        break;
      }
      case "jfc_panel_editor": {
        int pid = (Integer)client.getProperty("panel");
        CellRow data[] = Database.getCells(Database.getPanelById(pid).name);
        LayersPanel layers = new LayersPanel();
        table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, data, client, 64, 64, null);
        table.setName("t1");
        r.width = table.getColumns();
        r.height = table.getRows();
        layers.add(table);
        for(int a=0;a<data.length;a++) {
          CellRow cell = data[a];
          cells.add(createCell(cell.x, cell.y, cell.w, cell.h, "overlay", null, null, null, null, null, null));
        }
        CellRow cellsArray[] = cells.toArray(new CellRow[cells.size()]);
        for(int x=0;x<64;x++) {
          for(int y=0;y<64;y++) {
            if (empty(cellsArray,x,y)) {
              cells.add(createCell(x, y, 1, 1, "overlay", null, null, null, null, null, null));
            }
          }
        }
        table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new CellRow[cells.size()]), client, 64, 64, null);
        table.setName("t2");
        layers.add(table);
        return layers;
      }
      case "jfc_funcs": {
        FunctionRow data[] = Database.funcs.getRows().toArray(new FunctionRow[0]);
        for(int a=0;a<data.length;a++) {
          String fid = Integer.toString(data[a].id);
          String funcname = data[a].name;
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
        String groups[] = Database.getLogicGroups();
        int idx = -1;
        for(int a=0;a<groups.length;a++) {
          tabs.addTab(wrapPanel(getTable(createCell(r.x, r.y, r.width, r.height, "table", "jfc_logics", null, null, null, groups[a], null), null, new Rectangle(r), client)), "");
          if (groups[a].equals("bit")) idx = a;
        }
        if (idx != -1) tabs.setTabIndex(idx);
        setCellSize(tabs, r);
        client.setProperty("groups", tabs);
        return tabs;
      }
      case "jfc_logics": {
        LogicRow items[] = Database.getLogicsByGroupId(arg);
        for(int a=0;a<items.length;a++) {
          String desc = items[a].name;
          String shortname = items[a].shortname;
          if (shortname != null) {
            desc = shortname;
          }
          String style = "border";
          if (Images.getImage(desc) != null) {
            desc = "!image:" + desc;
          } else {
            String lns[] = desc.split("_");
            if (lns.length > 2) {
              style += ";xsmallfont";
            }
            else if (desc.length() > 3) {
              style += ";smallfont";
            }
            desc = desc.replaceAll("_", "<br/>");
          }
          cells.add(createCell(a, 0, 1, 1, "button", items[a].name, desc, null, "jfc_rung_editor_add", null, style));
        }
        break;
      }
      case "jfc_rung_viewer": {
        int fid = (Integer)client.getProperty("func");
        int rid = Integer.valueOf(arg);
        RungRow data = Database.getRungById(fid, rid);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        Rung rung = buildRung(data, cells, nodes, client, true, fid);
        if (rung == null) {
          rung = new Rung();
          rung.root = new NodeRoot(fid, rid);
        }
        rungs.rungs.add(rung);
        break;
      }
      case "jfc_rung_viewer_end": {
        int fid = (Integer)client.getProperty("func");
        cells.add(createCell(0, 0, 5, 1, "label", null, "End of Function", null, null, null, null));
        nodes.add(new NodeRoot(fid, -1));
        break;
      }
      case "jfc_alarm_editor_table": {
        TagBase alarms = TagsService.getTag("alarms");
        int length = alarms.getLength();
        for(int a=0;a<length;a++) {
          TagBase fields[] = alarms.getFields(a);
          TagBase field = fields[IDs.fid_alarm_text];
          String alarmText = field.getString8(0);
          cells.add(createCell(0, a, 2, 1, "label", null, Integer.toString(a), null, null, null, null));
          cells.add(createCell(2, a, 6, 1, "textfield", null, alarmText, "alarms[" + a + "].text" , null, null, null));
          cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_alarm_editor_delete", Integer.toString(a), null));
        }
        break;
      }
      case "jfc_alarm": {
        TagUDT alarms = (TagUDT)TagsService.getTag("alarms");
        int idx = 0;  //TODO
        //TagUDT timer_udt = (TagUDT)alarms;
        //TagBase[] timer = timer_udt.fields[IDs.fid_alarm_text];
        TagBase fields[] = alarms.getFields(idx);
        String alarmName = fields[IDs.fid_alarm_text].getString8();
        boolean alarmAck = fields[IDs.fid_alarm_ack].getBoolean();
        cells.add(createCell(2, 0, 2, 1, "label", null, alarmAck ? "X" : "", null, null, null, null));
        cells.add(createCell(4, 0, 10, 1, "label", null, arg + ":" + alarmName, null, null, null, null));
        break;
      }
      case "jfc_alarm_history": {
        long start = Long.valueOf(arg);
        long end = start + ms_per_day;
        AlarmRow data[] = Database.getAlarms(start, end);
        for(int a=0;a<data.length;a++) {
          cells.add(createCell(2, a, 4, 1, "label", null, Long.toString(data[a].timestamp), null, null, null, null));  //when
          cells.add(createCell(6, a, 10, 1, "label", null, arg + ":" + data[a], null, null, null, null));  //name
        }
        break;
      }
      case "jfc_config_errors": {
        cells.add(createCell(0, 0, 20, 4, "textarea", null, Main.msgs, null, null, null, "readonly"));
        break;
      }
      case "jfc_vision_cameras": {
        VisionCameraRow[] cams = Database.visioncameras.getRows().toArray(new VisionCameraRow[0]);
        for(int a=0;a<cams.length;a++) {
          String id = Integer.toString(cams[a].id);
          cells.add(createCell(0, a, 1, 1, "textfield", null, id, "jfc_visioncameras_cid_int_" + id, null, null, null));
          cells.add(createCell(1, a, 3, 1, "textfield", null, id, "jfc_visioncameras_name_int_" + id, null, null, null));
          cells.add(createCell(4, a, 5, 1, "textfield", null, cams[a].url, "jfc_visioncameras_url_str_" + id, null, null, null));
          cells.add(createCell(10, a, 2, 1, "button", null, "Delete", null, "jfc_visioncamera_delete", id, null));
        }
        break;
      }
      case "jfc_vision_programs": {
        VisionProgramRow[] prgs = Database.visionprograms.getRows().toArray(new VisionProgramRow[0]);
        for(int a=0;a<prgs.length;a++) {
          String id = Integer.toString(prgs[a].id);
          cells.add(createCell(0, a, 1, 1, "textfield", null, id, "jfc_visionprograms_pid_int_" + id, null, null, null));
          cells.add(createCell(1, a, 3, 1, "textfield", null, prgs[a].name, "jfc_visionprograms_name_str_" + id, null, null, null));
          cells.add(createCell(4, a, 2, 1, "button", null, "Edit", null, "jfc_vision_program_edit", id, null));
          cells.add(createCell(6, a, 2, 1, "button", null, "Delete", null, "jfc_vision_program_delete", id, null));
        }
        break;
      }
      case "jfc_vision_shots": {
        VisionShotRow[] prgs = Database.visionshots.getRows().toArray(new VisionShotRow[0]);
        for(int a=0;a<prgs.length;a++) {
          String id = Integer.toString(prgs[a].id);
          cells.add(createCell(0, a, 2, 1, "textfield", null, id, "jfc_visionshots_cid_int_" + id, null, null, null));
          cells.add(createCell(2, a, 2, 1, "textfield", null, id, "jfc_visionshots_offset_int_" + id, null, null, null));
          cells.add(createCell(4, a, 2, 1, "button", null, "Edit", null, "jfc_vision_shot_edit", id, null));
          cells.add(createCell(6, a, 2, 1, "button", null, "Select", null, "jfc_vision_shot_select", id, null));
          cells.add(createCell(8, a, 2, 1, "button", null, "Delete", null, "jfc_vision_shot_delete", id, null));
        }
        break;
      }
      case "jfc_vision_areas": {
        int pid = (Integer)client.getProperty("visionprogram");
        int sid = (Integer)client.getProperty("visionshot");
        VisionAreaRow[] rois = Database.getVisionAreas(pid, sid);
        for(int a=0;a<rois.length;a++) {
          VisionAreaRow row = rois[a];
          String id = Integer.toString(row.id);
          cells.add(createCell(0, a, 3, 1, "textfield", null, rois[a].name, "jfc_visionareas_name_str_" + id, null, null, null));
          cells.add(createCell(3, a, 2, 1, "textfield", null, Integer.toString(row.x1), "jfc_visionareas_x1_int_" + id, null, null, null));
          cells.add(createCell(5, a, 2, 1, "textfield", null, Integer.toString(row.y1), "jfc_visionareas_y1_int_" + id, null, null, null));
          cells.add(createCell(7, a, 2, 1, "textfield", null, Integer.toString(row.x2), "jfc_visionareas_x2_int_" + id, null, null, null));
          cells.add(createCell(9, a, 2, 1, "textfield", null, Integer.toString(row.y2), "jfc_visionareas_y2_int_" + id, null, null, null));
          cells.add(createCell(11, a, 2, 1, "button", null, "Select", null, "jfc_vision_area_select", id, null));
          if (a > 0) {
            cells.add(createCell(13, a, 2, 1, "button", null, "Delete", null, "jfc_vision_area_delete", id, null));
          }
        }
        break;
      }
      default: {
        JFLog.log("Unknown table:" + name);
      }
    }
    table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new CellRow[cells.size()]), client, -1, -1, nodes.toArray(new Node[nodes.size()]));
    r.width = table.getColumns();
    r.height = table.getRows();
    switch (name) {
      case "jfc_rung_viewer": {
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.rungs.get(rungs.rungs.size() - 1).table = table;
        layoutNodes(rungs.rungs.get(rungs.rungs.size()-1).root, table, client);
        break;
      }
    }
    return table;
  }
  private static Component getAutoScroll(CellRow v, Container container, WebUIClient client) {
    //auto scroll components are placed below the main table
    ClientContext context = (ClientContext)client.getProperty("context");
    String name = v.name;
    ScrollPanel panel = new ScrollPanel();
    JFLog.log("client.height=" + client.getHeight());
    panel.setHeight(client.getHeight() - (cellHeight * 2));
    client.addResizedListener((cmp, width, height) -> {
      panel.setHeight(client.getHeight() - (cellHeight * 2));
    });
    switch (name) {
      case "jfc_rungs_viewer": {
        int fid = (Integer)client.getProperty("func");
        RungRow data[] = Database.getRungsById(fid, true);
        client.setProperty("rungs", new Rungs());
        context.debug_en_idx = 0;
        context.debug_tv_idx = 0;
        for(int rung=0;rung<data.length;rung++) {
          ArrayList<CellRow> cells = new ArrayList<CellRow>();
          cells.add(createCell(0, 0, 1, 1, "table", "jfc_rung_viewer", null, null, null, Integer.toString(data[rung].rid), null));
          Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new CellRow[cells.size()]), client, -1, -1, null);
          panel.add(table);
        }
        ArrayList<CellRow> cells = new ArrayList<CellRow>();
        cells.add(createCell(0, 0, 1, 1, "table", "jfc_rung_viewer_end", null, null, null, null, null));
        Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new CellRow[cells.size()]), client, -1, -1, null);
        panel.add(table);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.panel = panel;
        break;
      }
      case "jfc_rung_editor": {
        int fid = (Integer)client.getProperty("func");
        int rid = (Integer)client.getProperty("rung");
        ArrayList<CellRow> cells = new ArrayList<CellRow>();
        RungRow data = Database.getRungById(fid, rid);
        ArrayList<Node> nodes = new ArrayList<Node>();
        Rung rung = buildRung(data, cells, nodes, client, false, fid);
        if (rung == null) {
          rung = new Rung();
          rung.root = new NodeRoot(fid, rid);
        }
        client.setProperty("rungObj", rung);
        Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), container, cells.toArray(new CellRow[cells.size()]), client, -1, -1, nodes.toArray(new Node[nodes.size()]));
        layoutNodes(rung.root, table, client);
        table.setName(name + "_table");
        panel.add(table);
        break;
      }
      case "jfc_alarms": {
        //view current alarms
        context.alarms.clear();
        updateAlarms(panel, client);
        TagBase tag = context.getTag("alarms");
        context.addListener(tag, panel, false, (_tag, _oldValue, _newValue, _cmp) -> {
          updateAlarms(panel, panel.getClient());
        });
        break;
      }
      case "jfc_alarm_history": {
        //view alarm history
        context.lastAlarmID = 0;
        updateAlarmHistory(panel, client);
        TagBase tag = context.getTag("alarms");
        context.addListener(tag, panel, false, (_tag, _oldValue, _newValue, _cmp) -> {
          updateAlarmHistory(panel, panel.getClient());
        });
        client.setProperty("history", panel);
        break;
      }
    }
    return panel;
  }
  private static Component getLight(CellRow v, ClientContext context) {
    String style = v.style;
    if (style == null) style = "";
    String ss[] = style.split(";");
    String c0 = "ff0000";
    String c1 = "00ff00";
    for(int a=0;a<ss.length;a++) {
      String s = ss[a];
      int idx = s.indexOf("=");
      if (idx == -1) continue;
      String key = s.substring(0, idx);
      String value = s.substring(idx + 1);
      switch (key) {
        case "0": c0 = value; break;
        case "1": c1 = value; break;
      }
    }
    TagBase tag = context.getTag(v.tag);
    boolean state = false;
    if (tag != null) {
      state = !tag.getValue().equals("0");
    }
    Light light = new Light(Integer.valueOf(c0, 16),Integer.valueOf(c1, 16), state);
    light.addClickListener((me, c) -> {
      Events.click(c);
    });
    light.addMouseDownListener((c) -> {
      Events.press(c);
    });
    light.addMouseUpListener((c) -> {
      Events.release(c);
    });
    return light;
  }
  private static Component getLight3(CellRow v, ClientContext context) {
    String style = v.style;
    if (style == null) style = "";
    String ss[] = style.split(";");
    String c0 = "ff0000";
    String c1 = "00ff00";
    String cn = "333333";
    for(int a=0;a<ss.length;a++) {
      String s = ss[a];
      int idx = s.indexOf("=");
      if (idx == -1) continue;
      String key = s.substring(0, idx);
      String value = s.substring(idx + 1);
      switch (key) {
        case "0": c0 = value; break;
        case "1": c1 = value; break;
        case "n": cn = value; break;
      }
    }
    Light3 light = new Light3(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16), Integer.valueOf(cn, 16), context.getTagInt(v.tag));
    light.addClickListener((me, c) -> {
      Events.click(c);
    });
    light.addMouseDownListener((c) -> {
      Events.press(c);
    });
    light.addMouseUpListener((c) -> {
      Events.release(c);
    });
    return light;
  }
  private static Component getProgressBar(CellRow v, ClientContext context) {
    String style = v.style;
    if (style == null) style = "";
    String ss[] = style.split(";");
    String c0 = "ff0000";
    String c1 = "00ff00";
    String c2 = "333333";
    String or = "h";
    String v0 = "5";
    String v1 = "10";
    String v2 = "100.0";
    for(int a=0;a<ss.length;a++) {
      String s = ss[a];
      int idx = s.indexOf("=");
      if (idx == -1) continue;
      String key = s.substring(0, idx);
      String value = s.substring(idx + 1);
      switch (key) {
        case "0": c0 = value; break;
        case "1": c1 = value; break;
        case "2": c2 = value; break;
        case "o": or = value; break;
        case "v0": v0 = value; break;
        case "v1": v1 = value; break;
        case "v2": v2 = value; break;
      }
    }
    int dir = or.equals("h") ? ProgressBar.HORIZONTAL : ProgressBar.VERTICAL;
    ProgressBar pb = new ProgressBar(dir, Float.valueOf(v2), 32);
    pb.setLevels(Float.valueOf(v0), Float.valueOf(v1), Float.valueOf(v2));
    pb.setColors(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16), Integer.valueOf(c2, 16));
    TagBase tag = context.getTag(v.tag);
    if (tag != null) {
      pb.setValue(Float.valueOf(tag.getValue()));
    }
    return pb;
  }
  private static void updateAlarmCount(Label label, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    String count = Integer.toString(FunctionRuntime.alarm_active_count());
    label.setText(count);
    if (count.equals("0")) {
      label.setBackColor(Color.green);
    } else {
      label.setBackColor(Color.red);
    }
    if (!FunctionService.isActive()) return;
    boolean unack = FunctionRuntime.alarm_not_ack();
    if (!count.equals("0") && unack) {
      if (!context.alarmActive) {
        client.sendEvent("body", "audio-alarm-start", null);
        context.alarmActive = true;
      }
    } else {
      if (context.alarmActive) {
        client.sendEvent("body", "audio-alarm-stop", null);
        context.alarmActive = false;
      }
    }
  }
  private static void updateAlarms(Panel panel, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    TagBase alarms = TagsService.getTag("alarms");
    int length = alarms.getLength();
    for(int idx=0;idx<length;idx++) {
      TagBase fields[] = alarms.getFields(idx);
      boolean alarmActive = fields[IDs.fid_alarm_active].getBoolean();
      if (!alarmActive) {
        //not active
        if (context.alarms.containsKey(idx)) {
          //remove inactived alarm
          Table table = (Table)context.alarms.remove(idx);
          panel.remove(table);
        }
        continue;
      }
      if (context.alarms.containsKey(idx)) continue;
      ArrayList<CellRow> cells = new ArrayList<CellRow>();
      cells.add(createCell(0, 0, 1, 1, "table", "jfc_alarm", null, null, null, Integer.toString(idx), null));
      Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new CellRow[cells.size()]), client, -1, -1, null);
      panel.add(table);
      context.alarms.put(Integer.toString(idx), table);
    }
  }
  private static final long ms_per_day = 24 * 60 * 60 * 1000;
  private static void updateAlarmHistory(Panel panel, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    long start = cal.getTimeInMillis();
    long end = start + ms_per_day;
    AlarmRow data[] = Database.getAlarms(start, end);
    for(int a=0;a<data.length;a++) {
      int id = Integer.valueOf(data[a].id);
      if (id < context.lastAlarmID) continue;
      context.lastAlarmID = id;
      ArrayList<CellRow> cells = new ArrayList<CellRow>();
      cells.add(createCell(0, 0, 1, 1, "table", "jfc_alarm_history", null, null, null, Integer.toString(data[a].id), null));
      Table table = buildTable(new Table(cellWidth, cellHeight, 1, 1), null, cells.toArray(new CellRow[cells.size()]), panel.getClient(), -1, -1, null);
      panel.add(table);
    }
  }
  private static Component getOverlay(CellRow v, boolean topRow) {
    Block div = new Block();
    div.setBorder(true);
    div.setBorderColor(topRow ? Color.grey : Color.black);
    div.addClickListener((me, comp) -> {
      WebUIClient client = comp.getClient();
      Block focus = (Block)client.getProperty("focus");
      if (focus != null) {
        Rectangle rect = (Rectangle)focus.getProperty("rect");
        focus.setBorderColor(rect.y == 0 ? Color.grey : Color.black);
      }
      comp.setBorderColor(Color.green);
      client.setProperty("focus", comp);
    });
    return div;
  }
  public static Component getOverlay(int x,int y) {
    Component c = getOverlay(null, y == 0);
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
    int pid = (Integer)client.getProperty("panel");
    CellRow cell = Database.getCell(pid, fr.x, fr.y);
    cell.x = fr.x + deltax;
    cell.y = fr.y + deltay;
    Database.saveCellTable(Database.getPanelById(pid).name);
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
    //remove from jfc_src pos
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
    int pid = (Integer)client.getProperty("panel");
    CellRow cell = Database.getCell(pid, fr.x, fr.y);
    cell.w = fr.width + deltax;
    cell.h = fr.height + deltay;
    Database.saveCellTable(Database.getPanelById(pid).name);
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
          tbl.add(c, x, y);
          tbl.setSpans(x, y, r.width, r.height);
          setCellSize(c, r);
        }
      }
    }
  }
  public static Rung buildRung(RungRow rungobj, ArrayList<CellRow> cells, ArrayList<Node> objs, WebUIClient client, boolean readonly, int fid) {
    ClientContext context = (ClientContext)client.getProperty("context");
    int x = 0;
    int y = 0;
    int rid = rungobj.rid;
    Rung rung = new Rung();
    String logic = rungobj.logic;
    String comment = rungobj.comment;
    String parts[] = logic.split("[|]");
    BlockRow blocks[] = Database.getRungBlocksById(fid, rid);
    ArrayList<Node> nodes = new ArrayList<Node>();
    NodeRoot root = new NodeRoot(fid, rid);

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
            JFLog.log("Error:corrupt logic (a)");
            return null;
          }
          x = upper.x;
          y = upper.getSegmentMaxY(node) + 1;
          nodes.add(node = node.insertLinkUpper(upper, 'a', x, y));
          break;
        }
        case "b": {
          //b can only be under t,b
          Node upper = Node.findFirstOpenNode(nodes, "tb");
          if (upper == null) {
            JFLog.log("Error:corrupt logic (b)");
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
            JFLog.log("Error:corrupt logic (c)");
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
            JFLog.log("Error:corrupt logic (d)");
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
          int bid = Integer.valueOf(part);
          for(int a=0;a<blocks.length;a++) {
            if (blocks[a].bid == bid) {
              name = blocks[a].name;
              tags = blocks[a].tags;
              break;
            }
          }
          if (name == null) {
            JFLog.log("Error:Block not found:rid=" + rid + ":bid=" + part);
            continue;
          }
          LogicBlock blk = null;
          try {
            Class<?> cls = Class.forName("jfcontrols.logic." + name.toUpperCase());
            Constructor ctor = cls.getConstructor();
            blk = (LogicBlock)ctor.newInstance();
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
    logic.remove(node.comp);
    node.x = x;
    node.y = y;
    logic.add(node.comp, x, y, spanx, 1);
    node.root.changed = true;
  }
  public static void buildNodes(NodeRoot root, Table logic, ArrayList<CellRow> newCells, ArrayList<Node> newNodes, WebUIClient client, int rid, boolean readonly) {
    ClientContext context = (ClientContext)client.getProperty("context");
    int x = 0;
    int y = 1;
    Node node = root.next;
    JFLog.log("buildNodes");
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
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_h", null, null, null, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y) {
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
            if (child.x != x || child.y != y) {
              moveNode(logic, child, x, y, 1);
            }
            y++;
          }
          while (y < y2) {
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_v", null, null, null, null));
            newNodes.add(node.addChild('v', x, y));
            y++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_" + node.type, null, null, null, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y) {
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
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_h", null, null, null, null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (x > x2) {
            JFLog.log("Error:buildNodes() failed to adjust upper node (see Node.adjustX())");
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_" + node.type, null, null, null, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y) {
              moveNode(logic, node, x, y, 1);
            }
          }
          y--;
          cnt = node.childs.size();
          for(int a=0;a<cnt;a++) {
            child = node.childs.get(a);
            if (child.x != x || child.y != y) {
              moveNode(logic, child, x, y, 1);
            }
            y--;
          }
          while (y > node.upper.y) {
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_v", null, null, null, null));
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
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_h", null, null, null, null));
            newNodes.add(node.insertPreNode('h', x, y));
            x++;
          }
          if (create) {
            node.x = x;
            node.y = y;
            newCells.add(createCell(x, y, 1, 1, "image", null, "w_t", null, null, null, null));
            newNodes.add(node);
          } else {
            if (node.x != x || node.y != y) {
              moveNode(logic, node, x, y, 1);
            }
          }
          x++;
          break;
        case '#': {
          //create cells for block
          //id,name,tags
          LogicBlock blk = node.blk;
          childIdx = 0;
          int tagIdx = 1;
          if (!blk.isBlock()) {
            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", "en_0_" + context.debug_en_idx, "w_h", null, null, null, null));
              newNodes.add(node.addChild('h', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
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
                if (child.x != x || child.y != y) {
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
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 3);
                }
              }
              tagIdx++;

              y -= 2;
              x++;
            }

            x++;

            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", "en_1_" + context.debug_en_idx++, "w_h", null, null, null, null));
              newNodes.add(node.addChild('h', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
                moveNode(logic, child, x, y, 1);
              }
            }

            x--;

            if (create) {
              node.x = x;
              node.y = y;
              newCells.add(createCell(x, y, 1, 1, "image", null, blk.getImage(), null, null, null, null));
              newNodes.add(node);
            } else {
              if (node.x != x || node.y != y) {
                moveNode(logic, node, x, y, 1);
              }
            }
            x += 2;

          } else {

            int bx = x;
            int by = y;
            //draw a box the size of the logic block
            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", "en_0_" + context.debug_en_idx, "b7", null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            if (create) {
              newCells.add(createCell(x, y, 3, 1, "label", null, blk.getDesc(), null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
                moveNode(logic, child, x, y, 3);
              }
            }

            for(int a=0;a<3;a++) {
              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, "b8", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
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
                newCells.add(createCell(x, y, 1, 1, "image", null, "b4", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              y++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, "b4", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
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
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x++;

              String tag = node.tags[tagIdx].substring(1);
              if (create) {
                if (blk.getTagType(a) == TagType.function) {
                  if (readonly) {
                    FunctionRow fnc = Database.getFunctionById(Integer.valueOf(tag));
                    tag = fnc.name;
                  }
                  newCells.add(createCell(x, y, 3, 1, combobox, "jfc_function", tag, null, null, "jfc_function", style));
                  newNodes.add(node.addChild('C', x, y));
                } else {
                  newCells.add(createCell(x, y, 3, 1, textfield, "tag_" + context.debug_tv_idx, tag, null, null, null, style));
                  newNodes.add(node.addChild('T', x, y));
                }
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
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
                  if (child.x != x || child.y != y) {
                    moveNode(logic, child, x, y, 3);
                  }
                }
              }
              y--;
              x += 3;
              tagIdx++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, "b6", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              y++;

              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, "b6", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 1);
                }
              }
              x -= 4; y++;
            }

            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", null, "b1", null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
              child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
                moveNode(logic, child, x, y, 1);
              }
            }
            x++;

            for(int a=0;a<3;a++) {
              if (create) {
                newCells.add(createCell(x, y, 1, 1, "image", null, "b2", null, null, null, null));
                newNodes.add(node.addChild('x', x, y));
              } else {
                child = node.childs.get(childIdx++);
                if (child.x != x || child.y != y) {
                  moveNode(logic, child, x, y, 1);
                }
                child = child.next;
              }
              x++;
            }

            if (create) {
              newCells.add(createCell(x, y, 1, 1, "image", null, "b3", null, null, null, null));
              newNodes.add(node.addChild('x', x, y));
            } else {
                child = node.childs.get(childIdx++);
              if (child.x != x || child.y != y) {
                moveNode(logic, child, x, y, 1);
              }
            }
            y = by;

            if (create) {
              node.x = x;
              node.y = y;
              newCells.add(createCell(x, y, 1, 1, "image", "en_1_" + context.debug_en_idx++, "b9", null, null, null, null));
              newNodes.add(node);
            } else {
              if (node.x != x || node.y != y) {
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
    JFLog.log("layoutNodes");
    do {
      root.changed = false;
      ArrayList<CellRow> newCells = new ArrayList<CellRow>();
      ArrayList<Node> newNodes = new ArrayList<Node>();
      buildNodes(root, logic, newCells, newNodes, client, root.rid, false);
      buildTable(logic, null, newCells.toArray(new CellRow[newCells.size()]), client, -1, -1, newNodes.toArray(new Node[newNodes.size()]));
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
