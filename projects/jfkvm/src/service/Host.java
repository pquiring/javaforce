package service;

/** Host system.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class Host implements Serializable {
  private static final long serialVersionUID = 1L;

  public static boolean debug = false;  //disable version checks during development

  public String token;
  public String host;  //domain name or ip
  public String hostname;  //hostname

  //transient data
  public transient boolean online;
  public transient boolean valid;
  public transient float version;
  public transient boolean gluster;

  public String[] getState() {
    return new String[] {host, hostname, String.format("%.1f", version), Boolean.toString(online), Boolean.toString(valid), Boolean.toString(gluster)};
  }

  public boolean isValid(float min_ver) {
    return online && valid && (version >= min_ver || debug);
  }

  public boolean isValid() {
    return isValid(0);
  }

  /** Checks if ssh client key is present. */
  public boolean checkValid() {
    String keyfile = Paths.clusterPath + "/" + host;
    valid = new File(keyfile).exists();
    return valid;
  }

  public boolean getVersion() {
    try {
      HTTPS https = new HTTPS();
      https.open(host);
      byte[] data = https.get("/api/getver");
      if (data == null) throw new Exception("offline");
      String str = new String(data);
      version = Float.valueOf(str);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean notify(String msg, String name) {
    try {
      HTTPS https = new HTTPS();
      https.open(host);
      https.get("/api/notify?msg=" + msg + "&name=" + name + "&token=" + token);
      https.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getHostname() {
    if (!isValid(0.6f)) return null;
    try {
      HTTPS https = new HTTPS();
      https.open(host);
      byte[] data = https.get("/api/gethostname");
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return host;
    }
  }

  public int getNetworkVLAN(String name) {
    if (!isValid(0.6f)) return -1;
    try {
      HTTPS https = new HTTPS();
      https.open(host);
      byte[] data = https.get("/api/getnetworkvlan?network=" + name + "&token=" + token);
      https.close();
      if (data == null || data.length == 0) return -1;
      return Integer.valueOf(new String(data));
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }
}
