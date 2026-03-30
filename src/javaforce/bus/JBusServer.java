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

public class JBusServer extends JBus {

  /** Create new server with reverse DNS name.
   * On Linux names should start with javaforce.*
   */
  public JBusServer(String name, Object obj) {
    super(name, obj);
  }
}
