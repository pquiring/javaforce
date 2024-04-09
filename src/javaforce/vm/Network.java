package javaforce.vm;

/** Network guest NIC.
 *
 * To list available models : kvm -net nic,model=?
 *
 * @author pquiring
 */

import java.io.*;

public class Network extends PCIAddress implements Serializable {
  private static final long serialVersionUID = 1L;

  public String network;
  public String model;
  public String mac;

  public Network(String network, String model, String mac) {
    this.network = network;
    this.model = model;
    this.mac = mac;
  }

  public static final String MODEL_E1000 = "e1000";
  public static final String MODEL_E1000E = "e1000e";
  public static final String MODEL_NET2K = "net2k_pci";
  public static final String MODEL_RTL8139 = "rtl8139";
  public static final String MODEL_VIRTIO = "virtio-net-pci";
  public static final String MODEL_i82801 = "i82801";
  public static final String MODEL_VMXNET3 = "vmxnet3";

  public String toString() {
    return "Network{" + network + "," + model + "," + mac + "}";
  }

  /** Create XML for VirtualMachine XML. */
  public String toXML(NetworkBridge bridge, int vlan) {
    StringBuilder xml = new StringBuilder();
    xml.append("<interface type='bridge'>");
    xml.append("<source bridge='" + bridge.name + "'/>");
    if (bridge.type.equals("os")) {
      xml.append("<virtualport type='openvswitch'/>");
      if (vlan != 0) {
        xml.append("<vlan><tag id='" + vlan + "'></tag></vlan>");
      }
    }
    xml.append("<model type='" + model + "'/>");
    xml.append("<mac address='" + mac + "'/>");
    xml.append("</interface>");
    return xml.toString();
  }
}
