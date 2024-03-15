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
    Device[] dlist = new Device[list.length];
    for(int idx=0;idx<list.length;idx++) {
      String item = list[idx];
      String[] nti = item.split("|");
      if (nti.length != 2) continue;
      Device dev = new Device();
      dev.name = nti[0];
      dev.type = type;
      dev.path = nti[2];
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
