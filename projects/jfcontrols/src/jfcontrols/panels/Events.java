package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.sql.*;

public class Events {
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
          PopupPanel panel = (PopupPanel)client.getProperty("login_panel");
          panel.setVisible(true);
        } else {
          PopupPanel panel = (PopupPanel)client.getProperty("menu_panel");
          panel.setVisible(true);
        }
        break;
      }
      case "_jfc_logout": {
        client.setProperty("user", null);
        PopupPanel panel = (PopupPanel)client.getProperty("menu_panel");
        panel.setVisible(false);
        break;
      }
      case "_jfc_login_ok":
        String user = ((TextField)c.getParent().findComponent("user")).getText();
        String pass = ((TextField)c.getParent().findComponent("pass")).getText();
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
          Label lbl = (Label)c.getParent().findComponent("errmsg");
          lbl.setText("Invalid username or password!");
          break;
        }
        //no break
      case "_jfc_login_cancel": {
        PopupPanel panel = (PopupPanel)client.getProperty("login_panel");
        panel.setVisible(false);
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
}
