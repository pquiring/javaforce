package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.app.*;
import jfcontrols.functions.*;
import jfcontrols.sql.*;
import jfcontrols.tags.*;
import jfcontrols.logic.*;

public class Events {
  private static final Object lock = new Object();
  public static void click(Component c) {
    WebUIClient client = c.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String func = (String)c.getProperty("func");
    String arg = (String)c.getProperty("arg");
    JFLog.log("click:" + c + ":func=" + func + ":arg=" + arg);
    clickEvents(c);
    if (func == null) return;
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
      case "jfc_error_ok": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_error");
        panel.setVisible(false);
        break;
      }
      case "jfc_error_textarea_ok": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_error_textarea");
        panel.setVisible(false);
        break;
      }
      case "jfc_confirm_ok": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_confirm");
        panel.setVisible(false);
        String action = (String)client.getProperty("action");
        arg = (String)client.getProperty("arg");
        switch (action) {
          case "jfc_ctrl_delete": {
            sql.execute("delete from ctrls where cid=" + arg);
            client.setPanel(Panels.getPanel("jfc_controllers", client));
            break;
          }
          case "jfc_funcs_delete": {
            sql.execute("delete from funcs where id=" + arg);
            sql.execute("delete from rungs where fid=" + arg);
            sql.execute("delete from blocks where fid=" + arg);
            client.setPanel(Panels.getPanel("jfc_funcs", client));
            break;
          }
          case "jfc_func_editor_del_rung": {
            String fid = (String)client.getProperty("func");
            Component focus = (Component)client.getProperty("focus");
            int rid = -1;
            Node node = null;
            if (focus != null) {
              node = (Node)focus.getProperty("node");
            }
            if (node != null) {
              if (node.parent == null) {
                rid = node.root.rid;
              } else {
                rid = node.parent.root.rid;
              }
            }
            if (rid == -1) break;
            sql.execute("delete from rungs where fid=" + fid + " and rid=" + rid);
            sql.execute("delete from blocks where fid=" + fid + " and rid=" + rid);
            sql.execute("update rungs set rid=rid-1 where fid=" + fid + " and rid>" + rid);
            sql.execute("update blocks set rid=rid-1 where fid=" + fid + " and rid>" + rid);
            long revision = Long.valueOf(sql.select1value("select revision from funcs where id=" + fid));
            revision++;
            sql.execute("update funcs set revision=" + revision + " where id=" + fid);
            client.setPanel(Panels.getPanel("jfc_func_editor", client));
            break;
          }
          case "jfc_rung_editor_cancel": {
            client.setPanel(Panels.getPanel("jfc_func_editor", client));
            break;
          }
          case "jfc_panels_delete": {
            sql.execute("delete from panels where id=" + arg);
            client.setPanel(Panels.getPanel("jfc_panels", client));
            break;
          }
          case "jfc_tags_delete": {
            sql.execute("delete from tags where id=" + arg);
            client.setPanel(Panels.getPanel("jfc_tags", client));
            break;
          }
          case "jfc_watch_delete": {
            sql.execute("delete from watch where id=" + arg);
            client.setPanel(Panels.getPanel("jfc_tags", client));
            break;
          }
          case "jfc_udts_delete": {
            sql.execute("delete from udts where id=" + arg);
            sql.execute("delete from udtmems where uid=" + arg);
            client.setPanel(Panels.getPanel("jfc_udts", client));
            break;
          }
        }
        break;
      }
      case "jfc_confirm_cancel": {
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_confirm");
        panel.setVisible(false);
        break;
      }
      case "jfc_logout": {
        client.setProperty("user", null);
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_menu");
        panel.setVisible(false);
        client.setPanel(Panels.getPanel("main", client));
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
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_login");
        panel.setVisible(false);
        break;
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
        String inuse = sql.select1value("select count(tags) from blocks where tags like '%,c" + arg + "#%'");
        if (!inuse.equals("0")) {
          Panels.showError(client, "Can not delete controller that is in use!");
          break;
        }
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete controller?", "jfc_ctrl_delete");
        break;
      }
      case "jfc_ctrl_save": {
        Main.restart();
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
          sql.execute("insert into tags (cid,name,type,unsigned,array,builtin) values (" + client.getProperty("ctrl") + ",'tag" + id + "',1,false,false,false)");
          client.setPanel(Panels.getPanel("jfc_tags", client));
        }
        break;
      }
      case "jfc_tags_delete": {
        String cid = (String)client.getProperty("ctrl");
        String query;
        if (cid.equals("0")) {
          query = "select count(tags) from blocks where tags like '%,c0#" + arg + ",%' or tags like '%," + arg + ",%'";
        } else {
          query = "select count(tags) from blocks where tags like '%,c" + cid + "#" + arg + ",%'";
        }
        String inuse = sql.select1value(query);
        if (!inuse.equals("0")) {
          Panels.showError(client, "Can not delete tag that is in use!");
          break;
        }
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete tag?", "jfc_tags_delete");
        break;
      }
      case "jfc_tags_save": {
        Main.restart();
        JFLog.log("Restart complete");
        break;
      }
      case "jfc_tags_xref": {
        client.setProperty("xref", arg);
        client.setPanel(Panels.getPanel("jfc_xref", client));
        break;
      }
      case "jfc_xref_view_func": {
        client.setProperty("func", arg);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }
      case "jfc_xref_view_panel": {
        client.setProperty("panel", arg);
        client.setPanel(Panels.getPanel("jfc_panel_editor", client));
        break;
      }
      case "jfc_watch_new": {
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select name from watch where name='watch" + id + "'");
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into watch (name) values ('watch" + id + "')");
          client.setPanel(Panels.getPanel("jfc_watch", client));
        }
        break;
      }
      case "jfc_watch_delete": {
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete watch table?", "jfc_watch_delete");
        break;
      }
      case "jfc_watch_edit": {
        client.setProperty("watch", arg);
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        break;
      }
      case "jfc_watch_tags_new": {
        if (context.watch != null) break;
        String wid = (String)client.getProperty("watch");
        synchronized(lock) {
          int id = 1;
          do {
            String inuse = sql.select1value("select tag from watchtags where tag='tag" + id + "' and wid=" + wid);
            if (inuse == null) break;
            id++;
          } while (true);
          sql.execute("insert into watchtags (wid, tag) values (" + wid + ",'tag" + id + "')");
          client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        }
        break;
      }
      case "jfc_watch_tags_start": {
        if (context.watch != null) {
          context.watch.cancel();
          context.watch = null;
          Button btn = (Button)c;
          btn.setText("Start");
        } else {
          context.watch = new WatchContext();
          if (!context.watch.init(client)) {
            context.watch = null;
            break;
          }
          context.watch.start();
          Button btn = (Button)c;
          btn.setText("Stop");
        }
        break;
      }
      case "jfc_watch_tags_delete": {
        if (context.watch != null) break;
        sql.execute("delete from watchtags where id=" + arg);
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        break;
      }
      case "jfc_config_save": {
        int uid = IDs.uid_io;
        int idx = ((ComboBox)client.getPanel().getComponent("config_type")).getSelectedIndex();
        switch (idx) {
          case 0:  //none
            sql.execute("delete from udtmems where uid=" + uid);
            sql.execute("update config set value='' where id='hw_di'");
            sql.execute("update config set value='' where id='hw_do'");
            sql.execute("delete from iocomments");
            break;
          case 1:  //di8 do8
            sql.execute("insert into udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",0,'di',1,true,false,false)");
            sql.execute("insert into udtmems (uid,mid,name,type,array,unsigned,builtin) values (" + uid + ",1,'do',1,true,false,false)");
            sql.execute("update config set value='0,1,2,3,4,5,6,7' where id='hw_di'");
            sql.execute("update config set value='8,9,10,11,12,13,14,15' where id='hw_do'");
            for(int a=0;a<8;a++) {
              sql.execute("insert into iocomments (mid, idx, comment) values (" + IDs.io_mid_di + "," + a + ", '')");
              sql.execute("insert into iocomments (mid, idx, comment) values (" + IDs.io_mid_do + "," + a + ", '')");
            }
            break;
        }
        if (!JF.isWindows()) {
          //TODO:save IP config
        }
        Main.restart();
        break;
      }
      case "jfc_config_shutdown": {
        Main.stop();
        SQLService.stop();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText("Database Shutdown");
        break;
      }
      case "jfc_config_restart": {
        SQLService.restart();
        Main.restart();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText("System running");
        break;
      }
      case "jfc_panels_new": {
        synchronized(lock) {
          int id = 1;
          do {
            String inuse1 = sql.select1value("select name from panels where name='panel" + id + "'");
            String inuse2 = sql.select1value("select display from panels where display='panel" + id + "'");
            if (inuse1 == null && inuse2 == null) break;
            id++;
          } while (true);
          sql.execute("insert into panels (name, display, popup, builtin) values ('panel" + id + "', 'panel" + id + "', false, false)");
        }
        client.setPanel(Panels.getPanel("jfc_panels", client));
        break;
      }
      case "jfc_udts_new": {
        synchronized(lock) {
          int uid = IDs.uid_user;
          do {
            String inuse = sql.select1value("select id from udts where uid=" + uid + " or name='udt" + (uid-IDs.uid_user+1) + "'");
            if (inuse == null) break;
            uid++;
            if (uid == IDs.uid_user_end) {
              JFLog.log("Error:Too many UDTs");
              break;
            }
          } while (true);
          if (uid == IDs.uid_user_end) break;
          sql.execute("insert into udts (name, uid) values ('udt" + (uid-IDs.uid_user+1) + "', " + uid + ")");
        }
        client.setPanel(Panels.getPanel("jfc_udts", client));
        break;
      }
      case "jfc_udts_delete": {
        int uid = Integer.valueOf(sql.select1value("select uid from udts where id=" + arg));
        String name = sql.select1value("select name from udts where id=" + arg);
        if (uid == IDs.uid_alarms) {
          Panels.showError(client, "Can not delete alarms type");
          break;
        }
        String inuse = sql.select1value("select count(id) from tags where type=" + uid);
        if (!inuse.equals("0")) {
          Panels.showError(client, "Can not delete UDT that is in use!");
          break;
        }
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete UDT " + name + "?", "jfc_udts_delete");
        break;
      }
      case "jfc_udts_edit": {
        client.setProperty("udt", arg);
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }

      case "jfc_udt_editor_new": {
        String uid = (String)client.getProperty("udt");
        synchronized(lock) {
          int mid = 0;
          do {
            String inuse = sql.select1value("select name from udtmems where uid=" + uid + " and mid=" + mid);
            if (inuse == null) break;
            mid++;
          } while (true);
          sql.execute("insert into udtmems (name,uid,mid,type,unsigned,array,builtin) values ('member" + (mid+1) + "'," + uid + "," + mid + ",1,false,false,false)");
        }
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }

      case "jfc_udt_editor_delete": {
        String uid = sql.select1value("select uid from udtmems where id=" + arg);
        String inuse = sql.select1value("select count(id) from tags where type=" + uid);
        if (!inuse.equals("0")) {
          Panels.showError(client, "Can not delete member that is in use!");
          break;
        }
        sql.execute("delete from udtmems where mid=" + arg);
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }

      case "jfc_sdts_edit": {
        client.setProperty("udt", arg);
        client.setPanel(Panels.getPanel("jfc_sdt_editor", client));
        break;
      }

      case "jfc_panels_edit": {
        client.setProperty("panel", arg);
        client.setProperty("focus", null);
        client.setPanel(Panels.getPanel("jfc_panel_editor", client));
        break;
      }
      case "jfc_panels_delete": {
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete panel?", "jfc_panels_delete");
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
        if (r.width > 1 || r.height > 1) {
          Table t2 = (Table)client.getPanel().getComponent("t2");  //overlays
          t2.remove(r.x, r.y);
          int x2 = r.x + r.width - 1;
          int y2 = r.y + r.height - 1;
          for(int x=r.x;x<=x2;x++) {
            for(int y=r.y;y<=y2;y++) {
              t2.add(Panels.getOverlay(x,y), x, y);
            }
          }
        }
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
          sql.execute("insert into funcs (name, revision) values ('func" + id + "', 1)");
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
          Panels.showError(client, "Can not delete function that is in use");
          break;
        }
        Panels.confirm(client, "Delete function?", "jfc_funcs_delete");
        break;
      }

      case "jfc_func_editor_add_rung": {
        int fid = Integer.valueOf((String)client.getProperty("func"));
        Component focus = (Component)client.getProperty("focus");
        int rid = 0;
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node != null) {
          if (node.parent == null) {
            rid = node.root.rid;
          } else {
            rid = node.parent.root.rid;
          }
        }
        Rungs rungs = (Rungs)client.getProperty("rungs");
        if (rid == -1) {
          //insert at end
          rid = rungs.rungs.size();
        }
        //insert rung before current one
        JFLog.log("rid=" + rid);
        sql.execute("update rungs set rid=rid+1 where fid=" + fid + " and rid>=" + rid);
        sql.execute("update blocks set rid=rid+1 where fid=" + fid + " and rid>=" + rid);
        sql.execute("insert into rungs (fid,rid,comment,logic) values (" + fid + "," + rid + ",'Comment','h')");
        ArrayList<String[]> cells = new ArrayList<String[]>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        String data[] = sql.select1row("select rid,logic,comment from rungs where fid=" + fid + " and rid=" + rid);
        Rung rung = Panels.buildRung(data, cells, nodes, client, true, fid);
        rungs.rungs.add(rid, rung);
        Table table = Panels.buildTable(new Table(Panels.cellWidth, Panels.cellHeight, 1, 1), null, cells.toArray(new String[cells.size()][]), client, 0, 0, nodes.toArray(new Node[0]));
        rungs.panel.add(rid, table);
        rung.table = table;
        int cnt = rungs.rungs.size();
        for(int a=rid+1;a<cnt;a++) {
          Rung r = rungs.rungs.get(a);
          Label lbl = (Label)r.table.get(0, 0, false);
          lbl.setText("Rung " + (a+1));
        }
        long revision = Long.valueOf(sql.select1value("select revision from funcs where id=" + fid));
        revision++;
        sql.execute("update funcs set revision=" + revision + " where id=" + fid);
        break;
      }

      case "jfc_func_editor_del_rung": {
        Panels.confirm(client, "Delete rung?", "jfc_func_editor_del_rung");
        break;
      }

      case "jfc_func_editor_edit_rung": {
        Component focus = (Component)client.getProperty("focus");
        int rid = 0;
        Node node = null;
        if (focus != null) {
          node = (Node)focus.getProperty("node");
        }
        if (node != null) {
          if (node.parent == null) {
            rid = node.root.rid;
          } else {
            rid = node.parent.root.rid;
          }
        }
        if (rid == -1) break;
        client.setProperty("rung", Integer.toString(rid));
        client.setProperty("focus", null);
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
        NodeRoot root = node.root;
        if (root.hasSolo()) {
          Panels.showError(client, "Rung contains a solo component");
          break;
        }
        while (node != null && !node.validInsert()) {
          node = node.next;
        }
        if (node == null) {
          JFLog.log("Error:no valid node found");
          break;
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
        if (blk.isSolo()) {
          if (!root.isEmpty()) {
            Panels.showError(client, "Rung must be empty to add that component");
            break;
          }
        }
        if (blk.isLast()) {
          if (node.next != null) {
            Panels.showError(client, "That component must be last");
            break;
          }
          if (root.hasLast()) {
            Panels.showError(client, "Rung already has a last component");
            break;
          }
        }
        if (node.type != 'h') {
          node = node.insertNode('h', x, y);
        }
        String tags[] = new String[blk.getTagsCount() + 1];
        node = node.insertLogic('#', x, y, blk, tags);
        if (node.next == null || !node.next.validFork()) {
          node = node.insertNode('h', x, y);
        }
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor_table");
        Panels.layoutNodes(node.root, logic, client);
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
        Table logic = (Table)client.getPanel().getComponent("jfc_rung_editor_table");
        node.delete(logic, client);
        Panels.layoutNodes(node.root, logic, client);
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
        if (node.root.hasSolo()) {
          Panels.showError(client, "Can not fork with solo component");
          break;
        }
        node.forkSource(client);
        break;
      }

      case "jfc_rung_editor_cancel": {
        Panels.confirm(client, "Discard changes?", "jfc_rung_editor_cancel");
        break;
      }

      case "jfc_rung_editor_save": {
        String fid = (String)client.getProperty("func");
        String rid = (String)client.getProperty("rung");
        Rung rung = (Rung)client.getProperty("rungObj");
        NodeRoot root = rung.root;
        if (!root.isValid(client)) {
          break;
        }
        sql.execute("delete from blocks where fid=" + fid + " and rid=" + rid);
        String str = root.saveLogic(sql);
        JFLog.log("logic=" + str);
        if (str == null) {
          Panels.showError(client, "Failed to save!");
          break;
        }
        sql.execute("update rungs set logic='" + str + "' where rid=" + rid + " and fid=" + fid);
        TextField tf = (TextField)client.getPanel().getComponent("comment" + rid);
        String cmt = tf.getText();
        sql.execute("update rungs set comment=" + SQL.quote(cmt) + " where rid=" + rid + " and fid=" + fid);
        long revision = Long.valueOf(sql.select1value("select revision from funcs where id=" + fid));
        revision++;
        sql.execute("update funcs set revision=" + revision + " where id=" + fid);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }

      case "jfc_func_editor_compile": {
        String fid = (String)client.getProperty("func");
        synchronized(lock) {
          if (!FunctionService.generateFunction(Integer.valueOf(fid), sql)) {
            Panels.showErrorText(client, "Compile failed!", FunctionCompiler.error);
            FunctionCompiler.error = null;
            break;
          }
          if (!FunctionService.compileProgram(sql)) {
            Panels.showErrorText(client, "Compile failed!", FunctionService.error);
            FunctionService.error = null;
          }
        }
        break;
      }

      case "jfc_func_editor_debug": {
        Button b = (Button)c;
        if (context.debug != null) {
          context.debug.cancel();
          context.debug = null;
          b.setText("Debug");
          break;
        }
        String fid = (String)client.getProperty("func");
        long revision = Long.valueOf(sql.select1value("select revision from funcs where id=" + fid));
        if (FunctionService.functionUpToDate(Integer.valueOf(fid), revision)) {
          context.debug = new DebugContext(client, Integer.valueOf(fid));
          context.debug.start();
          b.setText("Stop");
        } else {
          Panels.showError(client, "Function needs to be compiled!");
        }
        break;
      }

      case "jfc_alarm_editor_new": {
        synchronized(lock) {
          String tid = sql.select1value("select id from tags where name='alarms'");
          int idx = 0;
          do {
            String inuse = sql.select1value("select value from tagvalues where tid=" + tid + " and idx=" + idx);
            if (inuse == null) break;
            idx++;
          } while (true);
          sql.execute("insert into tagvalues (tid,idx,mid,midx,value) values (" + tid + "," + idx + ",0,0,'alarm" + idx + "')");
        }
        client.setPanel(Panels.getPanel("jfc_alarm_editor", client));
        break;
      }

      case "jfc_alarm_editor_del": {
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
  }
  public static void edit(TextField tf) {
    WebUIClient client = tf.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String red = (String)tf.getProperty("red");
    if (red != null) {
      tf.setBackColor("#fff");
      tf.setProperty("red", null);
    }
    String tag = (String)tf.getProperty("tag");
    if (tag == null) return;
    String value = tf.getText();
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
      if (type.equals("tagid")) {
        if (!TagsService.validTagName(value)) {
          tf.setBackColor("#c00");
          tf.setProperty("red", "true");
          return;
        }
      }
      if (type.equals("tag")) {
        if (context.decode(value) == null) {
          tf.setBackColor("#c00");
          tf.setProperty("red", "true");
          return;
        }
      }
      String stmt = "update " + table + " set " + col + "=" + SQLService.quote(value, type) + " where id=" + id;
      sql.execute(stmt);
      if (sql.lastException != null) {
        String org = sql.select1value("select " + col + " from " + table + " where id=" + id);
        tf.setText(org);
      }
    } else {
      context.write(tag, tf.getText());
    }
  }
  public static void edit(TextArea ta) {
    WebUIClient client = ta.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String red = (String)ta.getProperty("red");
    if (red != null) {
      ta.setBackColor("#fff");
      ta.setProperty("red", null);
    }
    String tag = (String)ta.getProperty("tag");
    if (tag == null) return;
    String value = ta.getText();
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
      String stmt = "update " + table + " set " + col + "=" + SQLService.quote(value, type) + " where id=" + id;
      sql.execute(stmt);
    } else {
      context.write(tag, ta.getText());
    }
  }
  public static void changed(ComboBox cb) {
    WebUIClient client = cb.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tag = (String)cb.getProperty("tag");
    if (tag == null) return;
    String value = cb.getSelectedValue();
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
      sql.execute("update " + table + " set " + col + "=" + SQLService.quote(value, type) + " where id=" + id);
    } else {
      context.write(tag, value);
    }
  }
  public static void changed(CheckBox cb) {
    WebUIClient client = cb.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    SQL sql = context.sql;
    String tag = (String)cb.getProperty("tag");
    if (tag == null) return;
    boolean set = cb.isSelected();
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
      sql.execute("update " + table + " set " + col + "=" + (set ? "true" : "false") + " where id=" + id);
    } else {
      context.write(tag, set ? "0" : "1");
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
      doCommand(cmd, args, c.getClient());
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
      doCommand(cmd, args, c.getClient());
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
      doCommand(cmd, args, c.getClient());
    }
  }
  public static void doCommand(String cmd, String args[], WebUIClient client) {
    //TODO : these commands need to be processed by the FunctionService thread - in between scan cycles
    ClientContext context = (ClientContext)client.getProperty("context");
    switch (cmd) {
      case "toggleBit": {
        TagAddr ta = context.decode(args[0]);
        TagBase tag = context.getTag(ta);
        if (tag != null) {
          tag.setValue(tag.getValue().equals("0") ? "1" : "0");
        }
        break;
      }
      case "setBit": {
        TagAddr ta = context.decode(args[0]);
        TagBase tag = context.getTag(ta);
        if (tag != null) {
          tag.setValue("1");
        }
        break;
      }
      case "resetBit": {
        TagAddr ta = context.decode(args[0]);
        TagBase tag = context.getTag(ta);
        if (tag != null) {
          tag.setValue("0");
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
