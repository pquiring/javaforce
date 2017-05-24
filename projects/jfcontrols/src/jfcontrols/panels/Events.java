package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import java.util.*;
import javaforce.*;
import javaforce.webui.*;

import jfcontrols.functions.*;
import jfcontrols.sql.*;
import jfcontrols.tags.*;
import jfcontrols.logic.*;

public class Events {
  private static final Object lock = new Object();
  //button clicked
  public static void click(Component c) {
    WebUIClient client = c.getClient();
    String func = (String)c.getProperty("func");
    String arg = (String)c.getProperty("arg");
    JFLog.log("click:" + c + ":func=" + func + ":arg=" + arg);
    clickEvents(c);
    if (func == null) return;
    SQL sql = SQLService.getSQL();
    switch (func) {
      case "showMenu": {
        if (client.getProperty("user") == null) {
          PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_login");
          panel.setVisible(true);
        } else {
          PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_menu");
          panel.setVisible(true);
        }
        break;
      }
      case "jfc_logout": {
        client.setProperty("user", null);
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_menu");
        panel.setVisible(false);
        break;
      }
      case "jfc_login_ok": {
        String user = ((TextField)client.getPanel().getComponent("user")).getText();
        String pass = ((TextField)client.getPanel().getComponent("pass")).getText();
        JFLog.log("user/pass=" + user + "," + pass);
        String data[][] = sql.select("select name,pass from users");
        boolean ok = false;
        for(int a=0;a<data.length;a++) {
          JFLog.log("user/pass=" + data[a][0] + "," + data[a][1]);
          if (user.equals(data[a][0]) && pass.equals(data[a][1])) {
            client.setProperty("user", user);
            ok = true;
            break;
          }
        }
        if (!ok) {
          Label lbl = (Label)client.getPanel().getComponent("errmsg");
          lbl.setText("Invalid username or password!");
          break;
        }
        //no break
      }
      case "jfc_login_cancel": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_login");
        panel.setVisible(false);
        break;
      }
      case "jfc_ctrl_new": {
        //find available ctrl id
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select cid from ctrls where cid=" + id);
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into ctrls (cid,ip,type,speed) values (" + id + ",'',0,0)");
          client.setPanel(Panels.getPanel("jfc_controllers", client));
        }
        break;
      }
      case "jfc_ctrl_delete": {
        //TODO : check if in use
        String inuse = sql.select1value("select count(tags) from blocks where tags like '%,c" + arg + "#%'");
        if (!inuse.equals("0")) {
          Panels.error(client, "Can not delete controller that is in use!");
          break;
        }
        sql.execute("delete from ctrls where cid=" + arg);
        client.setPanel(Panels.getPanel("jfc_controllers", client));
        break;
      }
      case "jfc_ctrl_save": {
        //TODO : force a reload of config options
        break;
      }
      case "jfc_ctrl_tags": {
        //load tags for controller
        client.setProperty("ctrl", arg);
        client.setPanel(Panels.getPanel("jfc_tags", client));
        break;
      }
      case "jfc_tags_new": {
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select name from tags where name='tag" + id + "' and cid=" + client.getProperty("ctrl"));
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into tags (cid,name,type) values (" + client.getProperty("ctrl") + ",'tag" + id + "',0)");
          client.setPanel(Panels.getPanel("jfc_tags", client));
        }
        break;
      }
      case "jfc_tags_delete": {
        break;
      }
      case "jfc_tags_save": {
        break;
      }
      case "jfc_panels_new": {
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select name from panels where name='panel" + id + "'");
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into panels (name, popup, builtin) values ('panel" + id + "', false, false)");
        }
        client.setPanel(Panels.getPanel("jfc_panels", client));
        break;
      }
      case "jfc_panels_edit": {
        client.setProperty("panel", arg);
        client.setProperty("focus", null);
        client.setPanel(Panels.getPanel("jfc_panel_editor", client));
        break;
      }
      case "jfc_panels_delete": {
        break;
      }
      case "jfc_panel_editor_add": {
        ComboBox cb = (ComboBox)client.getPanel().getComponent("panel_type");
        String type = cb.getSelectedText();
        Block focus = (Block)client.getProperty("focus");
        if (focus == null) break;
        Rectangle r = (Rectangle)focus.getProperty("rect");
        Rectangle nr = new Rectangle(r);
        Table t1 = (Table)client.getPanel().getComponent("t1");  //components
        if (t1.get(r.x, r.y, false) != null) break;  //something already there
        Component nc = null;
        String text = null;
        switch (type) {
          case "label": text = "label"; nc = new Label(text); break;
          case "button": text = "button"; nc = new Button(text); break;
        }
        if (nc == null) break;
        Panels.setCellSize(nc, nr);
        t1.add(nc, r.x, r.y);
        String pid = (String)client.getProperty("panel");
        sql.execute("insert into cells (pid,x,y,w,h,comp,text) values (" + pid + "," + r.x + "," + r.y + ",1,1," + SQL.quote(type) + "," + SQL.quote(text) + ")");
        break;
      }
      case "jfc_panel_editor_del": {
        Block focus = (Block)client.getProperty("focus");
        if (focus == null) break;
        Rectangle r = (Rectangle)focus.getProperty("rect");
        Table t1 = (Table)client.getPanel().getComponent("t1");  //components
        Component comp = t1.get(r.x, r.y, false);
        if (comp == null) break;
        t1.remove(r.x, r.y);
        String pid = (String)client.getProperty("panel");
        sql.execute("delete from cells where pid=" + pid + " and x=" + r.x + " and y=" + r.y);
        break;
      }
      case "jfc_panel_editor_props": {
        Component focus = (Component)client.getProperty("focus");
        if (focus == null) break;
        Rectangle r = (Rectangle)focus.getProperty("rect");
        String pid = (String)client.getProperty("panel");
        String events = sql.select1value("select events from cells where x=" + r.x + " and y=" + r.y + " and pid=" + pid);
        String tag = sql.select1value("select tag from cells where x=" + r.x + " and y=" + r.y + " and pid=" + pid);
        if (tag == null) tag = "";
        String text = sql.select1value("select text from cells where x=" + r.x + " and y=" + r.y + " and pid=" + pid);
        if (text == null) text = "";
        TextField textTF = (TextField)client.getPanel().getComponent("text");
        TextField tagTF = (TextField)client.getPanel().getComponent("tag");
        TextField pressTF = (TextField)client.getPanel().getComponent("press");
        TextField releaseTF = (TextField)client.getPanel().getComponent("release");
        TextField clickTF = (TextField)client.getPanel().getComponent("click");
        String press = "", release = "", click = "";
        if (events != null) {
          String parts[] = events.split("[|]");
          for(int a=0;a<parts.length;a++) {
            String part = parts[a];
            int idx = part.indexOf("=");
            if (idx == -1) continue;
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            switch (key) {
              case "press": press = value; break;
              case "release": release = value; break;
              case "click": click = value; break;
            }
          }
        }
        textTF.setText(text);
        tagTF.setText(tag);
        pressTF.setText(press);
        releaseTF.setText(release);
        clickTF.setText(click);
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_panel_props");
        panel.setVisible(true);
        break;
      }
      case "jfc_panel_props_ok": {
        Component focus = (Component)client.getProperty("focus");
        if (focus != null) {
          TextField textTF = (TextField)client.getPanel().getComponent("text");
          String text = textTF.getText();
          if (text.indexOf("|") != -1) {
            setFocus(textTF);
            break;
          }
          TextField tagTF = (TextField)client.getPanel().getComponent("tag");
          String tag = tagTF.getText();
          if (tag.indexOf("|") != -1) {
            setFocus(tagTF);
            break;
          }
          TextField pressTF = (TextField)client.getPanel().getComponent("press");
          String press = pressTF.getText();
          if (press.indexOf("|") != -1) {
            setFocus(pressTF);
            break;
          }
          TextField releaseTF = (TextField)client.getPanel().getComponent("release");
          String release = releaseTF.getText();
          if (release.indexOf("|") != -1) {
            setFocus(releaseTF);
            break;
          }
          TextField clickTF = (TextField)client.getPanel().getComponent("click");
          String click = clickTF.getText();
          if (click.indexOf("|") != -1) {
            setFocus(clickTF);
            break;
          }
          Rectangle r = (Rectangle)focus.getProperty("rect");
          Table t1 = (Table)client.getPanel().getComponent("t1");  //components
          Component comp = t1.get(r.x, r.y, false);
          if (comp instanceof Button) {
            ((Button)comp).setText(text);
          }
          if (comp instanceof Label) {
            ((Label)comp).setText(text);
          }
          String events = "press=" + press + "|release=" + release + "|click=" + click;
          String pid = (String)client.getProperty("panel");
          sql.execute("update cells set events=" + SQL.quote(events) + ",tag=" + SQL.quote(tag) + ",text=" + SQL.quote(text) + " where x=" + r.x + " and y=" + r.y + " and pid=" + pid);
        }
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_panel_props");
        panel.setVisible(false);
        break;
      }
      case "jfc_panel_props_cancel": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_panel_props");
        panel.setVisible(false);
        break;
      }
      case "jfc_panel_editor_move_u": {
        Panels.moveCell(client, 0, -1, sql);
        break;
      }
      case "jfc_panel_editor_move_d": {
        Panels.moveCell(client, 0, +1, sql);
        break;
      }
      case "jfc_panel_editor_move_l": {
        Panels.moveCell(client, -1, 0, sql);
        break;
      }
      case "jfc_panel_editor_move_r": {
        Panels.moveCell(client, +1, 0, sql);
        break;
      }

      case "jfc_panel_editor_size_w_inc": {
        Panels.resizeCell(client, +1, 0, sql);
        break;
      }
      case "jfc_panel_editor_size_w_dec": {
        Panels.resizeCell(client, -1, 0, sql);
        break;
      }
      case "jfc_panel_editor_size_h_inc": {
        Panels.resizeCell(client, 0, +1, sql);
        break;
      }
      case "jfc_panel_editor_size_h_dec": {
        Panels.resizeCell(client, 0, -1, sql);
        break;
      }

      case "jfc_funcs_new": {
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select name from funcs where name='func" + id + "'");
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into funcs (name) values ('func" + id + "')");
        }
        client.setPanel(Panels.getPanel("jfc_funcs", client));
        break;
      }
      case "jfc_funcs_edit": {
        JFLog.log("func=" + arg);
        client.setProperty("func", arg);
        client.setProperty("focus", null);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }
      case "jfc_funcs_delete": {
        String inuse = sql.select1value("select count(id) from blocks where name='CALL' and tags='," + arg + ",'");
        if (!inuse.equals("0")) {
          Panels.error(client, "Can not delete function that is in use");
          break;
        }
        //TODO : confirm action
        sql.execute("delete from funcs where id=" + arg);
        sql.execute("delete from rungs where fid=" + arg);
        sql.execute("delete from blocks where fid=" + arg);
        client.setPanel(Panels.getPanel("jfc_funcs", client));
        break;
      }

      case "jfc_func_editor_add_rung": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        Component focus = (Component)client.getProperty("focus");
        int idx = 0;
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node != null) {
          if (node.parent == null) {
            idx = node.root.rid;
          } else {
            idx = node.parent.root.rid;
          }
        }
        //insert rung before current one
        sql.execute("update rungs set rid=rid+1 where fid=" + fid + " and rid>=" + idx);
        sql.execute("insert into rungs (fid,rid,comment,logic) values (" + fid + "," + idx + ",'Comment','h')");
        ArrayList<String[]> cells = new ArrayList<String[]>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + idx);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.rungs.add(idx, Panels.buildRung(data, cells, nodes, sql, true, fid));
        Table table = Panels.buildTable(new Table(Panels.cellWidth, Panels.cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, 0, 0, null, sql);
        rungs.table.add(idx, table);
        break;
      }

      case "jfc_func_editor_edit_rung": {
        Component focus = (Component)client.getProperty("focus");
        int idx = 0;
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node != null) {
          if (node.parent == null) {
            idx = node.root.rid;
          } else {
            idx = node.parent.root.rid;
          }
        }
        client.setProperty("rung", Integer.toString(idx));
        client.setPanel(Panels.getPanel("jfc_rung_editor", client));
        break;
      }

      case "jfc_rung_editor_add": {
        Component focus = (Component)client.getProperty("focus");
        if (focus == null) {
          JFLog.log("Error:focus == null");
          break;
        }
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node == null) {
          JFLog.log("Error:Node not found");
          break;
        }
        int x = 0, y = 0;
        if (node.parent != null) {
          node = node.parent;
        }
        x = node.x + node.getDelta();
        y = node.y;
        String name = c.getName();
        Logic blk = null;
        try {
          Class cls = Class.forName("jfcontrols.logic." + name.replaceAll(" ", "_").toUpperCase());
          blk = (Logic)cls.newInstance();
        } catch (Exception e) {
          JFLog.log(e);
        }
        if (blk == null) {
          JFLog.log("Error:Logic not found:" + name);
          break;
        }
        if (node.type != 'h') {
          node = node.insertNode('h', x, y);
        }
        String tags[] = new String[blk.getTagsCount() + 1];
        for(int a=0;a<tags.length;a++) {
          tags[a] = "0";
        }
        node = node.insertLogic('#', x, y, blk, tags);
        if (node.next == null || !node.next.validFork()) {
          node = node.insertNode('h', x, y);
        }
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor");
        Panels.layoutNodes(node.root, logic, sql);
        break;
      }

      case "jfc_rung_editor_del": {
        Component focus = (Component)client.getProperty("focus");
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node == null) {
          JFLog.log("Error:Node not found");
          break;
        }
        if (node.parent != null) {
          node = node.parent;
        }
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor");
        node.delete(logic, sql);
        Panels.layoutNodes(node.root, logic, sql);
        break;
      }

      case "jfc_rung_editor_fork": {
        Node fork = (Node)client.getProperty("fork");
        if (fork != null) {
          fork.forkCancel(client);
          break;
        }
        Component focus = (Component)client.getProperty("focus");
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node == null) {
          JFLog.log("Error:Node not found");
          break;
        }
        if (node.parent != null) {
          node = node.parent;
        }
        node.forkSource(client);
        break;
      }

      case "jfc_rung_editor_save": {
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor");
        String fid = (String)client.getProperty("func");
        String rid = (String)client.getProperty("rung");
        Component comp = logic.get(0, 0, false);
        NodeRoot root = ((Node)comp.getProperty("node")).root;
        if (!root.isValid(client)) {
          break;
        }
        sql.execute("delete from blocks where fid=" + fid + " and rid=" + rid);
        String str = root.saveLogic(sql);
        JFLog.log("logic=" + str);
        sql.execute("update rungs set logic='" + str + "' where rid=" + rid + " and fid=" + fid);
        //recompile logic
        FunctionService.generateFunction(Integer.valueOf(fid), sql);
        FunctionService.compileProgram(sql);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }

      case "setPanel":
        Panel panel = Panels.getPanel(arg, client);
        if (panel != null) {
          client.setPanel(panel);
        }
        break;
      default:
        //TODO : support plugin events
        JFLog.log("Unknown event:" + func);
        break;
    }
    sql.close();
  }
  //textfield edited
  public static void edit(TextField tf) {
    WebUIClient client = tf.getClient();
    String red = (String)tf.getProperty("red");
    if (red != null) {
      tf.setBackColor("#fff");
      tf.setProperty("red", null);
    }
    String tag = (String)tf.getProperty("tag");
    if (tag == null) return;
    String value = tf.getText();
    if (tag.startsWith("jfc_")) {
      String f[] = tag.split("_");
      //jfc_table_col_id
      String table = f[1];
      String col = f[2];
      String type = f[3];
      String id = f[4];
      SQL sql = SQLService.getSQL();
      sql.execute("update " + table + " set " + col + "=" + SQLService.quote(value, type) + " where id=" + id);
      sql.close();
    } else {
      TagsService.write(tag, tf.getText());
    }
  }
  //combobox changed
  public static void changed(ComboBox cb) {
    WebUIClient client = cb.getClient();
    String tag = (String)cb.getProperty("tag");
    if (tag == null) return;
    String value = cb.getSelectedValue();
    if (tag.startsWith("jfc_")) {
      String f[] = tag.split("_");
      //jfc_table_col_id
      String table = f[1];
      String col = f[2];
      String type = f[3];
      String id = f[4];
      SQL sql = SQLService.getSQL();
      sql.execute("update " + table + " set " + col + "=" + SQLService.quote(value, type) + " where id=" + id);
      sql.close();
    } else {
      TagsService.write(tag, value);
    }
  }
  public static void clickEvents(Component c) {
    String events = (String)c.getProperty("events");
    if (events == null) return;
    String parts[] = events.split("[|]");
    String click = "";
    for(int a=0;a<parts.length;a++) {
      String part = parts[a];
      int idx = part.indexOf("=");
      if (idx == -1) continue;
      String key = part.substring(0, idx);
      String value = part.substring(idx + 1);
      switch (key) {
//        case "press": press = value; break;
//        case "release": release = value; break;
        case "click": click = value; break;
      }
    }
    String cmds[] = click.split(";");
    for(int a=0;a<cmds.length;a++) {
      String cmd_args_ = cmds[a].trim();
      if (cmd_args_.length() == 0) continue;
      int i1 = cmd_args_.indexOf("(");
      int i2 = cmd_args_.indexOf(")");
      if (i1 == -1 || i2 == -1) continue;
      String cmd = cmd_args_.substring(0, i1);
      String args[] = cmd_args_.substring(i1+1, i2).split(",");
      doCommand(cmd, args);
    }
  }
  public static void press(Component c) {
    String events = (String)c.getProperty("events");
    if (events == null) return;
    String parts[] = events.split("[|]");
    String press = "";
    for(int a=0;a<parts.length;a++) {
      String part = parts[a];
      int idx = part.indexOf("=");
      if (idx == -1) continue;
      String key = part.substring(0, idx);
      String value = part.substring(idx + 1);
      switch (key) {
        case "press": press = value; break;
//        case "release": release = value; break;
//        case "click": click = value; break;
      }
    }
    String cmds[] = press.split(";");
    for(int a=0;a<cmds.length;a++) {
      String cmd_args_ = cmds[a].trim();
      int i1 = cmd_args_.indexOf("(");
      int i2 = cmd_args_.indexOf(")");
      String cmd = cmd_args_.substring(0, i1);
      String args[] = cmd_args_.substring(i1+1, i2).split(",");
      doCommand(cmd, args);
    }
  }
  public static void release(Component c) {
    String events = (String)c.getProperty("events");
    if (events == null) return;
    String parts[] = events.split("[|]");
    String release = "";
    for(int a=0;a<parts.length;a++) {
      String part = parts[a];
      int idx = part.indexOf("=");
      if (idx == -1) continue;
      String key = part.substring(0, idx);
      String value = part.substring(idx + 1);
      switch (key) {
//        case "press": press = value; break;
        case "release": release = value; break;
//        case "click": click = value; break;
      }
    }
    String cmds[] = release.split(";");
    for(int a=0;a<cmds.length;a++) {
      String cmd_args_ = cmds[a].trim();
      int i1 = cmd_args_.indexOf("(");
      int i2 = cmd_args_.indexOf(")");
      String cmd = cmd_args_.substring(0, i1);
      String args[] = cmd_args_.substring(i1+1, i2).split(",");
      doCommand(cmd, args);
    }
  }
  public static void doCommand(String cmd, String args[]) {
    //TODO : these commands need to be processed by the FunctionService thread - in between scan cycles
    JFLog.log("cmd=" + cmd);
    switch (cmd) {
      case "toggleBit": {
        Tag tag = TagsService.getTag(args[0]);
        if (tag != null) {
          tag.setBoolean(!tag.getBoolean());
        }
        break;
      }
      case "setBit": {
        Tag tag = TagsService.getTag(args[0]);
        if (tag != null) {
          tag.setBoolean(true);
        }
        break;
      }
      case "clearBit": {
        Tag tag = TagsService.getTag(args[0]);
        if (tag != null) {
          tag.setBoolean(false);
        }
        break;
      }
    }
  }
  public static void setFocus(TextField tf) {
    tf.setFocus();
    tf.setBackColor("#c00");
    tf.setProperty("red", "true");
  }
}
