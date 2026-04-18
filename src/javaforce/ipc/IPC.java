package javaforce.ipc;

/** IPC interface
 *
 * @author pquiring
 */

public interface IPC {
  /** Connect to IPC service. */
  public boolean connect();

  /** Disconnect from IPC service. */
  public boolean disconnect();

  /** Return name on message bus. */
  public String getBusName();

  /** Invoke RPC on specified end point. */
  public Object invoke(String dest, String method, Object... args) throws Exception;

  /** Subscribe to a signal from another client. */
  public boolean subscribe(String sender, String method);

  /** Unsubscribe to a signal from another client. */
  public boolean unsubscribe(String sender, String method);

  /** Invoke RPC to all end points that have subscribed to the method. */
  public boolean signal(String method, Object... args);
}
