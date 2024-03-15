package javaforce.vm;

/** Network physical interface.
 *
 */

import java.io.*;

public class NetworkInterface implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;

  protected NetworkInterface(String name) {
    this.name = name;
  }

  private native static String[] nlistPhys();
  /** List server physical network interfaces. */
  public static NetworkInterface[] listPhysical() {
    String[] list = nlistPhys();
    NetworkInterface[] nics = new NetworkInterface[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      nics[idx] = new NetworkInterface(list[idx]);
    }
    return nics;
  }

  private native static String[] nlistVirt(String name);
  /** List virtual network interfaces bridged with this interface. */
  public NetworkVirtual[] listVirtual() {
    String[] list = nlistVirt(name);
    NetworkVirtual[] nics = new NetworkVirtual[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      String[] pp = list[idx].split(";");
      nics[idx] = new NetworkVirtual(pp[0], pp[1]);
    }
    return nics;
  }

  private native static boolean ncreate(String parent, String xml);
  /** Create virtual interface bridged with this interface. */
  public boolean createVirtual(String name) {
    return ncreate(this.name, NetworkVirtual.createXML(this.name, name));
  }
}
