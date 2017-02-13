package javaforce.webui;

/** WebClient.java
 *
 *  Tracks web page state for one client.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.service.*;

public class WebUIClient {
  public WebSocket socket;
  public Panel root;
  public String hash;
  public int nextID;
  public int zIndex = 1;

  public boolean popupMenuMouseDown;
  public PopupMenu topPopupMenu;

  public static WebUIClient NULL = new WebUIClient();

  public WebUIClient() {
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
    if (socket != null) {
      initPanel();
      dispatchEvent("", "load", null);
    }
  }
  public void initPanel() {
    root.setClient(this);
    root.init();
  }
  public void dispatchEvent(String id, String event, String args[]) {
    if (id.length() == 0 || id.equals("body")) {
      switch (event) {
        case "load":
          String html = root.html();
          sendEvent("body", "sethtml", new String[] {"html=" + html});
          break;
        case "mousedown":
          Menu.onMouseDownBody(this, args);
          break;
      }
    } else {
      Component c = root.get(id);
      if (c != null) {
        c.dispatchEvent(event, args);
      } else {
        System.out.println("Error:Component not found:" + id);
      }
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
        default: sb.append(ch); break;
      }
    }
    sb.append("\"");
    return sb.toString();
  }
  public synchronized void sendEvent(String id, String event, String args[]) {
    if (socket == null) return;
    StringBuffer sb = new StringBuffer();
    sb.append("{\"event\":\"" + event + "\"");
    if (id != null) {
      sb.append(",\"id\":\"" + id + "\"");
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
        sb.append(",\"" + key + "\":" + stringify(value));
      }
    }
    sb.append("}");
    String json = sb.toString();
//    if (!event.equals("sethtml")) {
      System.out.println("SEND=" + json);
//    }
    socket.write(json.getBytes());
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
  public int getZIndex() {
    return zIndex++;
  }
  public void releaseZIndex() {
    zIndex--;
  }
}
