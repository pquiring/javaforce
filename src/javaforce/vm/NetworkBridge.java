package javaforce.vm;

/** Network Bridge - "virtual switch"
 *
 * NOTE : Open vSwitch is required for VLAN tagging guest networks.
 *
 * https://docs.openvswitch.org/en/latest/howto/libvirt/
 *
 * @author pquiring
 */

import java.io.*;

public class NetworkBridge implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;  //virbr0
  public String iface;  //physical nic
}
