package javaforce.vm;

/** Network virtual interface.
 *
 * Used by VM Host.
 *
 */

import java.io.*;

public class NetworkVirtual implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String bridge;
  public String mac;
  public String ip;
  public String netmask;

  protected NetworkVirtual(String name) {
    this.name = name;
    this.bridge = ngetbridge(name);
  }

  private native static String ngetbridge(String name);

  private native static String[] nlistVirt();
  /** List virtual network interfaces. */
  public static NetworkVirtual[] listVirtual() {
    String[] list = nlistVirt();
    if (list == null) list = new String[0];
    NetworkVirtual[] nics = new NetworkVirtual[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      nics[idx] = new NetworkVirtual(list[idx]);
    }
    return nics;
  }

  private native static String[] nlistPort(String name);
  /** List network port groups bound to this interface. */
  public NetworkPort[] listPort() {
    String[] list = nlistPort(name);
    if (list == null) list = new String[0];
    NetworkPort[] nics = new NetworkPort[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      String[] pp = list[idx].split(";");
      nics[idx] = new NetworkPort(this.name, pp[0], Integer.valueOf(pp[1]));
    }
    return nics;
  }

  private native static boolean ncreatevirt(String name, String xml);
  /** Create virtual interface. */
  public static boolean createVirtual(String name, NetworkBridge bridge, String mac, String ip, String netmask, int vlan) {
    return ncreatevirt(name, createXML(name, bridge, mac, ip, netmask, vlan));
  }

  private native static boolean ncreateport(String name, String xml);
  /** Create network port group (VLAN) bound to this virtual interface. */
  public boolean createPort(String name, int vlan) {
    return ncreateport(this.name, NetworkPort.createXML(this.name, name, vlan));
  }

  private native static boolean nstart(String name);
  /** Start virtual interface. */
  public boolean start() {
    return nstart(name);
  }

  private native static boolean nremove(String name);
  /** Remove this virtual interface. */
  public boolean remove() {
    return nremove(name);
  }

  private native static boolean nassign(String name, String ip, String mask);
  /** Assign IP address to virtual interface. */
  public boolean assign(String ip, String mask) {
    return nassign(name, ip, mask);
  }

  protected static String createXML(String name, NetworkBridge bridge, String mac, String ip, String netmask, int vlan) {
    StringBuilder xml = new StringBuilder();
    xml.append("<network>");
    xml.append("<name>" + name + "</name>");
    xml.append("<uuid>" + UUID.generate() + "</uuid>");
    xml.append("<forward mode='bridge'/>");
    xml.append("<bridge name='" + bridge.name + "'/>");
    xml.append("<mac address='" + mac + "'/>");
    xml.append("<ip address='" + ip + "' netmask='" + netmask + "'/>");
    if (bridge.type.equals("os")) {
      xml.append("<vlan><tag id='" + vlan + "'></tag></vlan>");
    }
    xml.append("</network>");
    return xml.toString();
  }
}
