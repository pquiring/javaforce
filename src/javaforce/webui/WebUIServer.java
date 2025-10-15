package javaforce.webui;

/** WebUI Server.
 *
 * WebUI uses a WebSocket to communicate between the server and client side to create
 * rich interfaces that are programmed like standard desktop applications.
 * No ugly javascript, css or html knowledge needed.
 * WebUI is not dependant on AWT or any related APIs and should work with the Java base module.
 *
 * Browser support:
 *
 *    Internet Explorer is NOT supported
 *    Edge 12+
 *    FireFox 28+
 *    Chrome 21+
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.ui.*;
import javaforce.service.*;

public class WebUIServer implements WebHandler, WebSocketHandler {
  private WebServer web;
  private WebUIHandler handler;

  /** Enable debug log messages. */
  public static boolean debug = false;

  /** Start WebUI Server on non-secure port. */
  public void start(WebUIHandler handler, int port) {
    start(handler, port, null);
  }

  /** Start WebUI Server on secure port using provided SSL keys. */
  public void start(WebUIHandler handler, int port, KeyMgmt keys) {
    this.handler = handler;
    if (web != null) stop();
    clients = new ArrayList<WebUIClient>();
    web = new WebServer();
    web.setWebSocketHandler(this);
    web.start(this, port, keys);
    JFLog.log("WebUI Server starting on port " + port + "...");
  }

  public void stop() {
    if (web != null) {
      web.stop();
      web = null;
    }
  }

  /** Sets upload folder. */
  public void setUploadFolder(String folder) {
    web.setUploadFolder(folder);
  }

  /** Sets max file upload size (-1 = unlimited) (default = 64MBs) */
  public void setUploadLimit(long size) {
    WebUpload.setMaxLength(size);
  }

  public void setClientVerify(boolean state) {
    web.setClientVerify(state);
  }

  public byte[] getResource(String name) {
    InputStream is = getClass().getClassLoader().getResourceAsStream(name);
    if (is == null) {
      JFLog.log("WebUIServer:Resource not found:" + name);
      return null;
    }
    return JF.readAll(is);
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req, res);
  }

  public void doGet(WebRequest req, WebResponse res) {
    String url = req.getURL();
    HTTP.Parameters params = HTTP.Parameters.decode(req.getQueryString());
    byte[] data = null;

    if (url.equals("/favicon.ico")) {
      url = "/webui.png";
    }
/*
    if (url.endsWith(".html")) {
      res.setContentType("text/html");  //default
    }
*/
    if (url.endsWith(".css")) {
      res.setContentType("text/css");
    }
    else if (url.endsWith(".png")) {
      res.setContentType("image/png");
    }
    else if (url.endsWith(".js")) {
      res.setContentType("text/javascript");
    }

    if (url.startsWith("/static/")) {
      // url = /static/r#
      Resource r = Resource.getResource(url.substring(8));
      if (r != null) {
        data = r.data;
        res.setContentType(r.mime);
      }
    } else if (url.startsWith("/user/")) {
      data = handler.getResource(url, params, req, res);
      res.addHeader("Cache-Control: no-store");
    } else if (url.startsWith("/api/")) {
      data = handler.getResource(url, params, req, res);
      res.addHeader("Cache-Control: no-store");
    } else if (url.startsWith("/icons/")) {
      try {
        InputStream is = this.getClass().getResourceAsStream("/javaforce" + url);
        data = is.readAllBytes();
        is.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    } else {
      int idx = url.indexOf('.');
      if (idx == -1) {
        url = "/webui.html";
      }
      String browser = req.getHeader("User-Agent");
      if (browser != null) {
        if (browser.indexOf("Trident") != -1) {
          //Internet Explorer - not supported
          url = "/webui-ie.html";
        }
      }
      //load resource from javaforce.jar
      data = getResource("javaforce/webui/static" + url);
    }
    if (data == null) {
      res.setStatus(404, "Resource not found");
      return;
    }
    try {
      res.write(data);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static ArrayList<WebUIClient> clients;

  public static WebUIClient getClient(WebSocket sock) {
    int cnt = clients.size();
    for(int a=0;a<cnt;a++) {
      WebUIClient client = clients.get(a);
      if (client.socket == sock) {
        return client;
      }
    }
    return null;
  }

  public static WebUIClient getClient(String hash) {
    int cnt = clients.size();
    for(int a=0;a<cnt;a++) {
      WebUIClient client = clients.get(a);
      if (client.hash.equals(hash)) {
        return client;
      }
    }
    return null;
  }

  public boolean doWebSocketConnect(WebSocket sock) {
    try {
      WebUIClient client = new WebUIClient(sock, handler);
      clients.add(client);
      handler.clientConnected(client);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void doWebSocketClosed(WebSocket sock) {
    WebUIClient client = getClient(sock);
    clients.remove(client);
    handler.clientDisconnected(client);
  }

  public void doWebSocketMessage(WebSocket sock, byte[] data, int type) {
    WebUIClient client = getClient(sock);
    if (client == null) {
      JFLog.log("Unknown Socket:" + sock);
      return;
    }
    if (type == WebSocket.TYPE_BINARY || type == WebSocket.TYPE_CONT) {
      client.dispatchData(data);
      return;
    }
    String msg = new String(data);
    if (debug) JFLog.log("RECV=" + msg);
    //decode JSON
    JSON.Element json;
    try {
      json = JSON.parse(msg);
    } catch (Exception e) {
      JFLog.log("msg=" + msg);
      e.printStackTrace();
      return;
    }
    String id = "";
    String event = "";
    ArrayList<String> args = new ArrayList<String>();
    int cnt = json.children.size();
    for(int a=0;a<cnt;a++) {
      JSON.Element e = json.children.get(a);
      if (e.key.equals("id")) {
        id = e.value;
      }
      else if (e.key.equals("event")) {
        event = e.value;
      }
      else {
        args.add(e.key + "=" + e.value);
      }
    }
    try {
      client.dispatchEvent(id, event, args.toArray(JF.StringArrayType));
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
