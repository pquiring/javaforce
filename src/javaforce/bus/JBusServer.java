package javaforce.bus;

/**
 * JBusServer is a server for inter-process communications (RPC).
 *
 * Based on DBus.  JBusServer is a "named" end point.
 *
 * Created : Apr 9, 2012
 *
 * @author pquiring
 */

import javaforce.ipc.*;
import javaforce.*;

public class JBusServer implements EndPoint {

  private String name;
  private DBus dbus;
  private Dispatcher dispatcher;
  private Object lock = new Object();

  /** Create new server with reverse DNS name.
   * On Linux names should start with javaforce.*
   */
  public JBusServer(String name, Object methods) {
    this.name = name;
    dispatcher = new Dispatcher(methods);
  }

  public String getEndPointName() {
    return name;
  }

  public Object dispatch(String method, Object[] args) throws Exception {
    return dispatcher.dispatch(method, args);
  }

  /** Connect to the message bus. */
  public void connect() {
    JFLog.append(JF.getLogPath() + "/jfbusserver.log", false);
    JFLog.setRetention(30);
    try {
      dbus = new DBus(this);
      JFLog.log("JBusServer starting on end point : " + name);
    } catch (Exception e2) {
      JFLog.log(e2);
    }
  }

  /** Disconnect from message bus. */
  public void disconnect() {
    if (dbus == null) {
      return;
    }
    try {
      dbus.disconnect();
    } catch (Exception e) {
    }
    dbus = null;
  }
}
