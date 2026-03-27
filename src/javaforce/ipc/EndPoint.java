package javaforce.ipc;

/** IPC Endpoint
 *
 * @author pquiring
 */

public interface EndPoint {
  /** Returns end point name in reverse DNS notation.
   * Example : com.example.MyService
   */
  public String getEndPointName();
  /** Dispatch function called to this endpoint.
   * @param method = method invoked
   * @param args = arguments
   *
   * @return method return value or null if error
   */
  public Object dispatch(String method, Object[] args) throws Exception;
}
