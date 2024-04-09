package javaforce.vm;

/** Host Device (USB or PCI).
 *
 * @author pquiring
 */

import java.io.*;

public class Device extends Address implements Serializable {
  private static final long serialVersionUID = 1L;

  public int type;
  public String name;

  public static final int TYPE_USB = 1;
  public static final int TYPE_PCI = 2;

  //virConnectListAllNodeDevices
  private native static String[] nlist(int type);
  public static Device[] list(int type) {
    if (type < 1 || type > 2) return null;
    String[] list = nlist(type);
    if (list == null) list = new String[0];
    Device[] dlist = new Device[list.length];
    for(int idx=0;idx<list.length;idx++) {
      Device dev = new Device();
      dev.name = list[idx];
      dev.type = type;
      dlist[idx] = dev;
    }
    return dlist;
  }

  public String getType() {
    switch (type) {
      case TYPE_PCI: return "pci";
      case TYPE_USB: return "usb";
    }
    return "???";
  }

  public String toString() {
    return name + ":" + getType();
  }

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<hostdev mode='subsystem' type='" + getType() + "'>");
    xml.append("<source>");
    xml.append(getAddressXML());
    xml.append("</source>");
    xml.append("</hostdev>");
    return xml.toString();
  }
}
