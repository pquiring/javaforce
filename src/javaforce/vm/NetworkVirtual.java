package javaforce.vm;

/** Network virtual interface.
 *
 * Used by VM Host.
 *
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class NetworkVirtual extends NetworkConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  private static final boolean libvirt = false;  //not working

  public String bridge;
  public int vlan;

  protected NetworkVirtual(String name) {
    super(name);
    this.bridge = ngetbridge(name);
  }

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

  private native static String ngetbridge(String name);

  private native static String[] nlistVirt();
  /** List virtual network interfaces. */
  public static NetworkVirtual[] listVirtual() {
    if (libvirt) {
      String[] list = nlistVirt();
      if (list == null) list = new String[0];
      NetworkVirtual[] nics = new NetworkVirtual[list.length];
      for(int idx = 0;idx<list.length;idx++) {
        nics[idx] = new NetworkVirtual(list[idx]);
      }
      getInfo(nics);
      return nics;
    } else {
      return null;
    }
  }

  private native static String[] nlistPort(String name);
  /** List network port groups bound to this interface. */
  public NetworkPort[] listPort() {
    if (libvirt) {
      String[] list = nlistPort(name);
      if (list == null) list = new String[0];
      NetworkPort[] nics = new NetworkPort[list.length];
      for(int idx = 0;idx<list.length;idx++) {
        String[] pp = list[idx].split(";");
        nics[idx] = new NetworkPort(this.name, pp[0], Integer.valueOf(pp[1]));
      }
      return nics;
    } else {
      return null;
    }
  }

  private native static boolean ncreatevirt(String xml);
  /** Create virtual interface. */
  public static NetworkVirtual createVirtual(String name, NetworkBridge bridge, String mac, String ip, String netmask, int vlan) {
    if (libvirt) {
      String xml = createXML(name, bridge, mac, ip, netmask, vlan);
      JFLog.log("NetworkVirtual.xml=" + xml);
      if (!ncreatevirt(xml)) return null;
      return new NetworkVirtual(name);
    } else {
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
  }

  private native static boolean ncreateport(String name, String xml);
  /** Create network port group (VLAN) bound to this virtual interface. */
  public boolean createPort(String name, int vlan) {
    if (libvirt) {
      String xml = NetworkPort.createXML(this.name, name, vlan);
      JFLog.log("NetworkPort.xml=" + xml);
      return ncreateport(this.name, xml);
    } else {
      return false;
    }
  }

  private native static boolean nstart(String name);
  /** Start virtual interface. */
  public boolean start() {
    if (!libvirt) return false;
    return nstart(name);
  }

  private native static boolean nstop(String name);
  /** Stop virtual interface. */
  public boolean stop() {
    if (!libvirt) return false;
    return nstop(name);
  }

  private native static boolean nremove(String name);
  /** Remove this virtual interface. */
  public boolean remove() {
    if (libvirt) {
      return nremove(name);
    } else {
      {
        //delete bridge
        ShellProcess p = new ShellProcess();
        p.keepOutput(true);
        p.run(new String[] {"/usr/bin/ovs-vsctl", "del-br", name}, true);
      }
      return true;
    }
  }

  protected static String createXML(String name, NetworkBridge bridge, String mac, String ip, String netmask, int vlan) {
    StringBuilder xml = new StringBuilder();
    xml.append("<network>");
    xml.append("<name>" + name + "</name>");
    xml.append("<uuid>" + JF.generateUUID() + "</uuid>");
    xml.append("<forward mode='bridge'/>");
    xml.append("<bridge name='" + bridge.name + "'/>");
//    xml.append("<mac address='" + mac + "'/>");  //not supported
//    xml.append("<ip address='" + ip + "' netmask='" + netmask + "'/>");  //not supported
    if (bridge.type.equals("os")) {
      xml.append("<virtualport type='openvswitch'/>");
      xml.append("<vlan><tag id='" + vlan + "'></tag></vlan>");
    }
    xml.append("</network>");
    return xml.toString();
  }
}
