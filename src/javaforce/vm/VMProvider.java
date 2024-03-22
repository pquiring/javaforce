package javaforce.vm;

/** VMProvider
 *
 * Provides details from VM Management System of a Virtual Machine.
 *
 * @author pquiring
 */

public interface VMProvider {
  /** Provide VLAN for Network. */
  public int getVLAN(String name);
  /** Provide Bridge (virtual switch) for Network. */
  public NetworkBridge getBridge(String name);
  /** Provide free VNC port for Virtual Machine (5901-5999). */
  public int getVNCPort(String name);
  /** Provides server host name */
  public String getServerHostname();
}
