package javaforce.vm;

/** Network virtual interface.
 *
 * Used by VM Host.
 *
 */

import java.io.*;

import javaforce.*;

public class NetworkVirtual extends NetworkConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  public String bridge;
  public int vlan;

  public NetworkVirtual(String name, String bridge, String mac, String ip, String netmask, int vlan) {
    super(name);
    this.mac = mac;
    this.ip = ip;
    this.netmask = netmask;
    this.bridge = bridge;
    this.vlan = vlan;
  }

  /** Return [name, ip/mask, mac, vlan, bridge, link] */
  public String[] getState() {
    return new String[] {name, state.ip + "/" + state.netmask, state.mac, Integer.toString(vlan), bridge, state.link};
  }

  public static NetworkVirtual createVirtual(String name, NetworkBridge bridge, String mac, String ip, String netmask, int vlan) {
    {
      //create fake bridge with vlan
      ShellProcess p = new ShellProcess();
      p.keepOutput(true);
      p.run(new String[] {"/usr/bin/ovs-vsctl", "add-br", name, bridge.name, Integer.toString(vlan)}, true);
    }
    NetworkVirtual nic = new NetworkVirtual(name, bridge.name, mac, ip, netmask, vlan);
    nic.link_up();
    nic.set_ip();
    return nic;
  }

  /** Start virtual interface. */
  public boolean start() {
    link_up();
    set_ip();
    return true;
  }

  /** Stop virtual interface. */
  public boolean stop() {
    link_down();
    return true;
  }

  /** Remove this virtual interface. */
  public boolean remove() {
    {
      //delete bridge
      ShellProcess p = new ShellProcess();
      p.keepOutput(true);
      p.run(new String[] {"/usr/bin/ovs-vsctl", "del-br", name}, true);
    }
    return true;
  }
}
