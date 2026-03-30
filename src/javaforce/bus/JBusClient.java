package javaforce.bus;

/**
 * JBusClient is the client side of inter-process communications (RPC).
 *
 * Based on DBus. JBusClient is an unnamed end point.
 *
 * Created : Apr 9, 2012
 *
 * @author pquiring
 */

import javaforce.*;
import javaforce.ipc.*;

public class JBusClient implements EndPoint {

  private Dispatch dispatch;
  private Dispatcher dispatcher;
  private DBus dbus;
  private boolean debug = false;

  /** Creates new client.
   * @param obj = object with methods to invoke for RPC calls
   */
  public JBusClient(Object obj) {
    if (obj == null) obj = new Object();
    dispatcher = new Dispatcher(obj);
  }

  public String getEndPointName() {
    return null;  //use generated client name
  }

  public Object dispatch(String method, Object[] args) throws Exception {
    if (dispatch != null) {
      return dispatch.onMessage(method, args);
    } else {
      return dispatcher.dispatch(method, args);
    }
  }

  /** Setup alternative message dispatch. */
  public void setDispatch(Dispatch dispatch) {
    this.dispatch = dispatch;
  }

  /** Enable logging exceptions to console. */
  public void setDebug(boolean state) {
    debug = state;
  }

  /** Connect to message bus. */
  public boolean connect() {
    try {
      JFLog.log("JBusClient:starting...");
      dbus = new DBus(this);
      return dbus.connect();
    } catch (Exception e3) {
      if (debug) JFLog.log(e3);
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

  public Object invoke(String dest, String method, Object[] args) {
    try {
      return dbus.invoke(dest, method, args);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
}
