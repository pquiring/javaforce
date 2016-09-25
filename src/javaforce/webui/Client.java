package javaforce.webui;

/** WebClient.java
 *
 *  Tracks web page state for one client.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.service.*;

public class Client {
  public WebSocket socket;
  public Panel root;
  public String hash;
  public int nextID;

  public static Client NULL = new Client();

  public Client() {
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
  }
  public void dispatchEvent(String comp, String event, String args[]) {
    if (comp.length() == 0) {
      switch (event) {
        case "load":
          String html = root.html();
          sendEvent("body", "sethtml", new String[] {"html=" + html});
          break;
      }
    } else {
      Component c = root.get(comp);
      if (c != null) {
        c.dispatchEvent(event, args);
      } else {
        System.out.println("Error:Component not found:" + comp);
      }
    }
  }
  public synchronized void sendEvent(String comp, String event, String args[]) {
    if (socket == null) return;
    StringBuffer sb = new StringBuffer();
    sb.append("{\"event\":\"" + event + "\",\"comp\":\"" + comp + "\"");
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
        sb.append(",\"" + key + "\":\"" + value + "\"");
      }
    }
    sb.append("}");
    socket.write(sb.toString().getBytes());
  }
  public void redirect(Panel panel) {
    root = panel;
    socket.write(("{\"event\":\"redir\",\"url\":\"" + panel.id + "\"}").getBytes());
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
}
