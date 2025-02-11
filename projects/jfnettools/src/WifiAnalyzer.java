/** Wifi Analyzer
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class WifiAnalyzer extends Thread {
  private static boolean debug = false;

  public static String device = "wlan0";

  private SSID[] ssids;
  private boolean active;
  public static interface Callback {
    public void callback(SSID[] ssids);
    public void error(String msg);
  }
  private Callback callback;

  public String error;

  public void run() {
    active = true;
    while (active) {
      ssids = query_ssids();
      if (callback != null) {
        if (ssids != null) {
          callback.callback(ssids);
        }
        if (error != null) {
          callback.error(error);
        }
      }
      JF.sleep(1000);
    }
  }

  public void cancel() {
    active = false;
  }

  public void setCallback(Callback callback) {
    this.callback = callback;
  }

  public SSID[] query() {
    return ssids;
  }

  private SSID[] query_ssids() {
    if (JF.isWindows()) {
      return query_windows();
    } else {
      return query_linux();
    }
  }
  private SSID[] query_windows() {
    //netsh wlan show all
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"netsh", "wlan", "show", "all"}, true);
    if (out == null) {
      error = "WifiAnalyzer:null output";
      JFLog.log("WifiAnalyzer:output == null");
      return null;
    }
    String[] lns = out.split("\r\n");
    ArrayList<SSID> ssids = new ArrayList<>();
    SSID ssid = null;
    SSID.AP ap = null;
    boolean show_networks = false;
    for(String ln : lns) {
      ln = ln.trim();
      if (ln.length() == 0) continue;
      if (ln.equals("Access is denied.")) {
        error = "WifiAnalyzer:access denied";
        JFLog.log("WifiAnalyzer:Access is denied.");
        return null;
      }
      if (ln.contains("need location permission")) {
        error = "WifiAnalyzer:need location permission";
        JFLog.log("WifiAnalyzer:Need location permission.");
        return null;
      }
      if (ln.charAt(0) == '=') {
        if (!ln.contains("SHOW")) continue;
        show_networks = ln.contains("SHOW NETWORKS");
        continue;
      }
      if (!show_networks) continue;
      int idx = ln.indexOf(' ');
      if (idx == -1) continue;
      String tag = ln.substring(0 ,idx);
      switch (tag) {
        case "SSID":  //SSID {#} : {name}
          ssid = new SSID();
          ssids.add(ssid);
          idx = ln.indexOf(':');
          ssid.ssid = ln.substring(idx + 1).trim();
          if (ssid.ssid.length() == 0) {
            ssid.ssid = "<hidden>";
          }
          break;
        case "Authentication":  //Authentication : {type}
          idx = ln.indexOf(':');
          ssid.auth = ln.substring(idx + 1).trim();
          break;
        case "Encryption":  //Encryption : {type}
          idx = ln.indexOf(':');
          ssid.encryption = ln.substring(idx + 1).trim();
          break;
        case "BSSID":  //BSSID {#} : {mac}
          ap = ssid.createAccessPoint();
          idx = ln.indexOf(':');
          ap.mac = ln.substring(idx + 1).trim();
          break;
        case "Signal":  //Signal : {#}%
          idx = ln.indexOf(':');
          ap.dbm = signal_to_dbm(ln.substring(idx + 1).trim());
          break;
        case "Radio":  //Radio type : 802.11{xyz}
          idx = ln.indexOf("802.11");
          ap.type = ln.substring(idx + 6).trim();
          break;
        case "Channel":  //Channel : {#}
          idx = ln.indexOf(':');
          ap.channel = JF.atoi(ln.substring(idx + 1).trim());
          break;
        //Basic rates (Mbps) : ...
        //Other rates (Mbps) : ...
      }
    }
    return ssids.toArray(new SSID[0]);
  }

  private ArrayList<SSID> ssid_list;

  private SSID get_ssid_by_name(String ssid) {
    for(SSID ss : ssid_list) {
      if (ss.ssid.equals(ssid)) {
        return ss;
      }
    }
    SSID ss = new SSID();
    ssid_list.add(ss);
    return ss;
  }

  private SSID[] query_linux() {
    //iw dev {device} scan
    if (device == null) {
      JFLog.log("WifiAnalyzer:Please run config first");
      return null;
    }
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"/usr/sbin/iw", "dev", device, "scan"}, true);
    String[] lns = out.split("\n");
    String mac = null;
    String signal = null;
    String auth = null;
    String channel = null;
    SSID ssid = null;
    SSID.AP ap = null;
    int idx, i1, i2;
    ssid_list = new ArrayList<>();
    for(String ln : lns) {
      ln = ln.trim();
      if (ln.length() == 0) continue;
      idx = ln.indexOf(' ');
      if (idx == -1) continue;
      String tag = ln.substring(0, idx);
      if (tag.equals("*")) {
        //get next tag value
        i1 = idx + 1;
        i2 = ln.indexOf(' ', i1);
        tag = ln.substring(i1, i2);
      }
      switch (tag) {
        case "BSS":
          i1 = idx + 1;
          i2 = ln.indexOf('(');
          mac = ln.substring(i1, i2).trim();
          break;
        case "signal:":
          i1 = idx + 1;
          i2 = ln.indexOf("dBm");
          signal = ln.substring(i1, i2).trim();
          idx = signal.indexOf('.');
          if (idx != -1) {
            signal = signal.substring(0, idx);
          }
          break;
        case "freq:":
          i1 = idx + 1;
          i2 = ln.indexOf('.');
          channel = ln.substring(i1, i2);
          break;
        case "SSID:":
          String name = ln.substring(idx + 1).trim();
          if (name.length() == 0) {
            name = "<hidden>";
          }
          ssid = get_ssid_by_name(name);
          ap = ssid.createAccessPoint();
          ap.mac = mac;
          ap.dbm = Integer.valueOf(signal);
          ap.type = "?";
          ap.channel = freq_to_channel(Integer.valueOf(channel));
          break;
        case "Group":  //* Group cipher: CCMP or TKIP
          idx = ln.indexOf(':');
          ssid.encryption = ln.substring(idx + 1).trim();
          break;
        case "Authentication":
          idx = ln.indexOf(':');
          ssid.auth = auth_convert(ln.substring(idx + 1).trim());
          break;
        case "HT":
          ap.type = "n";
          break;
        case "VHT":
          ap.type = "ac";
          break;
      }
    }
    SSID[] ssids = ssid_list.toArray(new SSID[0]);
    ssid_list = null;
    return ssids;
  }

  private int signal_to_dbm(String signal) {
    signal = signal.trim();
    signal = signal.substring(0, signal.length() - 1);  //remove '%'
    int percent = JF.atoi(signal);
    return (percent / 2) - 100;
  }

  private int freq_to_channel(int freq) {
    //TODO
    return freq;
  }

  private String auth_convert(String in) {
    switch (in) {
      case "PSK": return "PSK-Personal";
      case "IEEE 802.1X": return "PSK-Enterprise";
    }
    return in;
  }

  public static void main(String[] args) {
    WifiAnalyzer wifi = new WifiAnalyzer();
    try {
      SSID[] ssids = wifi.query_ssids();
      if (ssids == null) return;
      JFLog.log("# SSID = " + ssids.length);
      for(SSID ssid : ssids) {
        JFLog.log(ssid.toString());
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
