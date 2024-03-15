package javaforce.vm;

/** Network port group.
 *
 * These are not persistent.
 *
 */

import java.io.*;

public class NetworkPort implements Serializable {
  private static final long serialVersionUID = 1L;

  protected NetworkPort(String parent, String name, int vlan) {
    this.parent = parent;
    this.name = name;
    this.vlan = vlan;
  }

  public String name;
  public String parent;  //bound to virtual network
  public int vlan;

  private native static boolean nremove(String parent, String name);
  /** Remove this port group from virtual interface. */
  public boolean remove() {
    return nremove(parent, name);
  }

  protected static String createXML(String parent, String name, int vlan) {
    StringBuilder xml = new StringBuilder();
    xml.append("<portgroup>");
    xml.append("<name>" + name + "</name>");
    xml.append("<vlan><tag id='" + vlan + "'></tag></vlan>");
    xml.append("</portgroup>");
    return xml.toString();
  }
}
