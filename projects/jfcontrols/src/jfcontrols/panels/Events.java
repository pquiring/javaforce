package jfcontrols.panels;

/** Events.
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

public class Events {
  public static void click(Component c) {
    WebUIClient client = c.getClient();
    String func = (String)c.getProperty("func");
    String arg = (String)c.getProperty("arg");
    JFLog.log("click:" + c + ":func=" + func + ":arg=" + arg);
    if (func == null) return;
    switch (func) {
      case "showMenu": {
        PopupPanel panel = (PopupPanel)client.getProperty("popup_panel");
        panel.setVisible(true);
        break;
      }
      case "_jfc_login_ok":
        //TODO : login
      case "_jfc_login_cancel": {
        PopupPanel panel = (PopupPanel)client.getProperty("popup_panel");
        panel.setVisible(false);
        break;
      }
      case "changePanel":
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
  }
}
