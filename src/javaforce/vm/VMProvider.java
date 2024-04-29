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
  /** Provide VNC port for Virtual Machine. */
  public int getVNCPort(String name);
  /** Provide VNC password. */
  public String getVNCPassword();
  /** Provides server host name */
  public String getServerHostname();
}
