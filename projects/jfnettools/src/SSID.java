/** SSID
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class SSID {
  public String ssid;
  public String auth;  //WPA2-Personal, WPA2-Enterprise
  public String encryption;  //PKS, CCMP
  public ArrayList<AP> aps = new ArrayList<>();

  /** Access Point */
  public static class AP {
    public String mac;
    public int dbm;
    public String type;  //802.11 type : a b g n ac ax etc.
    public int channel;

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("  {"); sb.append(JF.eol);
      sb.append("    BSSID:"); sb.append(mac); sb.append(JF.eol);
      sb.append("    dBm:"); sb.append(dbm); sb.append(JF.eol);
      sb.append("    type:"); sb.append(type); sb.append(JF.eol);
      sb.append("    channel:"); sb.append(channel); sb.append(JF.eol);
      sb.append("  }"); sb.append(JF.eol);
      return sb.toString();
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SSID:"); sb.append(ssid); sb.append(JF.eol);
    sb.append("{"); sb.append(JF.eol);
    for(AP ap : aps) {
      sb.append(ap.toString());
    }
    sb.append("}"); sb.append(JF.eol);
    return sb.toString();
  }

  public AP createAccessPoint() {
    AP ap = new AP();
    aps.add(ap);
    return ap;
  }
}
