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

public class JBusClient extends JBus  {

  /** Creates new client.
   * @param obj = object with methods to invoke for RPC calls
   */
  public JBusClient(Object obj) {
    super(null, obj);
  }
}
