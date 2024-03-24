package javaforce.vm;

/** Network physical interface.
 *
 */

import java.io.*;

public class NetworkInterface extends NetworkConfig implements Serializable {
  private static final long serialVersionUID = 1L;

  protected NetworkInterface(String name) {
    super(name);
  }

  private native static String[] nlistPhys();
  /** List server physical network interfaces. */
  public static NetworkInterface[] listPhysical() {
    String[] list = nlistPhys();
    if (list == null) list = new String[0];
    NetworkInterface[] nics = new NetworkInterface[list.length];
    for(int idx = 0;idx<list.length;idx++) {
      nics[idx] = new NetworkInterface(list[idx]);
    }
    getInfo(nics);
    return nics;
  }
}
