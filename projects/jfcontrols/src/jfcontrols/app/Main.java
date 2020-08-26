package jfcontrols.app;

/** jfControls : Main
 *
 * @author pquiring
 */

import java.io.ByteArrayOutputStream;
import javaforce.*;
import javaforce.webui.*;

import jfcontrols.api.*;
import jfcontrols.panels.*;
import jfcontrols.functions.*;
import jfcontrols.db.*;
import jfcontrols.tags.*;

public class Main implements WebUIHandler {

  public static String version = "0.6";
  public static ClassLoader loader;
  public static boolean debug = false;
  public static boolean debug_scantime = false;
  public static String msgs = "";
  public static WebUIServer server;

  public static void main(String args[]) {
    if (args != null && args.length > 0) {
      for(int a=0;a<args.length;a++) {
        if (args[a].equals("debug")) {
          debug = true;
        }
        if (args[a].equals("scantime")) {
          debug_scantime = true;
        }
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
    server = new WebUIServer();
    server.start(new Main(), 34000, false);
  }

  public Panel getRootPanel(WebUIClient client) {
    System.out.println("getRootPanel()");
    client.setProperty("xref", -1);
    client.setProperty("audio-init", 0);
    if (debug) {
      client.setProperty("user", "admin");
    }
    client.setTitle("jfControls");
    return Panels.getPanel("main", client);
  }

  public byte[] getResource(String url) {
    System.out.println("getResource(" + url + ")");
    // url = /user/hash/id/counter
    String f[] = url.split("/");
    //empty = f[0];
    //user = f[1];
    String hash = f[2];
    String id = f[3];
    WebUIClient client = server.getClient(hash);
    if (client == null) {
      JFLog.log("Error:Main.getResources():client==null");
    }
    JFImage img = VisionSystem.generateImage(client);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    img.savePNG(baos);
    return baos.toByteArray();
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

  public static void start() {
    Database.start();
    TagsService.main();
    FunctionService.main();
  }

  public static void restart() {
    stop();
    start();
  }

  public static void stop() {
    FunctionService.cancel();
    TagsService.cancel();
    Database.stop();
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
