/** MACTableEntry
 *
 * @author peter.quiring
 */

import java.io.*;

public class MACTableEntry implements Serializable {
  public static final long serialVersionUID = 1;

  public MACTableEntry(String mac, String port) {
    this.mac = mac;
    this.port = port;
  }

  public String mac;
  public String port;
}
