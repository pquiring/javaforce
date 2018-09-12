package jfcontrols.app;

/** jfControls : Main
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.webui.*;

import jfcontrols.api.*;
import jfcontrols.panels.*;
import jfcontrols.functions.*;
import jfcontrols.db.*;
import jfcontrols.tags.*;

public class Main implements WebUIHandler {

  public static String version = "0.3";
  public static ClassLoader loader;
  public static boolean debug = false;
  public static String msgs = "";

  public static void main(String args[]) {
    if (args != null && args.length > 0) {
      if (args[0].equals("debug")) {
        debug = true;
      }
    }
//    if (debug) SQL.debug = true;
    Paths.init();
    //start database
    Database.start();
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
    client.setProperty("xref", "-1");
    if (debug) {
      client.setProperty("user", "admin");
    }
    client.setTitle("jfControls");
    return Panels.getPanel("main", client);
  }

  public byte[] getResource(String string) {
    System.out.println("getResource(" + string + ")");
    return null;
  }

  public void clientConnected(WebUIClient client) {
    ClientContext context = new ClientContext(client);
    client.setProperty("context", context);
    context.start();
  }

  public void clientDisconnected(WebUIClient client) {
    ClientContext context = (ClientContext)client.getProperty("context");
    if (context != null) {
      context.cancel();
    }
    client.setProperty("context", null);
  }

  public static void restart() {
    FunctionService.cancel();
    TagsService.cancel();
    System.gc();
    TagsService.main();
    FunctionService.main();
  }

  public static void stop() {
    FunctionService.cancel();
    TagsService.cancel();
    System.gc();
  }

  /** Adds error message shown in config (restart is needed to clear them) */
  public static void addMessage(String msg) {
    msgs += msg;
    msgs += "\r\n";
  }

  public static void trace() {
    try { throw new Exception(); } catch (Exception e) { JFLog.log(e); }
  }

  public static void serviceStart(String args[]) {
    main(args);
  }

  public static void serviceStop() {
    stop();
//    Database.stop();
    loader = null;
    System.gc();
  }
}
