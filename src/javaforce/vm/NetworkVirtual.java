package javaforce.vm;

/** Network virtual interface.
 *
 */

import java.io.*;

public class NetworkVirtual implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String bridge;

  protected NetworkVirtual(String name) {
    this.name = name;
    this.bridge = ngetbridge(name);
  }

  private native static String ngetbridge(String name);

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

  private native static boolean ncreate(String parent, String xml);
  /** Create network port group (VLAN) bound to this virtual interface. */
  public boolean createPort(String name, int vlan) {
    return ncreate(this.name, NetworkPort.createXML(this.name, name, vlan));
  }

  private native static boolean nremove(String name);
  /** Remove this virtual interface from physical interface. */
  public boolean remove() {
    return nremove(name);
  }

  private native static boolean nassign(String name, String ip, String mask);
  /** Assign IP address to virtual interface. */
  public boolean assign(String ip, String mask) {
    return nassign(name, ip, mask);
  }

  protected static String createXML(String parent, String name) {
    StringBuilder xml = new StringBuilder();
    xml.append("<network>");
    xml.append("<name>" + name + "</name>");
    xml.append("<bridge>" + parent + "</bridge>");
    xml.append("</network>");
    return xml.toString();
  }
}
