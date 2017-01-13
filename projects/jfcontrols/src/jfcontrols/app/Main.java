package jfcontrols.app;

/** jfControls : Main
 *
 * @author pquiring
 */

import jfcontrols.webui.MainPanel;
import javaforce.webui.*;
import jfcontrols.logic.Service;

public class Main implements WebUIHandler {
  public static void main(String args[]) {
    //start logic server
    Service.main();
    //start webui server
    Server server = new Server();
    server.start(new Main(), 34000, false);
  }

  public Panel getRootPanel(Client client) {
    System.out.println("getRootPanel()");
    return new MainPanel();
  }

  public byte[] getResource(String string) {
    System.out.println("getResource(" + string + ")");
    return null;
  }
}
