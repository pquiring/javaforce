 package javaforce.webui;

/** WebUI Server.
 *
 * WebUI uses a WebSocket to communicate between the server and client side to create
 * rich interfaces that are programmed like standard desktop applications.
 * No ugly javascript, css or html knowledge needed.
 * WebUI is not dependant on AWT or any related APIs and should work with the Java base module.
 *
 * Browser support (WebSocket, css:flex):
 *
 *    IE 10+ (but still has issues with flex layouts)
 *    Edge 12+
 *    FireFox 28+
 *    Chrome 21+
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.service.*;

public class Server implements WebHandler, WebSocketHandler {
  private Web web;
  private WebUIHandler handler;

  public void start(WebUIHandler handler, int port, boolean secure) {
    this.handler = handler;
    if (web != null) stop();
    web = new Web();
    web.setWebSocketHandler(this);
    web.start(this, port, secure);
    System.out.println("WebUI Server listing on port " + port);
  }

  public void stop() {
    if (web != null) {
      web.stop();
      web = null;
    }
  }

  public byte[] getResource(String name) {
    InputStream is = getClass().getClassLoader().getResourceAsStream(name);
    if (is == null) {
      return null;
    }
    return JF.readAll(is);
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req, res);
  }

  public void doGet(WebRequest req, WebResponse res) {
    String url = req.getURL();
    byte data[] = null;
    if (url.equals("/")) {
      url = "/webui.html";
    }
    if (url.startsWith("/static/")) {
      // url = /static/r#
      Resource r = Resource.getResource(url.substring(8));
      if (r != null) {
        data = r.data;
        res.setContentType(r.mime);
      }
    } else if (url.startsWith("/user/")) {
      data = handler.getResource(url);
    } else {
      data = getResource("javaforce/webui" + url);
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

  public ArrayList<Client> clients = new ArrayList<Client>();

  public Client getClient(WebSocket sock) {
    int cnt = clients.size();
    for(int a=0;a<cnt;a++) {
      Client client = clients.get(a);
      if (client.socket == sock) {
        return client;
      }
    }
    return null;
  }

  public Client getClient(String hash) {
    int cnt = clients.size();
    for(int a=0;a<cnt;a++) {
      Client client = clients.get(a);
      if (client.hash.equals(hash)) {
        return client;
      }
    }
    return null;
  }

  public boolean doWebSocketConnect(WebSocket sock) {
    try {
      Client client = new Client();
      client.setPanel(handler.getRootPanel(client));  //client can be used to save/store values
      client.setSocket(sock);
      client.initPanel();
      clients.add(client);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public void doWebSocketClosed(WebSocket sock) {
    clients.remove(getClient(sock));
  }

  public void doWebSocketMessage(WebSocket sock, byte[] data, int type) {
    Client client = getClient(sock);
    if (client == null) {
      JFLog.log("Unknown Socket:" + sock);
      return;
    }
    String msg = new String(data);
    JFLog.log("RECV=" + msg);
    //decode JSON
    JSON.Element json;
    try {
      json = JSON.parse(msg);
    } catch (Exception e) {
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
      client.dispatchEvent(id, event, args.toArray(new String[args.size()]));
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
