package jfcontrols.app;

/** jfControls : Main
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;
import javaforce.webui.*;
import jfcontrols.api.APIService;
import jfcontrols.panels.*;
import jfcontrols.functions.*;
import jfcontrols.sql.*;
import jfcontrols.tags.*;

public class Main implements WebUIHandler {

  public static String version = "0.1B";
  public static ClassLoader loader;

  public static void main(String args[]) {
    //start database
    SQLService.start();
    //start tags server
    TagsService.main();
    //start logic server
    FunctionService.main();
    //start api server
    APIService.main();
    //start webui server
    WebUIServer server = new WebUIServer();
    server.start(new Main(), 34000, false);
  }

  public Panel getRootPanel(WebUIClient client) {
    System.out.println("getRootPanel()");
    ClientContext context = new ClientContext(client);
    client.setProperty("context", context);
    context.start();
    return Panels.getPanel("main", client);
  }

  public byte[] getResource(String string) {
    System.out.println("getResource(" + string + ")");
    return null;
  }

  public void clientDisconnected(WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    if (context != null) {
      context.cancel();
    }
  }

  public static void restart() {
    FunctionService.cancel();
    TagsService.cancel();
    FunctionService.main();
    TagsService.main();
  }
}
