package jfcontrols.panels;

/** Events
 *
 * @author pquiring
 */

import java.util.*;
import java.lang.reflect.*;

import javaforce.*;
import javaforce.webui.*;
import javaforce.webui.event.*;
import javaforce.controls.*;

import jfcontrols.app.*;
import jfcontrols.functions.*;
import jfcontrols.images.*;
import jfcontrols.db.*;
import jfcontrols.tags.*;
import jfcontrols.logic.*;

public class Events {
  private static final Object lock = new Object();
  public static void click(Component c) {
    WebUIClient client = c.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    String func = (String)c.getProperty("func");
    String arg = (String)c.getProperty("arg");
    JFLog.log("click:" + c + ":func=" + func + ":arg=" + arg + ":" + c + ":" + c.hashCode());
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
            int id = Integer.valueOf(arg);
            Database.deleteControllerById(id);
            client.setPanel(Panels.getPanel("jfc_controllers", client));
            break;
          }
          case "jfc_funcs_delete": {
            int id = Integer.valueOf(arg);
            Database.deleteFunctionById(id);
            Database.deleteRungsById(id);
            Database.deleteBlocksById(id);
            FunctionService.deleteFunction(id);
            client.setPanel(Panels.getPanel("jfc_funcs", client));
            break;
          }
          case "jfc_func_editor_del_rung": {
            int fid = (Integer)client.getProperty("func");
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
            Database.deleteRungById(fid, rid);
            FunctionRow fnc = Database.getFunctionById(fid);
            fnc.revision++;
            Database.funcs.save();
            client.setPanel(Panels.getPanel("jfc_func_editor", client));
            break;
          }
          case "jfc_rung_editor_cancel": {
            client.setPanel(Panels.getPanel("jfc_func_editor", client));
            break;
          }
          case "jfc_panels_delete": {
            Database.deletePanelById(Integer.valueOf(arg));
            client.setPanel(Panels.getPanel("jfc_panels", client));
            break;
          }
          case "jfc_tags_delete": {
            int id = Integer.valueOf(arg);
            TagsService.deleteTag(id);
            Database.deleteTag(id);
            client.setPanel(Panels.getPanel("jfc_tags", client));
            break;
          }
          case "jfc_watch_delete": {
            int wid = (Integer)client.getProperty("watch");
            Database.deleteWatchTagById(wid, Integer.valueOf(arg));
            client.setPanel(Panels.getPanel("jfc_watch", client));
            break;
          }
          case "jfc_udts_delete": {
            Database.deleteUDTById(Integer.valueOf(arg));
            Database.deleteUDTMembersById(Integer.valueOf(arg));
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
        client.setPanel(Panels.getPanel("usr_Main", client));
        break;
      }
      case "jfc_login_ok": {
        String user = ((TextField)client.getPanel().getComponent("user")).getText();
        String pass = ((TextField)client.getPanel().getComponent("password")).getText();
        UserRow data[] = Database.users.getRows().toArray(new UserRow[0]);
        boolean ok = false;
        for(int a=0;a<data.length;a++) {
          if (user.equals(data[a].name) && pass.equals(data[a].pass)) {
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
        ((TextField)panel.getComponent("user")).setText("");
        ((TextField)panel.getComponent("pass")).setText("");
        panel.setVisible(false);
        break;
      }
      case "jfc_ctrl_new": {
        //find available ctrl id
        ControllerRow[] ctrls = Database.controllers.getRows().toArray(new ControllerRow[0]);
        synchronized(lock) {
          int cid = 1;
          for(int a=0;a<ctrls.length;a++) {
            if (ctrls[a].cid == cid) {
              cid++;
              a = -1;
            }
          }
          Database.addController(cid, "", 0, 0);
          client.setPanel(Panels.getPanel("jfc_controllers", client));
        }
        break;
      }
      case "jfc_ctrl_delete": {
        if (Database.isControllerInUse(Integer.valueOf(arg))) {
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
        JFLog.log("controller id=" + arg);
        client.setProperty("ctrl", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_tags", client));
        break;
      }
      case "jfc_tags_new": {
        int id = Integer.valueOf(client.getProperty("ctrl").toString());
        int cid = Database.getControllerCID(id);
        String tag_type = cid == 0 ? "tag_udt" : "tag";
        ComboBox cbType = (ComboBox)client.getPanel().getComponent(tag_type + "_type");
        clear(cbType, 0);
        CheckBox array = (CheckBox)client.getPanel().getComponent(tag_type + "_array");
        clear(array, false);
        TextField arraysize = (TextField)client.getPanel().getComponent(tag_type + "_arraysize");
        clear(arraysize, "0");
        TextField name = (TextField)client.getPanel().getComponent(tag_type + "_name");
        clear(name, "");
        PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_new_" + tag_type);
        panel.setVisible(true);
        break;
      }
      case "jfc_tag_new_ok_udt":
      case "jfc_tag_new_ok":
      {
        synchronized(lock) {
          int id = Integer.valueOf(client.getProperty("ctrl").toString());
          int cid = Database.getControllerCID(id);
          int type = 0;
          int idx = 0;
          String tag_type = cid == 0 ? "tag_udt" : "tag";
          ComboBox cbType = (ComboBox)client.getPanel().getComponent(tag_type + "_type");
          idx = cbType.getSelectedIndex();
          CheckBox isarray = (CheckBox)client.getPanel().getComponent(tag_type + "_array");
          boolean is_array = isarray.isSelected();
          TextField arraysize = (TextField)client.getPanel().getComponent(tag_type + "_arraysize");
          int array_size = Integer.valueOf(arraysize.getText());
          TextField name = (TextField)client.getPanel().getComponent(tag_type + "_name");
          String tag_name = name.getText();
          clearError(name);
          if (!TagsService.validTagName(cid, tag_name)) {
            setError(name);
            return;
          }
          TagRow exists = Database.getTagByName(cid, tag_name);
          if (exists != null) {
            setError(name);
            return;
          }
          clearError(arraysize);
          if (is_array) {
            if (array_size < 1 || array_size > 65536) {
              setError(arraysize);
              return;
            }
          }
          if (cid == 0) {
            PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_new_tag_udt");
            panel.setVisible(false);
          } else {
            PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_new_tag");
            panel.setVisible(false);
          }
          type = Integer.valueOf(cbType.getSelectedValue());
          String comment = "";  //edit later
          Database.addTag(cid, tag_name, type, array_size, false);
          TagBase tag = TagsService.createTag(null, cid, id, type, is_array ? array_size : 0, tag_name, comment);
          client.setPanel(Panels.getPanel("jfc_tags", client));  //force update
        }
        break;
      }
      case "jfc_tag_new_cancel":
      case "jfc_tag_new_cancel_udt":
      {
        int id = Integer.valueOf(client.getProperty("ctrl").toString());
        int cid = Database.getControllerCID(id);
        if (cid == 0) {
          PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_new_tag_udt");
          panel.setVisible(false);
        } else {
          PopupPanel panel = (PopupPanel)client.getPanel().getComponent("jfc_new_tag");
          panel.setVisible(false);
        }
        break;
      }
      case "jfc_tags_delete": {
        int id = Integer.valueOf(client.getProperty("ctrl").toString());
        int cid = Database.getControllerCID(id);
        if (Database.isTagInUse(cid, arg)) {
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
        client.setProperty("xref", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_xref", client));
        break;
      }
      case "jfc_xref_view_func": {
        client.setProperty("func", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }
      case "jfc_xref_view_panel": {
        client.setProperty("panel", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_panel_editor", client));
        break;
      }
      case "jfc_watch_new": {
        Database.addWatchTable("New Watch Table");
        client.setPanel(Panels.getPanel("jfc_watch", client));
        break;
      }
      case "jfc_watch_delete": {
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete watch table?", "jfc_watch_delete");
        break;
      }
      case "jfc_watch_edit": {
        client.setProperty("watch", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        break;
      }
      case "jfc_watch_tags_new": {
        if (context.watch != null) break;
        int wid = (Integer)client.getProperty("watch");
        Database.addWatchTag(wid, arg);
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        break;
      }
      case "jfc_watch_tag_delete": {
        if (context.watch != null) break;
        int wid = (Integer)client.getProperty("watch");
        Database.deleteWatchTagById(wid, Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
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
        int wid = (Integer)client.getProperty("watch");
        Database.deleteWatchTagById(wid, Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_watch_tags", client));
        break;
      }
      case "jfc_config_password": {
        Panel panel = client.getPanel();
        ((TextField)panel.getComponent("jfc_password_old")).setText("");
        ((TextField)panel.getComponent("jfc_password_new")).setText("");
        ((TextField)panel.getComponent("jfc_password_confirm")).setText("");
        panel.getComponent("jfc_change_password").setVisible(true);
        break;
      }
      case "jfc_change_password_ok": {
        String user = (String)client.getProperty("user");
        Panel panel = client.getPanel();
        panel.getComponent("jfc_change_password").setVisible(false);
        String old = ((TextField)panel.getComponent("jfc_password_old")).getText();
        String newpw = ((TextField)panel.getComponent("jfc_password_new")).getText();
        String cfmpw = ((TextField)panel.getComponent("jfc_password_confirm")).getText();
        UserRow userobj = Database.getUser(user);
        if (!userobj.pass.equals(old)) {
          Panels.showError(client, "Wrong current password");
          break;
        }
        if (!newpw.equals(cfmpw)) {
          Panels.showError(client, "Passwords do not match");
          break;
        }
        if (newpw.length() < 4) {
          Panels.showError(client, "Password too short");
          break;
        }
        userobj.pass = newpw;
        Database.users.save();
        Panels.showError(client, "Password changed!");
        break;
      }
      case "jfc_change_password_cancel": {
        Panel panel = client.getPanel();
        panel.getComponent("jfc_change_password").setVisible(false);
        break;
      }
      case "jfc_config_shutdown": {
        Main.stop();
        Database.stop();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText("Database Shutdown");
        break;
      }
      case "jfc_config_restart": {
        Main.restart();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText("System running");
        break;
      }
      case "jfc_config_backup": {
        Main.stop();
        String msg = Database.backup();
        Main.start();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText(msg);
        break;
      }
      case "jfc_config_restore": {
        ComboBox cb = (ComboBox)client.getPanel().getComponent("backups");
        String filename = cb.getSelectedText();
        Main.stop();
        String msg = Database.restore(Paths.backupPath + "/" + filename);
        Main.start();
        Label lbl = (Label)client.getPanel().getComponent("jfc_config_status");
        lbl.setText(msg);
        break;
      }
      case "jfc_panels_new": {
        synchronized(lock) {
          //BUG : panel name should be unique
          String name = "usr_NewPanel";
          int pid = Database.addPanel(name, false, false);
          Database.addCellTable(name, pid).save();
        }
        client.setPanel(Panels.getPanel("jfc_panels", client));
        break;
      }
      case "jfc_udts_new": {
        synchronized(lock) {
          Database.addUDT("New UDT");
        }
        client.setPanel(Panels.getPanel("jfc_udts", client));
        break;
      }
      case "jfc_udts_delete": {
        int uid = Integer.valueOf(arg);
        if (uid == IDs.uid_alarms) {
          Panels.showError(client, "Can not delete alarms type");
          break;
        }
        if (Database.isUDTInUse(uid)) {
          Panels.showError(client, "Can not delete UDT that is in use!");
          break;
        }
        client.setProperty("arg", arg);
        UDT udt = Database.getUDTById(uid);
        Panels.confirm(client, "Delete UDT " + udt.name + "?", "jfc_udts_delete");
        break;
      }
      case "jfc_udts_edit": {
        client.setProperty("udt", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }

      case "jfc_udt_editor_new": {
        int uid = (Integer)client.getProperty("udt");
        synchronized(lock) {
          int mid = 0;
          do {
            UDTMember member = Database.getUDTMemberById(uid, mid);
            if (member == null) break;
            mid++;
          } while (true);
          Database.addUDTMember(uid, mid, "NewMember" + (mid+1), 0, 0, false);
        }
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }

      case "jfc_udt_editor_delete": {
        int uid = (Integer)client.getProperty("udt");
        int mid = Integer.valueOf(arg);
        if (Database.isUDTInUse(uid)) {
          Panels.showError(client, "Can not delete member that is in use!");
          break;
        }
        Database.deleteUDTMemberById(uid, mid);
        client.setPanel(Panels.getPanel("jfc_udt_editor", client));
        break;
      }
      case "jfc_udt_editor_save": {
        //TODO
        break;
      }

      case "jfc_sdts_edit": {
        client.setProperty("udt", Integer.valueOf(arg));
        client.setPanel(Panels.getPanel("jfc_sdt_editor", client));
        break;
      }

      case "jfc_panels_edit": {
        client.setProperty("panel", Integer.valueOf(arg));
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
        String text = "new";
        String style = "";
        switch (type) {
          case "label": text = "label"; nc = new Label(text); break;
          case "button": text = "button"; nc = new Button(text); break;
          case "light": style = "0=ff0000;1=00ff00"; nc = new Light(Color.red,Color.green, false); break;
          case "light3": style = "0=ff0000;1=00ff00;n=333333"; nc = new Light3(Color.red, Color.green, Color.lightGrey, 0); break;
          case "togglebutton": style = "0=ff0000;1=00ff00"; nc = new ToggleButton(text, Color.red, Color.green); break;
          case "progressbar": style = "o=h;0=ff0000;1=ffff00;2=00ff00;v0=5;v1=10;max=100.0"; nc = new ProgressBar(ProgressBar.HORIZONTAL, 100.0f, 32); break;
          case "image": text = "image"; nc = new Image(Images.getImage(text)); break;
        }
        if (nc == null) break;
        Panels.setCellSize(nc, nr);
        t1.add(nc, r.x, r.y);
        int pid = (Integer)client.getProperty("panel");
        javaforce.db.Table celltable = Database.getCellTableById(pid);
        celltable.add(new CellRow(pid,r.x,r.y,1,1,type,text,"").setStyle(style));
        celltable.save();
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
        int pid = (Integer)client.getProperty("panel");
        Database.deleteCell(pid, r.x, r.y);
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
        Table t1 = (Table)client.getPanel().getComponent("t1");  //components
        Component comp = t1.get(r.x, r.y, false);
        if (comp == null) break;
        String type = getComponentType(comp);
        int pid = (Integer)client.getProperty("panel");
        CellRow cell = Database.getCell(pid, r.x, r.y);
        if (cell == null) {
          JFLog.log("Error:no cell found @" + pid + ":" + r.x + "," + r.y);
          return;
        }
        String events = cell.events;
        String tag = cell.tag;
        if (tag == null) tag = "";
        String text = cell.text;
        if (text == null) text = "";
        String style = cell.style;
        if (style == null) style = "";
        Panel panel = client.getPanel();

        Label textLbl = (Label)panel.getComponent("textLbl");
        TextField textTF = (TextField)panel.getComponent("text");
        Label c0Lbl = (Label)panel.getComponent("c0Lbl");
        Light c0L = (Light)panel.getComponent("c0");
        Label c1Lbl = (Label)panel.getComponent("c1Lbl");
        Light c1L = (Light)panel.getComponent("c1");
        Label cnLbl = (Label)panel.getComponent("cnLbl");
        Light cnL = (Light)panel.getComponent("cn");

        Label v0Lbl = (Label)panel.getComponent("v0Lbl");
        TextField v0TF = (TextField)panel.getComponent("v0");
        Label v1Lbl = (Label)panel.getComponent("v1Lbl");
        TextField v1TF = (TextField)panel.getComponent("v1");
        Label v2Lbl = (Label)panel.getComponent("v2Lbl");
        TextField v2TF = (TextField)panel.getComponent("v2");

        Label dir = (Label)panel.getComponent("dir");
        ToggleButton h = (ToggleButton)panel.getComponent("h");
        ToggleButton v = (ToggleButton)panel.getComponent("v");

        TextField tagTF = (TextField)panel.getComponent("tag");
        TextField pressTF = (TextField)panel.getComponent("press");
        TextField releaseTF = (TextField)panel.getComponent("release");
        TextField clickTF = (TextField)panel.getComponent("click");
        String press = "", release = "", click = "";
        String c0 = "", c1 = "", cn = "";
        String o = "";
        String v0 = "", v1 = "", v2 = "";
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
        if (style != null) {
          String parts[] = style.split(";");
          for(int a=0;a<parts.length;a++) {
            String part = parts[a];
            int idx = part.indexOf("=");
            if (idx == -1) continue;
            String key = part.substring(0, idx);
            String value = part.substring(idx + 1);
            switch (key) {
              case "0": c0 = value; break;
              case "1": c1 = value; break;
              case "n": cn = value; break;
              case "2": cn = value; break;
              case "o": o = value; break;
              case "v0": v0 = value; break;
              case "v1": v1 = value; break;
              case "v2": v2 = value; break;
            }
          }
        }
        textTF.setText(text);
        tagTF.setText(tag);
        pressTF.setText(press);
        releaseTF.setText(release);
        clickTF.setText(click);
        if (c0.length() > 0) c0L.setBackColor(Integer.valueOf(c0,16));
        if (c1.length() > 0) c1L.setBackColor(Integer.valueOf(c1,16));
        if (cn.length() > 0) cnL.setBackColor(Integer.valueOf(cn,16));
        if (o.length() > 0) {
          if (o.equals("h")) {
            h.setSelected(true);
            v.setSelected(false);
          } else {
            v.setSelected(true);
            h.setSelected(false);
          }
        }
        if (v0.length() > 0) v0TF.setText(v0);
        if (v1.length() > 0) v1TF.setText(v1);
        if (v2.length() > 0) v2TF.setText(v2);
        textLbl.setText("text");
        JFLog.log("type=" + type);
        switch (type) {
          case "button":
          case "label":
            textTF.setVisible(true);
            textLbl.setVisible(true);
            c0L.setVisible(false);
            c0Lbl.setVisible(false);
            c0Lbl.setText("0");
            c1L.setVisible(false);
            c1Lbl.setVisible(false);
            cnL.setVisible(false);
            cnLbl.setVisible(false);
            v0Lbl.setVisible(false);
            v0TF.setVisible(false);
            v1Lbl.setVisible(false);
            v1TF.setVisible(false);
            v2Lbl.setVisible(false);
            v2TF.setVisible(false);
            dir.setVisible(false);
            h.setVisible(false);
            v.setVisible(false);
            break;
          case "light":
            textTF.setVisible(false);
            textLbl.setVisible(false);
            c0L.setVisible(true);
            c0Lbl.setVisible(true);
            c0Lbl.setText("0");
            c1L.setVisible(true);
            c1Lbl.setVisible(true);
            c1Lbl.setText("1");
            cnL.setVisible(false);
            cnLbl.setVisible(false);
            v0Lbl.setVisible(false);
            v0TF.setVisible(false);
            v1Lbl.setVisible(false);
            v1TF.setVisible(false);
            v2Lbl.setVisible(false);
            v2TF.setVisible(false);
            dir.setVisible(false);
            h.setVisible(false);
            v.setVisible(false);
            break;
          case "light3":
            textTF.setVisible(false);
            textLbl.setVisible(false);
            c0L.setVisible(true);
            c0Lbl.setVisible(true);
            c0Lbl.setText("0");
            c1L.setVisible(true);
            c1Lbl.setVisible(true);
            c1Lbl.setText("+");
            cnL.setVisible(true);
            cnLbl.setVisible(true);
            cnLbl.setText("-");
            v0Lbl.setVisible(false);
            v0TF.setVisible(false);
            v1Lbl.setVisible(false);
            v1TF.setVisible(false);
            v2Lbl.setVisible(false);
            v2TF.setVisible(false);
            dir.setVisible(false);
            h.setVisible(false);
            v.setVisible(false);
            break;
          case "togglebutton":
            textTF.setVisible(true);
            textLbl.setVisible(true);
            c0L.setVisible(true);
            c0Lbl.setVisible(true);
            c0Lbl.setText("0");
            c1L.setVisible(true);
            c1Lbl.setVisible(true);
            c1Lbl.setText("1");
            cnL.setVisible(false);
            cnLbl.setVisible(false);
            v0Lbl.setVisible(false);
            v0TF.setVisible(false);
            v1Lbl.setVisible(false);
            v1TF.setVisible(false);
            v2Lbl.setVisible(false);
            v2TF.setVisible(false);
            dir.setVisible(false);
            h.setVisible(false);
            v.setVisible(false);
            break;
          case "progressbar":
            textTF.setVisible(false);
            textLbl.setVisible(false);
            c0L.setVisible(true);
            c0Lbl.setVisible(true);
            c0Lbl.setText("Low");
            c1L.setVisible(true);
            c1Lbl.setVisible(true);
            c1Lbl.setText("Mid");
            cnL.setVisible(true);
            cnLbl.setVisible(true);
            cnLbl.setText("High");
            v0Lbl.setVisible(true);
            v0TF.setVisible(true);
            v1Lbl.setVisible(true);
            v1TF.setVisible(true);
            v2Lbl.setVisible(true);
            v2TF.setVisible(true);
            dir.setVisible(true);
            h.setVisible(true);
            v.setVisible(true);
            break;
          case "image":
            textLbl.setText("image");  //TODO : create a combobox
            textTF.setVisible(true);
            textLbl.setVisible(true);
            c0L.setVisible(false);
            c0Lbl.setVisible(false);
            c1L.setVisible(false);
            c1Lbl.setVisible(false);
            cnL.setVisible(false);
            cnLbl.setVisible(false);
            v0Lbl.setVisible(false);
            v0TF.setVisible(false);
            v1Lbl.setVisible(false);
            v1TF.setVisible(false);
            v2Lbl.setVisible(false);
            v2TF.setVisible(false);
            dir.setVisible(false);
            h.setVisible(false);
            v.setVisible(false);
            break;
          default:
            JFLog.log("Error:unknown panel type:" + type);
            break;
        }
        PopupPanel props = (PopupPanel)client.getPanel().getComponent("jfc_panel_props");
        props.setVisible(true);
        break;
      }
      case "jfc_panel_props_c0": {
        Light light = (Light)client.getPanel().getComponent("c0");
        client.setProperty("light", light);
        ColorChooserPopup color = (ColorChooserPopup)client.getPanel().getComponent("colorpanel");
        color.setValue(light.getBackColor());
        color.setVisible(true);
        break;
      }
      case "jfc_panel_props_c1": {
        Light light = (Light)client.getPanel().getComponent("c1");
        client.setProperty("light", light);
        ColorChooserPopup color = (ColorChooserPopup)client.getPanel().getComponent("colorpanel");
        color.setValue(light.getBackColor());
        color.setVisible(true);
        break;
      }
      case "jfc_panel_props_cn": {
        Light light = (Light)client.getPanel().getComponent("cn");
        client.setProperty("light", light);
        ColorChooserPopup color = (ColorChooserPopup)client.getPanel().getComponent("colorpanel");
        color.setValue(light.getBackColor());
        color.setVisible(true);
        break;
      }
      case "jfc_panel_props_h": {
        Panel panel = client.getPanel();
        ToggleButton hTB = (ToggleButton)panel.getComponent("h");
        ToggleButton vTB = (ToggleButton)panel.getComponent("v");
        boolean state = hTB.isSelected();
        vTB.setSelected(!state);
        break;
      }
      case "jfc_panel_props_v": {
        Panel panel = client.getPanel();
        ToggleButton hTB = (ToggleButton)panel.getComponent("h");
        ToggleButton vTB = (ToggleButton)panel.getComponent("v");
        boolean state = vTB.isSelected();
        hTB.setSelected(!state);
        break;
      }
      case "jfc_panel_props_ok": {
        Panel panel = client.getPanel();
        Component focus = (Component)client.getProperty("focus");
        if (focus != null) {
          TextField textTF = (TextField)panel.getComponent("text");
          String text = textTF.getText();
          if (text.indexOf("|") != -1) {
            setError(textTF);
            break;
          }
          TextField tagTF = (TextField)panel.getComponent("tag");
          String tag = tagTF.getText();
          if (tag.indexOf("|") != -1) {
            setError(tagTF);
            break;
          }
          TextField pressTF = (TextField)panel.getComponent("press");
          String press = pressTF.getText();
          if (press.indexOf("|") != -1) {
            setError(pressTF);
            break;
          }
          TextField releaseTF = (TextField)panel.getComponent("release");
          String release = releaseTF.getText();
          if (release.indexOf("|") != -1) {
            setError(releaseTF);
            break;
          }
          TextField clickTF = (TextField)panel.getComponent("click");
          String click = clickTF.getText();
          if (click.indexOf("|") != -1) {
            setError(clickTF);
            break;
          }
          TextField v0TF = (TextField)panel.getComponent("v0");
          TextField v1TF = (TextField)panel.getComponent("v1");
          TextField v2TF = (TextField)panel.getComponent("v2");
          ToggleButton hTB = (ToggleButton)panel.getComponent("h");
          ToggleButton vTB = (ToggleButton)panel.getComponent("v");
          Light c0L = (Light)panel.getComponent("c0");
          String c0 = Integer.toString(c0L.getBackColor(), 16);
          Light c1L = (Light)panel.getComponent("c1");
          String c1 = Integer.toString(c1L.getBackColor(), 16);
          Light cnL = (Light)panel.getComponent("cn");
          String cn = Integer.toString(cnL.getBackColor(), 16);
          String v0 = v0TF.getText();
          String v1 = v1TF.getText();
          String v2 = v2TF.getText();
          Rectangle r = (Rectangle)focus.getProperty("rect");
          Table t1 = (Table)client.getPanel().getComponent("t1");  //components
          Component comp = t1.get(r.x, r.y, false);
          String type = getComponentType(comp);
          String style = "";
          switch (type) {
            case "button": ((Button)comp).setText(text); break;
            case "label": ((Label)comp).setText(text); break;
            case "light": ((Light)comp).setColors(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16)); style = "0=" + c0 + ";1=" + c1; break;
            case "light3": ((Light3)comp).setColors(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16), Integer.valueOf(cn, 16)); style = "0=" + c0 + ";1=" + c1 + ";n=" + cn; break;
            case "togglebutton":
              ToggleButton tb = (ToggleButton)comp;
              tb.setText(text);
              tb.setColors(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16));
              style = "0=" + c0 + ";1=" + c1;
              break;
            case "progressbar":
              ProgressBar pb = (ProgressBar)comp;
              pb.setColors(Integer.valueOf(c0, 16), Integer.valueOf(c1, 16), Integer.valueOf(cn, 16));
              pb.setLevels(Float.valueOf(v0), Float.valueOf(v1), Float.valueOf(v2));
              pb.setDir(hTB.isSelected() ? ProgressBar.HORIZONTAL : ProgressBar.VERTICAL);
              style = "0=" + c0 + ";1=" + c1 + ";2=" + cn + ";v0=" + v0 + ";v1=" + v1 + ";v2=" + v2 + "o=" + (hTB.isSelected() ? "h" : "v");
              break;
            case "image":
              Image img = (Image)comp;
              img.setImage(Images.getImage(text));
              break;
          }
          String events = "press=" + press + "|release=" + release + "|click=" + click;
          int pid = (Integer)client.getProperty("panel");
          CellRow cell = Database.getCell(pid, r.x, r.y);
          cell.events = events;
          cell.tag = tag;
          cell.text = text;
          cell.style = style;
          Database.saveCellTable(Database.getPanelById(pid).name);
        }
        PopupPanel props = (PopupPanel)client.getPanel().getComponent("jfc_panel_props");
        props.setVisible(false);
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
          Database.addFunction("New Function");
        }
        client.setPanel(Panels.getPanel("jfc_funcs", client));
        break;
      }
      case "jfc_funcs_edit": {
        client.setProperty("func", Integer.valueOf(arg));
        client.setProperty("focus", null);
        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }
      case "jfc_funcs_delete": {
        if (Database.isFunctionInUse(Integer.valueOf(arg))) {
          Panels.showError(client, "Can not delete function that is in use");
          break;
        }
        client.setProperty("arg", arg);
        Panels.confirm(client, "Delete function?", "jfc_funcs_delete");
        break;
      }

      case "jfc_func_editor_add_rung": {
        if (context.debug != null) {
          context.debug.cancel();
          context.debug = null;
          Button debug = (Button)client.getPanel().getComponent("jfc_debug");
          debug.setText("Debug");
        }
        int fid = (Integer)client.getProperty("func");
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
        Rungs rungs = (Rungs)client.getProperty("rungs");
        if (rid == -1) {
          //insert at end
          rid = rungs.rungs.size();
        }
        Database.addRung(fid, rid, "h", "Comment");  //saves rung table
        ArrayList<CellRow> cells = new ArrayList<CellRow>();
        ArrayList<Node> nodes = new ArrayList<Node>();
        RungRow data = Database.getRungById(fid, rid);
        Rung rung = Panels.buildRung(data, cells, nodes, client, true, fid);
        rungs.rungs.add(rid, rung);
        Table table = Panels.buildTable(new Table(Panels.cellWidth, Panels.cellHeight, 1, 1), null, cells.toArray(new CellRow[cells.size()]), client, 0, 0, nodes.toArray(new Node[0]));
        rungs.panel.add(rid, table);
        rung.table = table;
        int cnt = rungs.rungs.size();
        for(int a=rid+1;a<cnt;a++) {
          Rung r = rungs.rungs.get(a);
          Label lbl = (Label)r.table.get(0, 0, false);
          lbl.setText("Rung " + (a+1));
        }
        FunctionRow fnc = Database.getFunctionById(fid);
        fnc.revision++;
        Database.funcs.save();
        //BUG:need to reload panel - node.rid are not incremented
            client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }

      case "jfc_func_editor_del_rung": {
        if (context.debug != null) {
          context.debug.cancel();
          context.debug = null;
          Button debug = (Button)client.getPanel().getComponent("jfc_debug");
          debug.setText("Debug");
        }
        Panels.confirm(client, "Delete rung?", "jfc_func_editor_del_rung");
        break;
      }

      case "jfc_func_editor_edit_rung": {
        if (context.debug != null) {
          context.debug.cancel();
          context.debug = null;
          Button debug = (Button)client.getPanel().getComponent("jfc_debug");
          debug.setText("Debug");
        }
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
        if (rid == -1) {
          JFLog.log("No rung selected to edit");
          break;
        }
        client.setProperty("rung", rid);
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
        LogicBlock blk = null;
        try {
          Class cls = Class.forName("jfcontrols.logic." + name.replaceAll(" ", "_").toUpperCase());
          Constructor ctor = cls.getConstructor();
          blk = (LogicBlock)ctor.newInstance();
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
        int fid = (Integer)client.getProperty("func");
        int rid = (Integer)client.getProperty("rung");
        Rung rung = (Rung)client.getProperty("rungObj");
        NodeRoot root = rung.root;
        if (!root.isValid(client)) {
          break;
        }
        Database.clearBlocksById(fid, rid);
        String str = root.saveLogic();
        JFLog.log("logic=" + str);
        if (str == null) {
          Panels.showError(client, "Failed to save!");
          break;
        }
        RungRow rungobj = Database.getRungById(fid, rid);
        rungobj.logic = str;
        TextField tf = (TextField)client.getPanel().getComponent("comment" + rid);
        String cmt = tf.getText();
        rungobj.comment = cmt;
        Database.saveRungsById(fid);
        FunctionRow fnc = Database.getFunctionById(fid);
        fnc.revision++;
        Database.funcs.save();

        client.setPanel(Panels.getPanel("jfc_func_editor", client));
        break;
      }

      case "jfc_func_editor_compile": {
        if (context.debug != null) {
          context.debug.cancel();
          context.debug = null;
          Button debug = (Button)client.getPanel().getComponent("jfc_debug");
          debug.setText("Debug");
        }
        int fid = (Integer)client.getProperty("func");
        synchronized(lock) {
          if (!FunctionService.recompileFunction(fid)) {
            Panels.showErrorText(client, "Compile failed!", FunctionCompiler.error);
            FunctionCompiler.error = null;
            break;
          }
          JFLog.log("Compiling successful");
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
        int fid = (Integer)client.getProperty("func");
        FunctionRow fnc = Database.getFunctionById(fid);
        long revision = fnc.revision;
        if (FunctionService.functionUpToDate(fid, revision)) {
          context.debug = new DebugContext(client, fid);
          context.debug.start();
          b.setText("Stop");
        } else {
          Panels.showError(client, "Function needs to be compiled!");
        }
        break;
      }

      case "jfc_alarm_editor_new": {
        synchronized(lock) {
          //TODO
        }
        client.setPanel(Panels.getPanel("jfc_alarm_editor", client));
        break;
      }

      case "jfc_alarm_editor_delete": {
        synchronized(lock) {
          //TODO
        }
        client.setPanel(Panels.getPanel("jfc_alarm_editor", client));
        break;
      }

      case "jfc_vision_camera_new": {
        synchronized(lock) {
          Database.addVisionCamera();
        }
        client.setPanel(Panels.getPanel("jfc_vision_cameras", client));
        break;
      }

      case "jfc_vision_camera_save": {
        //not needed?
        break;
      }

      case "jfc_vision_camera_delete": {
        synchronized(lock) {
          int id = Integer.valueOf(arg);
          Database.deleteVisionCamera(id);
        }
        client.setPanel(Panels.getPanel("jfc_vision_cameras", client));
        break;
      }

      case "jfc_vision_program_new": {
        synchronized(lock) {
          Database.addVisionProgram();
        }
        client.setPanel(Panels.getPanel("jfc_vision_programs", client));
        break;
      }

      case "jfc_vision_program_save": {
        //TODO
        break;
      }

      case "jfc_vision_program_edit": {
        int id = Integer.valueOf(arg);
        int pid = Database.getVisionProgramPID(id);
        client.setProperty("visionprogram", pid);
        client.setProperty("visionshot", -1);
        client.setProperty("visionarea", -1);
        client.setPanel(Panels.getPanel("jfc_vision_shots", client));
        break;
      }

      case "jfc_vision_program_delete": {
        synchronized(lock) {
          int id = Integer.valueOf(arg);
          Database.deleteVisionProgram(id);
        }
        client.setPanel(Panels.getPanel("jfc_vision_shots", client));
        break;
      }

      case "jfc_vision_shot_new": {
        int pid = (Integer)client.getProperty("visionprogram");
        synchronized(lock) {
          Database.addVisionShot(pid);
        }
        client.setPanel(Panels.getPanel("jfc_vision_shots", client));
        break;
      }

      case "jfc_vision_shot_save": {
        //TODO
        break;
      }

      case "jfc_vision_shot_edit": {
        int sid = Integer.valueOf(arg);
        client.setProperty("visionshot", sid);
        client.setPanel(Panels.getPanel("jfc_vision_areas", client));
        break;
      }

      case "jfc_vision_shot_select": {
        int sid = Integer.valueOf(arg);
        client.setProperty("visionshot", sid);
        client.setPanel(Panels.getPanel("jfc_vision_shots", client));
        break;
      }

      case "jfc_vision_shot_delete": {
        synchronized(lock) {
          int id = Integer.valueOf(arg);
          int pid = (Integer)client.getProperty("visionprogram");
          Database.deleteVisionShot(pid, id);
        }
        client.setPanel(Panels.getPanel("jfc_vision_shots", client));
        break;
      }

      case "jfc_vision_shot_save_load_last": {
        //TODO
        break;
      }

      case "jfc_vision_shot_save_load_ok": {
        //TODO
        break;
      }

      case "jfc_vision_shot_save_load_nok": {
        //TODO
        break;
      }

      case "jfc_vision_shot_save_save_ok": {
        //TODO
        break;
      }

      case "jfc_vision_shot_save_save_nok": {
        //TODO
        break;
      }

      case "jfc_vision_area_new": {
        int pid = (Integer)client.getProperty("visionprogram");
        int sid = (Integer)client.getProperty("visionshot");
        synchronized(lock) {
          Database.addVisionArea(pid, sid, false);
        }
        client.setPanel(Panels.getPanel("jfc_vision_areas", client));
        break;
      }

      case "jfc_vision_area_save": {
        //TODO
        break;
      }

      case "jfc_vision_area_update": {
        int pid = (Integer)client.getProperty("visionprogram");
        int sid = (Integer)client.getProperty("visionshot");
        int id = (Integer)client.getProperty("visionarea");
        LayersPanel panel = (LayersPanel)client.getProperty("jfc_vision_area");
        VisionSystem.setupVisionImage(panel, pid, sid, id);
        break;
      }

      case "jfc_vision_area_select": {
        int pid = (Integer)client.getProperty("visionprogram");
        int sid = (Integer)client.getProperty("visionshot");
        int id = Integer.valueOf(arg);
        client.setProperty("visionarea", id);
        LayersPanel panel = (LayersPanel)client.getProperty("jfc_vision_area");
        VisionSystem.setupVisionImage(panel, pid, sid, id);
        break;
      }

      case "jfc_vision_area_delete": {
        int id = Integer.valueOf(arg);
        synchronized(lock) {
          Database.deleteVisionArea(id);
        }
        int pid = (Integer)client.getProperty("visionprogram");
        int rid = Database.getVisionAreaLocator(pid).id;
        client.setProperty("visionarea", rid);
        client.setPanel(Panels.getPanel("jfc_vision_areas", client));
        break;
      }

      case "setPanel":
        String name;
        if (arg.startsWith("jfc_")) {
          name = arg;
        } else {
          name = "usr_" + arg;
        }
        Panel panel = Panels.getPanel(name, client);
        if (panel != null) {
          client.setPanel(panel);
        } else {
          JFLog.log("Error:Panel not found:" + name);
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
    String red = (String)tf.getProperty("red");
    if (red != null) {
      tf.setBackColor(Color.white);
      tf.setProperty("red", null);
    }
    String tag = (String)tf.getProperty("tag");
    if (tag == null) return;
    String value = tf.getText();
    if (tag.startsWith("jfc_")) {
      String f[] = tag.split("_", 5);
      //jfc_table_col_type_id
      String table = f[1];
      String col = f[2];
      String type = f[3];
      String id = f[4];
      if (type.equals("tagid")) {
        int ctrl = Integer.valueOf(client.getProperty("ctrl").toString());
        int cid = Database.getControllerCID(ctrl);
        if (!TagsService.validTagName(cid, value)) {
          tf.setBackColor(Color.red);
          tf.setProperty("red", "true");
          return;
        }
      }
      if (type.equals("tag")) {
        int ctrl = Integer.valueOf(client.getProperty("ctrl").toString());
        int cid = Database.getControllerCID(ctrl);
        if (Database.getTagByName(cid, value) != null) {
          //duplicate tag name
          tf.setBackColor(Color.red);
          tf.setProperty("red", "true");
          return;
        }
      }
      String current = Database.select(table, id, col, type);
      if (current != null && current.equals(value)) return;
      if (!Database.update(table, id, col, value, type)) {
        String org = Database.select(table, id, col, type);
        tf.setText(org);
      }
    } else {
//      JFLog.log("setTag:" + tag + "=" + tf.getText());
      context.write(tag, tf.getText());
    }
  }
  public static void edit(TextArea ta) {
    WebUIClient client = ta.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    String red = (String)ta.getProperty("red");
    if (red != null) {
      ta.setBackColor(Color.white);
      ta.setProperty("red", null);
    }
    String tag = (String)ta.getProperty("tag");
    if (tag == null) return;
    String value = ta.getText();
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
      Database.update(table, id, col, value, type);
    } else {
      context.write(tag, ta.getText());
    }
  }
  public static void changed(ComboBox cb) {
    WebUIClient client = cb.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = (String)cb.getProperty("tag");
    if (tag == null) return;
    String value = cb.getSelectedValue();
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
      Database.update(table, id, col, value, type);
    } else {
      context.write(tag, value);
    }
  }
  public static void changed(CheckBox cb) {
    WebUIClient client = cb.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = (String)cb.getProperty("tag");
    if (tag == null) return;
    if (tag.startsWith("jfc_")) {
      String value = cb.isSelected() ? "true" : "false";
      String f[] = tag.split("_", 5);
      //jfc_table_col_type_id
      String table = f[1];
      String col = f[2];
      String type = f[3];
      String id = f[4];
      if (table.equals("config")) {
        id = "\'" + id + "\'";
      }
      Database.update(table, id, col, value, type);
    } else {
      String value = cb.isSelected() ? "1" : "0";
      context.write(tag, value);
    }
  }
  public static void changed(ToggleButton tb) {
    WebUIClient client = tb.getClient();
    ClientContext context = (ClientContext)client.getProperty("context");
    String tag = (String)tb.getProperty("tag");
    if (tag == null) return;
    if (tag.startsWith("jfc_")) {
      String value = tb.isSelected() ? "true" : "false";
      String f[] = tag.split("_", 5);
      //jfc_table_col_type_id
      String table = f[1];
      String col = f[2];
      String type = f[3];
      String id = f[4];
      if (table.equals("config")) {
        id = "\'" + id + "\'";
      }
      Database.update(table, id, col, value, type);
    } else {
      String value = tb.isSelected() ? "1" : "0";
      context.write(tag, value);
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
    JFLog.log("press:" + c);
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
      if (cmd_args_.length() == 0) continue;
      int i1 = cmd_args_.indexOf("(");
      int i2 = cmd_args_.indexOf(")");
      String cmd = cmd_args_.substring(0, i1);
      String args[] = cmd_args_.substring(i1+1, i2).split(",");
      doCommand(cmd, args, c.getClient());
    }
  }
  public static void release(Component c) {
    JFLog.log("release:" + c);
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
      if (cmd_args_.length() == 0) continue;
      int i1 = cmd_args_.indexOf("(");
      int i2 = cmd_args_.indexOf(")");
      String cmd = cmd_args_.substring(0, i1);
      String args[] = cmd_args_.substring(i1+1, i2).split(",");
      doCommand(cmd, args, c.getClient());
    }
  }
  private static String getArgs(String[] args) {
    StringBuilder sb = new StringBuilder();
    for(String arg : args) {
      if (sb.length() > 0) sb.append(",");
      sb.append(arg);
    }
    return sb.toString();
  }
  public static void doCommand(String cmd, String[] args, WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    switch (cmd) {
      case "toggleBit": {
        TagBase tag = context.getTag(args[0]);
        FunctionService.addCommand(cmd, tag);
        break;
      }
      case "setBit": {
        TagBase tag = context.getTag(args[0]);
        FunctionService.addCommand(cmd, tag);
        break;
      }
      case "clearBit":
      case "resetBit": {
        TagBase tag = context.getTag(args[0]);
        FunctionService.addCommand(cmd, tag);
        break;
      }
      case "setPanel": {
        JFLog.log("doCommand:" + cmd + ":" + getArgs(args));
        client.setPanel(Panels.getPanel("usr_" + args[0], client));
        break;
      }
      default: {
        JFLog.log("doCommand:unknown cmd=" + cmd);
        break;
      }
    }
  }
  public static void setError(TextField tf) {
    tf.setFocus();
    tf.setBackColor(Color.red);
    tf.setProperty("red", "true");
  }
  public static void clearError(Component c) {
    String red = (String)c.getProperty("red");
    if (red != null) {
      c.setBackColor(Color.white);
      c.setProperty("red", null);
    }
  }
  public static void clear(ComboBox cb, int initIndex) {
    clearError((Component)cb);
    cb.setSelectedIndex(initIndex);
  }
  public static void clear(TextField tf, String initText) {
    clearError((Component)tf);
    tf.setText(initText);
  }
  public static void clear(CheckBox cb, boolean initState) {
    clearError((Component)cb);
    cb.setSelected(initState);
  }
  public static String getComponentType(Component comp) {
    String type = comp.getClass().getName().toLowerCase();
    int idx = type.lastIndexOf(".");
    return type.substring(idx+1);
  }
}
