package service;

/** Host system.
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.linux.*;

public class Host implements Serializable {
  private static final long serialVersionUID = 1L;

  public static boolean debug = false;

  public String token;
  public String host;  //ip address
  public String hostname;  //hostname
  public int type;
  public transient String ip_storage;  //storage ip

  //transient data
  public transient boolean online;
  public transient boolean valid;
  public transient float version;
  public transient String gluster_status;
  public transient boolean ceph_setup;  //ceph setup in progress
  public transient String ceph_status;

  //types
  public static final int TYPE_ON_PREMISE = 0;
  public static final int TYPE_REMOTE = 1;

  public String[] getState() {
    return new String[] {host, hostname, String.format("%.1f", version), Boolean.toString(online), Boolean.toString(valid), gluster_status, ceph_status};
  }

  public boolean isValid(float min_ver) {
    return online && valid && (version >= min_ver);
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
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/getver");
      https.close();
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
      if (!https.open(host)) throw new Exception("connect failed");
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
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/gethostname");
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return host;
    }
  }

  public String getStorageIP() {
    if (!isValid(3.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/getstorageip");
      https.close();
      if (data == null || data.length == 0) return null;
      ip_storage = new String(data);
      return ip_storage;
    } catch (Exception e) {
      JFLog.log(e);
      return host;
    }
  }

  public int getNetworkVLAN(String name) {
    if (!isValid(0.6f)) return -1;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/getnetworkvlan?network=" + name + "&token=" + token);
      https.close();
      if (data == null || data.length == 0) return -1;
      return Integer.valueOf(new String(data));
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public boolean addsshkey(String sshkey) {
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] result = https.get("/api/addsshkey?sshkey=" + JF.encodeURL(sshkey) + "&token=" + token);
      https.close();
      return new String(result).equals("okay");
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public String getGlusterStatus() {
    if (!isValid(3.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/gluster_status");
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return host;
    }
  }

  public String getCephStatus() {
    if (!isValid(3.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/ceph_status");
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return host;
    }
  }

  public boolean setCephStart() {
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] result = https.get("/api/ceph_setup_start?hostname=" + Linux.getHostname() + "&token=" + token);
      https.close();
      return new String(result).equals("okay");
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean setCephComplete() {
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] result = https.get("/api/ceph_setup_complete?hostname=" + Linux.getHostname() + "&token=" + token);
      https.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public HostDetails toHostDetails() {
    HostDetails hd = new HostDetails();
    hd.hostname = hostname;
    hd.ip4 = host;
    return hd;
  }

  public String toString() {
    return hostname;
  }
}
