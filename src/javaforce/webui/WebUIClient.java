package javaforce.webui;

/** WebClient
 *
 * Tracks web page state for one client.
 *
 * @author pquiring
 */

import java.io.*;
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
  private OutputStream os;

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
  /** Get Component by user assigned name. */
  public Component getComponent(String name) {
    return root.getComponent(name);
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
  public void dispatchEvent(String id, String event, String[] args) {
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
          sendEvent("body", "setroot", new String[] {"root=" + root.id});
          root.events();
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
  public void dispatchData(byte[] data) {
    //JFLog.log("data=" + data.length);
    if (os != null) {
      try { os.write(data); } catch (Exception e) {}
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
    char[] ca = in.toCharArray();
    int len = ca.length;
    sb.append("\"");
    for(int a=0;a<len;a++) {
      char ch = ca[a];
      switch (ch) {
        case '\"': sb.append("\\\""); break;
        case '\r': sb.append("\\r"); break;
        case '\n': sb.append("\\n"); break;
        case '\\': sb.append("\\\\"); break;
        default:
          if (ch < ' ') {
            sb.append(String.format("\\u%04x", (int)ch));
          } else {
            sb.append(ch);
          }
          break;
      }
    }
    sb.append("\"");
    return sb.toString();
  }
  private Object lock = new Object();
  public void sendData(byte data[]) {
    if (!isReady) return;
    synchronized (lock) {
      socket.write(data, WebSocket.TYPE_BINARY);
    }
  }
  public void sendData(byte data[], int pos, int length) {
    if (!isReady) return;
    synchronized (lock) {
      socket.write(Arrays.copyOfRange(data, pos,pos + length), WebSocket.TYPE_BINARY);
    }
  }
  private int tid = 1;
  public void sendEvent(String id, String event, String args[]) {
    if (!isReady) return;
    if (id == null) {
      JFLog.log("WebUIClient:Error:sendEvent():id==null");
      return;
    }
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
    if (WebUIServer.debug_tid) {
      //add transaction id for debugging
      str = ",\"tid\":\"" + (tid++) + "\"";
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
          //omit lengthy html code from log
          log.append(",\"" + key + "\":\"...\"");
        } else {
          log.append(str);
        }
      }
    }
    sb.append("}");
    log.append("}");
    if (WebUIServer.debug) JFLog.log("SEND=" + log.toString());
    try {
      synchronized (lock) {
        socket.write(sb.toString().getBytes("utf-8"));
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void sendDataEvent(byte data[], String id, String event, String[] args) {
    if (!isReady) return;
    synchronized (lock) {
      sendData(data);
      sendEvent(id, event, args);
    }
  }
  public void sendDataEvent(byte data[], int pos, int length, String id, String event, String[] args) {
    if (!isReady) return;
    synchronized (lock) {
      sendData(data, pos, length);
      sendEvent(id, event, args);
    }
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

  public void openURL(String url) {
    sendEvent(root.id, "openurl", new String[] {"url=" + url});
  }

  /** Set where binary data is written to. */
  public void setOutputStream(OutputStream os) {
    this.os = os;
  }
}
