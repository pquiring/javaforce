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
import javaforce.access.*;
import javaforce.service.*;
import javaforce.webui.event.*;
import javaforce.webui.panel.*;
import javaforce.webui.tasks.*;

public class WebUIClient {
  public WebUIServer server;
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
  private String upload_folder;
  private Status upload_status;

  private AccessControl access;

  public WebUIClient(WebUIServer server, WebSocket socket, WebUIHandler handler) {
    this.server = server;
    this.socket = socket;
    this.handler = handler;
    hash = Integer.toString(this.hashCode(), 16);
  }

  public WebUIServer getServer() {
    return server;
  }

  //TODO : create getters
  public NewUserPanel new_user_panel;
  public EditUserPanel edit_user_panel;
  public NewGroupPanel new_group_panel;
  public EditGroupPanel edit_group_panel;
  public ConfirmPanel confirm_panel;
  public ResetPasswordPanel reset_password_panel;
  public ChangePasswordPanel change_password_panel;
  public SelectFromListPanel select_panel;

  public ConfirmPanel getConfirmPanel() {
    return confirm_panel;
  }

  public ResetPasswordPanel getResetPasswordPanel() {
    return reset_password_panel;
  }

  public ChangePasswordPanel getChangePasswordPanel() {
    return change_password_panel;
  }

  public SelectFromListPanel getSelectFromListPanel() {
    return select_panel;
  }

  /** Adds standard set of PopupPanels to provided Panel.
   * These popup panels are required for AccessControl related Panels.
   */
  public void addPopupPanels(Panel panel) {
    panel.add(new_user_panel = new NewUserPanel("New User", this));
    panel.add(edit_user_panel = new EditUserPanel("Edit User", this));
    panel.add(new_group_panel = new NewGroupPanel("New Group", this));
    panel.add(edit_group_panel = new EditGroupPanel("Edit Group", this));
    panel.add(confirm_panel = new ConfirmPanel("Confirm Action", this));
    panel.add(reset_password_panel = new ResetPasswordPanel("Reset Password", this));
    panel.add(change_password_panel = new ChangePasswordPanel("Change Password", this));
    panel.add(select_panel = new SelectFromListPanel("Selection"));
  }

  public AccessControl getAccessControl() {
    return access;
  }

  public void setAccessControl(AccessControl access) {
    this.access = access;
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
    try {
      dispatchEvent("", "load", null);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void refresh() {
    root = null;
    sendEvent("body", "redir", null);
  }
  private void initPanel() {
    root.setClient(this);
    root.init();
  }
  public void dispatchEvent(String id, String event, String[] msg_args) {
    HTTP.Parameters msg;
    if (msg_args != null) {
      msg = HTTP.Parameters.decode(msg_args, '=', 0);
    } else {
      msg = new HTTP.Parameters();
    }
    if (id.length() == 0 || id.equals("body")) {
      switch (event) {
        case "load":
          if (root == null) {
            String name = msg.get("panel");
            if (name == null) {
              name = "root";
            } else {
              int i1 = name.indexOf('/');
              int i2 = name.indexOf('/', 1);
              if (i2 == -1) {
                name = name.substring(i1 + 1);
              } else {
                name = name.substring(i1 + 1, i2);
              }
              if (name.length() == 0) {
                name = "root";
              }
            }
            String http_params = msg.get("args");
            HTTP.Parameters params;
            if (http_params == null) {
              params = new HTTP.Parameters();
            } else {
              if (http_params.length() > 0 && http_params.charAt(0) == '?') {
                http_params = http_params.substring(1);
              }
              params = HTTP.Parameters.decode(http_params.split("[&]"), '=', 0);
            }
            root = handler.getPanel(name, params, this);
            initPanel();
          }
          isReady = true;
          String html = root.html();
          html = "<iframe name='upload' style='display:none;'></iframe>" + html;
          sendEvent("body", "sethtml", new String[] {"html=" + html});
          sendEvent("body", "setroot", new String[] {"root=" + root.id});
          root.events();
          break;
        case "mousedown":
          Menu.onMouseDownBody(this, msg_args);
          break;
        case "size":
          for(int a=0;a<msg_args.length;a++) {
            if (msg_args[a].startsWith("w=")) width = Integer.valueOf(msg_args[a].substring(2));
            if (msg_args[a].startsWith("h=")) height = Integer.valueOf(msg_args[a].substring(2));
          }
          if (WebUIServer.debug) JFLog.log("size=" + width + "x" + height);
          if (resized != null) {
            resized.onResized(null, width, height);
          }
          break;
        case "onloaded":
          root.dispatchEvent(event, msg_args);
          break;
        case "pong":
          synchronized(pingLock) {
            pingLock.notify();
          }
          break;
      }
    } else {
      if (root == null) {
        JFLog.log("Error:root panel not set");
        return;
      }
      Component c = root.get(id);
      if (c != null) {
        c.dispatchEvent(event, msg_args);
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
  public void sendData(byte[] data) {
    if (!isReady) return;
    synchronized (lock) {
      socket.write(data, WebSocket.TYPE_BINARY);
    }
  }
  public void sendData(byte[] data, int pos, int length) {
    if (!isReady) return;
    synchronized (lock) {
      socket.write(Arrays.copyOfRange(data, pos,pos + length), WebSocket.TYPE_BINARY);
    }
  }
  public boolean sendEvent(String id, String event, String[] args) {
    if (!isReady) return false;
    if (id == null) {
      JFLog.log("WebUIClient:Error:sendEvent():id==null");
      return false;
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
        return socket.write(sb.toString().getBytes("utf-8"));
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public void sendDataEvent(byte[] data, String id, String event, String[] args) {
    if (!isReady) return;
    synchronized (lock) {
      sendData(data);
      sendEvent(id, event, args);
    }
  }
  public void sendDataEvent(byte[] data, int pos, int length, String id, String event, String[] args) {
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
  public String getHost() {
    return socket.getHost();
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

  public String getUploadFolder() {
    return upload_folder;
  }

  public void setUploadFolder(String folder) {
    upload_folder = folder;
  }

  public Status getUploadStatus() {
    return upload_status;
  }

  public void setUploadStatus(Status status) {
    upload_status = status;
  }
}
