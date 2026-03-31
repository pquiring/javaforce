package javaforce.bus;

/** Base class for JBusClient, JBusServer
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.ipc.*;

public class JBus implements EndPoint {
  private String name;
  private Dispatch dispatch;
  private Dispatcher dispatcher;

  private DBus dbus;

  public JBus(String name, Object obj) {
    if (obj == null) obj = new Object();
    this.name = name;
    dispatcher = new Dispatcher(obj);
  }

  /** Setup alternative message dispatch. */
  public void setDispatch(Dispatch dispatch) {
    this.dispatch = dispatch;
  }

  public String getEndPointName() {
    return name;
  }

  /** Dispatches inbound method call. */
  public Object dispatch(String method, Object[] args) throws Exception {
    if (dispatch != null) {
      return dispatch.onMessage(method, args);
    } else {
      return dispatcher.dispatch(method, args);
    }
  }

  /** Invoke a method on another member on the message bus. */
  public Object invoke(String dest, String method, Object... args) {
    try {
      return dbus.invoke(dest, method, args);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Invoke a method on all clients that have subscribed to method. */
  public Object signal(String dest, String method, Object... args) {
    try {
      return dbus.signal(method, args);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  /** Subscribe to a signal from another client. */
  public boolean subscribe(String sender, String method) {
    try {
      return dbus.subscribe(sender, method);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Unsubscribe to a signal from another client. */
  public boolean unsubscribe(String sender, String method) {
    try {
      return dbus.unsubscribe(sender, method);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Returns bus name requested or assigned. */
  public String getBusName() {
    return dbus.getBusName();
  }

  /** Connect to message bus. */
  public boolean connect() {
    try {
      JFLog.log("JBus:starting...");
      dbus = new DBus(this);
      boolean res = dbus.connect();
      if (!res) {
        JFLog.log("JBus.connect() failed!");
      }
      return res;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Closes connection to message bus. */
  public void disconnect() {
    if (dbus == null) return;
    try {
      dbus.disconnect();
    } catch (Exception e) {
    }
    dbus = null;
  }
}
