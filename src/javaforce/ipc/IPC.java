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

  /** Invoke RPC on specified end point. */
  public Object invoke(String dest, String method, Object[] args) throws Exception;

  /** Invoke RPC to all end points that have subscribed to the method. */
  public boolean signal(String method, Object[] args);
}
