package javaforce.vm;

/** Network Bridge
 *
 * @author pquiring
 */

import java.io.*;

public class NetworkVLAN implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String bridge;
  public int vlan;

  public NetworkVLAN(String name, String bridge, int vlan) {
    this.name = name;
    this.bridge = bridge;
    this.vlan = vlan;
  }
}
