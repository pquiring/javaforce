package javaforce.vm;

/** Network guest NIC.
 *
 * To list available models : kvm -net nic,model=?
 *
 * @author pquiring
 */

import java.io.*;

public class Network implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String network;
  public String model;
  public String mac;

  public static final String MODEL_E1000 = "e1000";
  public static final String MODEL_E1000E = "e1000e";
  public static final String MODEL_NET2K = "net2k_pci";
  public static final String MODEL_RTL8139 = "rtl8139";
  public static final String MODEL_VIRTIO = "virtio-net-pci";
  public static final String MODEL_i82801 = "i82801";
  public static final String MODEL_VMXNET3 = "vmxnet3";

  /** Create XML for VirtualMachine XML. */
  public String toXML(String bridge, int vlan) {
    StringBuilder xml = new StringBuilder();
    xml.append("<network>");
    xml.append("<name>" + name + "</name>");
    xml.append("<uuid>" + UUID.generate() + "</uuid>");
    xml.append("<forward mode='bridge'/>");
    xml.append("<bridge name='" + bridge + "'/>");
    xml.append("<model>" + model + "</model>");
    xml.append("<mac address='" + mac + "'/>");
    xml.append("<vlan><tag id='" + vlan + "'></tag></vlan>");
    xml.append("</network>");
    return xml.toString();
  }
}
