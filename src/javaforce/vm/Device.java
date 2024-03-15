package javaforce.vm;

/** Device (USB or PCI).
 *
 * @author pquiring
 */

import java.io.*;

public class Device implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public int type;
  public String path;

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

  public String toXML() {
    StringBuilder xml = new StringBuilder();
    xml.append("<device>");
    xml.append("<name>" + name + "</name>");
    xml.append("<path>" + path + "</path>");
    xml.append("</device>");
    return xml.toString();
  }
}
