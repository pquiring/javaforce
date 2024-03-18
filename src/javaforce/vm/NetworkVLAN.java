package javaforce.vm;

/** Network Bridge
 *
 * @author pquiring
 */

import java.io.*;

public class NetworkVLAN implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String bridge;
  public int vlan;

  public NetworkVLAN(String name, String bridge, int vlan) {
    this.name = name;
    this.bridge = bridge;
    this.vlan = vlan;
  }

  public int getUsage() {
    int count = 0;
    VirtualMachine[] vms = VirtualMachine.list();
    for(VirtualMachine vm : vms) {
      Hardware hw = vm.loadHardware();
      if (hw == null) continue;
      for(Network nic : hw.networks) {
        if (nic.network.equals(name)) {
          count++;
        }
      }
    }
    return count;
  }
}
