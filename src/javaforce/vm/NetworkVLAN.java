package javaforce.vm;

/** Network Bridge
 *
 * @author pquiring
 */

import java.io.*;

public class NetworkVLAN implements Serializable {
  private static final long serialVersionUID = 1L;

  public String name;
  public String virt;  //virtual vlan
  public int vlan;
}
