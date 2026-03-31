package javaforce.ipc;

/** IPC Endpoint
 *
 * @author pquiring
 */

public interface EndPoint {
  /** Returns end point name in reverse DNS notation.
   * Return null for client end points to have a generic name provided.
   * On Linux the Javaforce package will install a DBus conf
   *   that will allow root to use names that start with "javaforce."
   *   or you could install your own into /etc/dbus-1/system.d
   * See DBus.getBusName() to get supplied name.
   * Example : com.example.MyService
   * @return End Point name (null to request system supplied name)
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
