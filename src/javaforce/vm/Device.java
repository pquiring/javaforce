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
  public String desc;
  public transient String xml;

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
      try {
        dev.decode_address();
      } catch (Exception e) {
        JFLog.log(e);
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
    return name + ":" + desc;
  }

  private void decode_address() {
    XML _xml = new XML();
    ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
    _xml.read(bais);
    switch (type) {
      case TYPE_USB: {
        addr_type = "usb";
        /* <device><name>...</name><devnode type='dev'>/dev/bus/usb/###/###<devnode><path>/sys/devices/pci####:##/####:##:##.#/usb#/#-#</path><capability><bus>..</bus><device>..</device></capability></device> */
        XML.XMLTag caps = _xml.getTag(new String[] {"device", "capability"});
        XML.XMLTag[] tags = caps.getChildren();
        for(XML.XMLTag tag : tags) {
          String name = tag.getName();
          String content = tag.getContent();
          switch (name) {
            case "bus": bus = content; break;
            case "device": port = content; break;
            case "product": desc = content; break;
          }
        }
        break;
      }
      case TYPE_PCI: {
        addr_type = "pci";
        /* <device><name>...</name><path>/sys/devices/pci####:##/####:##:##.#</path><capability><domain>....</domain><bus>..</bus><slot>..</slot><function>.</function></capability></device> */
        XML.XMLTag caps = _xml.getTag(new String[] {"device", "capability"});
        XML.XMLTag[] tags = caps.getChildren();
        for(XML.XMLTag tag : tags) {
          String name = tag.getName();
          String content = tag.getContent();
          switch (name) {
            case "domain": domain = content; break;
            case "bus": bus = content; break;
            case "slot": slot = content; break;
            case "function": function = content; break;
            case "product": desc = content; break;
          }
        }
        break;
      }
    }
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
