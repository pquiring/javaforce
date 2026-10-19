package javaforce.ipc;

/** InterfaceSelector.
 *
 * @author pquiring
 */

public interface InterfaceSelector {
  /** Returns an interface for a given object and interface name. */
  public Object getInterface(String object, String iface);
}
