package javaforce.webui;

/** WebClient.java
 *
 *  Tracks web page state for one client.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;
import javaforce.service.*;
import javaforce.webui.event.*;

public class WebUIClient {
  public WebSocket socket;
  public Panel root;
  public String hash;
  public int nextID;
  public int zIndex = 1;
  public int width, height;
  public boolean isReady;

  public boolean popupMenuMouseDown;
  public PopupMenu topPopupMenu;

  private WebUIHandler handler;

  public WebUIClient(WebSocket socket, WebUIHandler handler) {
    this.socket = socket;
    this.handler = handler;
    hash = Integer.toString(this.hashCode(), 16);
  }

  public synchronized int getNextID() {
    return nextID++;
  }

  public void setSocket(WebSocket socket) {
    this.socket = socket;
  }
  public Panel getPanel() {
    return root;
  }
  public void setPanel(Panel root) {
    this.root = root;
    initPanel();
    dispatchEvent("", "load", null);
  }
  public void refresh() {
    root = null;
    sendEvent("body", "redir", null);
  }
  private void initPanel() {
    root.setClient(this);
    root.init();
  }
  public void dispatchEvent(String id, String event, String args[]) {
    if (id.length() == 0 || id.equals("body")) {
      switch (event) {
        case "load":
          if (root == null) {
            root = handler.getRootPanel(this);
            initPanel();
          }
          isReady = true;
          String html = root.html();
          sendEvent("body", "sethtml", new String[] {"html=" + html});
          break;
        case "mousedown":
          Menu.onMouseDownBody(this, args);
          break;
        case "size":
          for(int a=0;a<args.length;a++) {
            if (args[a].startsWith("w=")) width = Integer.valueOf(args[a].substring(2));
            if (args[a].startsWith("h=")) height = Integer.valueOf(args[a].substring(2));
          }
          if (resized != null) {
            resized.onResized(null, width, height);
          }
          break;
        case "onloaded":
          root.dispatchEvent(event, args);
          break;
        case "pong":
          synchronized(pingLock) {
            pingLock.notify();
          }
          break;
      }
    } else {
      Component c = root.get(id);
      if (c != null) {
        c.dispatchEvent(event, args);
      } else {
        JFLog.log("Error:Component not found:" + id);
      }
    }
  }
  private Object pingLock = new Object();
  /** Pings the client and waits for a reply (with a timeout). */
  public void ping(int ms) {
    synchronized(pingLock) {
      sendEvent("body", "ping", null);
      try {pingLock.wait(ms);} catch (Exception e) {}
    }
  }
  private String stringify(String in) {
    StringBuilder sb = new StringBuilder();
    char ca[] = in.toCharArray();
    int len = ca.length;
    sb.append("\"");
    for(int a=0;a<len;a++) {
      char ch = ca[a];
      switch (ch) {
        case '\"': sb.append("\\\""); break;
        case '\r': sb.append("\\r"); break;
        case '\n': sb.append("\\n"); break;
        case '\\': sb.append("\\\\"); break;
        default: sb.append(ch); break;
      }
    }
    sb.append("\"");
    return sb.toString();
  }
  public void sendData(byte data[]) {
    socket.write(data, WebSocket.TYPE_BINARY);
  }
  public synchronized void sendEvent(String id, String event, String args[]) {
    if (!isReady) return;
    StringBuilder sb = new StringBuilder();
    StringBuilder log = new StringBuilder();
    String str;
    str = "{\"event\":\"" + event + "\"";
    sb.append(str);
    log.append(str);
    if (id != null) {
      str = ",\"id\":\"" + id + "\"";
      sb.append(str);
      log.append(str);
    }
    if (args != null) {
      int cnt = args.length;
      for(int a=0;a<cnt;a++) {
        String arg = args[a];
        int idx = arg.indexOf("=");
        if (idx == -1) {
          idx = arg.length();
          arg += "=true";
        }
        String key = arg.substring(0, idx);
        String value = arg.substring(idx+1);
        str = ",\"" + key + "\":" + stringify(value);
        sb.append(str);
        if (key.equals("html")) {
          log.append(",\"" + key + "\":\"...\"");
        } else {
          log.append(str);
        }
      }
    }
    sb.append("}");
    log.append("}");
    if (WebUIServer.debug) JFLog.log("SEND=" + log.toString());
    socket.write(sb.toString().getBytes());
  }
  public String html() {
    return root.html();
  }
  public void close() {
    //TODO - cleanup resources
  }
  private HashMap<String,Object> map = new HashMap<>();
  /** Set user define property. */
  public void setProperty(String key, Object value) {
    map.put(key, value);
  }
  /** Get user define property. */
  public Object getProperty(String key) {
    return map.get(key);
  }
  public HashMap<String,Object> getProperties() {
    return map;
  }
  public int getZIndex() {
    return zIndex++;
  }
  public void releaseZIndex() {
    zIndex--;
  }
  public int getWidth() {
    return width;
  }
  public int getHeight() {
    return height;
  }
  public boolean isReady() {
    return isReady;
  }
  public boolean isConnected() {
    return socket.isConnected();
  }
  public void setTitle(String title) {
    sendEvent("body", "settitle", new String[] {"title=" + title});
  }
  /** Returns current ID.  This can be used to determine if user has switched to another page.  Returns -1 if user disconnected. */
  public int getCurrentID() {
    if (!isConnected()) return -1;
    return nextID;
  }

  private Resized resized;
  public void addResizedListener(Resized handler) {
    resized = handler;
  }
}
