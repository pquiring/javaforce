package javaforce.vm;

/** Host Device (USB or PCI).
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Device extends Address implements Serializable {
  private static final long serialVersionUID = 1L;

  public static boolean debug = true;

  public int type;
  public String name;
  public String path;
  public String xml;

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
      String devstr = list[idx];
      int eq = devstr.indexOf('=');
      if (eq == -1) continue;
      String name = devstr.substring(0, eq);
      String xml = devstr.substring(eq + 1);
      Device dev = new Device();
      dev.type = type;
      dev.name = name;
      dev.xml = xml;
      if (debug) {
        JFLog.log("Device:" + dev);
      }
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
    return name + ":" + getType() + "=" + xml;
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
