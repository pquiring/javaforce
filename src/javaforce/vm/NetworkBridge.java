package javaforce.vm;

/** Network Bridge - "virtual switch"
 *
 * NOTE : Open vSwitch is required for VLAN tagging guest networks.
 *
 * https://docs.openvswitch.org/en/latest/howto/libvirt/
 *
 * delete old bridge:
 *   brctl delbr virbr0
 *
 * setup Open vSwitch Bridge:
 *   ovs-vsctl add-br ovsbr
 *   ovs-vsctl add-port ovsbr eth0
 *   ovs-vsctl show
 *
 * @author pquiring
 */

import java.io.*;

public class NetworkBridge implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;  //virbr0
  public String iface;  //physical nic
}
