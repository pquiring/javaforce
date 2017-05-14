package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;
import jfcontrols.tags.TagsService;

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
          client.setPanel(Panels.getPanel("jfc_panels", client));
        }
        break;
      }
      case "jfc_panels_edit": {
        client.setProperty("panel", arg);
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
        Panels.setCellSize(nc, r);

        Table t1 = (Table)client.getPanel().getComponent("t1");  //components
        t1.add(nc, r.x, r.y);
        Table t2 = (Table)client.getPanel().getComponent("t2");  //overlays

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
