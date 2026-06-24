package javaforce.vm;

import java.io.*;

/** NetworkConfig
 *
 * @author pquiring
 */

public class NetworkState implements Serializable {
  private static final long serialVersionUID = 1L;

  public String ip;
  public String netmask;
  public String mac;
  public String link;
}
