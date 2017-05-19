package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import java.util.ArrayList;
import javaforce.*;
import javaforce.webui.*;

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
    if (func == null) return;
    SQL sql = SQLService.getSQL();
    switch (func) {
      case "showMenu": {
        if (client.getProperty("user") == null) {
          PopupPanel panel = (PopupPanel)client.root.getComponent("login_panel");
          panel.setVisible(true);
        } else {
          PopupPanel panel = (PopupPanel)client.root.getComponent("menu_panel");
          panel.setVisible(true);
        }
        break;
      }
      case "jfc_logout": {
        client.setProperty("user", null);
        PopupPanel panel = (PopupPanel)client.root.getComponent("menu_panel");
        panel.setVisible(false);
        break;
      }
      case "jfc_login_ok": {
        String user = ((TextField)client.root.getComponent("user")).getText();
        String pass = ((TextField)client.root.getComponent("pass")).getText();
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
          Label lbl = (Label)client.root.getComponent("errmsg");
          lbl.setText("Invalid username or password!");
          break;
        }
        //no break
      }
      case "jfc_login_cancel": {
        PopupPanel panel = (PopupPanel)client.root.getComponent("login_panel");
        panel.setVisible(false);
        break;
      }
      case "jfc_ctrl_new": {
        //find available ctrl id
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select num from ctrls where num=" + id);
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into ctrls (num,ip,type,speed) values (" + id + ",'',0,0)");
          client.setPanel(Panels.getPanel("jfc_controllers", client));
        }
        break;
      }
      case "jfc_ctrl_delete": {
        //TODO
        break;
      }
      case "jfc_ctrl_save": {
        //force a reload of config options
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
        JFLog.log("type=" + type);
        Component nc = null;
        switch (type) {
          case "label": nc = new Label("label"); break;
          case "button": nc = new Button("button"); break;
        }
        if (nc == null) break;
        Block focus = (Block)client.getProperty("focus");
        if (focus == null) break;
        Rectangle r = (Rectangle)focus.getProperty("rect");
        Rectangle nr = new Rectangle(r);
        Panels.setCellSize(nc, nr);
        Table t1 = (Table)client.getPanel().getComponent("t1");  //components
        t1.add(nc, r.x, r.y);
        break;
      }
      case "jfc_panel_editor_del": {
        break;
      }
      case "jfc_panel_editor_move_u": {
        Panels.moveCell(client, 0, -1);
        break;
      }
      case "jfc_panel_editor_move_d": {
        Panels.moveCell(client, 0, +1);
        break;
      }
      case "jfc_panel_editor_move_l": {
        Panels.moveCell(client, -1, 0);
        break;
      }
      case "jfc_panel_editor_move_r": {
        Panels.moveCell(client, +1, 0);
        break;
      }

      case "jfc_panel_editor_size_w_inc": {
        Panels.resizeCell(client, +1, 0);
        break;
      }
      case "jfc_panel_editor_size_w_dec": {
        Panels.resizeCell(client, -1, 0);
        break;
      }
      case "jfc_panel_editor_size_h_inc": {
        Panels.resizeCell(client, 0, +1);
        break;
      }
      case "jfc_panel_editor_size_h_dec": {
        Panels.resizeCell(client, 0, -1);
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
        client.setProperty("func", arg);
        client.setProperty("focus", null);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }
      case "jfc_funcs_delete": {
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
          if (node.ref == null) {
            idx = node.root.rid;
          } else {
            idx = node.ref.root.rid;
          }
        }
        //insert rung before current one
        sql.execute("update rungs set rid=rid+1 where fid=" + fid + " and rid>=" + idx);
        sql.execute("insert into rungs (fid,rid,comment,logic) values (" + fid + "," + idx + ",'','h')");
        ArrayList<String[]> cells = new ArrayList<String[]>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + idx);
        Rungs rungs = (Rungs)client.getProperty("rungs");
        rungs.rungs.add(idx, Panels.buildRung(data, cells, nodes, sql, true));
        Table table = Panels.buildTable(new Table(Panels.cellWidth, Panels.cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, 0, 0, null);
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
          if (node.ref == null) {
            idx = node.root.rid;
          } else {
            idx = node.ref.root.rid;
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
        if (node.ref != null) {
          node = node.ref;
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
        int rid = Integer.valueOf((String)client.getProperty("rung"));
        if (node.type != 'h') {
          node = node.insertNode('h', x, y);
        }
        node = node.insertLogic('#', x, y, "TODO", blk, "TODO");
        if (node.next == null || !node.next.validFork()) {
          node = node.insertNode('h', x, y);
        }
        Panels.layoutNodes(node.root, (Table)client.getPanel().getComponent("jfc_rung_editor"));
        break;
      }

      case "jfc_rung_editor_del": {
        Component focus = (Component)client.getProperty("focus");
        JFLog.log("TODO:delete:" + focus);
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node == null) {
          JFLog.log("Error:Node not found");
          break;
        }
        if (node.ref != null) {
          node = node.ref;
        }
        Panels.layoutNodes(node.root, (Table)client.getPanel().getComponent("jfc_rung_editor"));
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
        if (node.ref != null) {
          node = node.ref;
        }
        node.forkSource(client);
        break;
      }

      case "jfc_rung_editor_save": {
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor");
        int rid = Integer.valueOf((String)client.getProperty("rung"));
        Component comp = logic.get(0, 0, false);
        NodeRoot root = ((Node)comp.getProperty("node")).root;
        String str = root.saveLogic(false);
        JFLog.log("logic=" + str);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }

      case "setPanel":
        Panel panel = Panels.getPanel(arg, client);
        if (panel != null) {
          client.setPanel(panel);
        }
        break;
      case "toggleBit":
        break;
      case "setBit":
        break;
      case "clearBit":
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
      if (sql.lastException != null) {
        JFLog.log(sql.lastException);
      }
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
      if (sql.lastException != null) {
        JFLog.log(sql.lastException);
      }
      sql.close();
    } else {
      TagsService.write(tag, value);
    }
  }
}
