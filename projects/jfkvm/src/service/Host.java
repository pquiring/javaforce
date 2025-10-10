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
import javaforce.vm.*;

public class Host implements Serializable {
  private static final long serialVersionUID = 1L;

  public static boolean debug = false;

  public static final Host[] HostArrayType = new Host[0];

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

  private String getType() {
    switch (type) {
      case TYPE_ON_PREMISE: return "On Premise";
      case TYPE_REMOTE: return "Remote";
      default: return "?";
    }
  }

  public String[] getState() {
    if (type == TYPE_REMOTE) {
      gluster_status = "n/a";
      ceph_status = "n/a";
    }
    return new String[] {host, hostname, String.format("%.1f", version), getType(), Boolean.toString(online), Boolean.toString(valid), gluster_status, ceph_status};
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
      byte[] data = https.get("/api/getver?token=" + token);
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
      byte[] data = https.get("/api/gethostname?token=" + token);
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
      byte[] data = https.get("/api/getstorageip?token=" + token);
      https.close();
      if (data == null || data.length == 0) return null;
      ip_storage = new String(data);
      return ip_storage;
    } catch (Exception e) {
      JFLog.log(e);
      return ip_storage;
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
      byte[] data = https.get("/api/gluster_status?token=" + token);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String getCephStatus() {
    if (!isValid(3.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/ceph_status?token=" + token);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String[] getStoragePools() {
    if (!isValid(6.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/getpools?token=" + token);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data).split("[|]");
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String isStoragePoolMounted(String name) {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/is_pool_mounted?token=" + token + "&pool=" + name);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String pathExists(String path) {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      String enpath = JF.encodeURL(path);
      byte[] data = https.get("/api/path_exists?token=" + token + "&path=" + enpath);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String mkdirs(String path) {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      String enpath = JF.encodeURL(path);
      byte[] data = https.get("/api/mkdirs?token=" + token + "&path=" + enpath);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String rmdirs(String path) {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      String enpath = JF.encodeURL(path);
      byte[] data = https.get("/api/rmdirs?token=" + token + "&path=" + enpath);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String[][] vm_list() {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/vm_list?token=" + token);
      https.close();
      if (data == null || data.length == 0) return null;
      String[] vms = new String(data).split("\n");
      String[][] list = new String[vms.length][0];
      int pos = 0;
      for(String vm : vms) {
        list[pos++] = vm.split("\t");
      }
      return list;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public Hardware vm_load(String name) {
    if (!isValid(7.0f)) return null;
    try {
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/vm_load?token=" + token + "&vm=" + name);
      https.close();
      if (data == null || data.length == 0) return null;
      String str = new String(data);
      HTTP.Parameters params = HTTP.Parameters.decode(str);
      //deserialize Hardware
      byte[] hw_bin = Base16.decode(params.get("hardware").getBytes());
      ByteArrayInputStream hw_bais = new ByteArrayInputStream(hw_bin);
      Hardware hw = (Hardware) Compression.deserialize(hw_bais, hw_bin.length);
      //deserialize Location
      byte[] loc_bin = Base16.decode(params.get("loc").getBytes());
      ByteArrayInputStream loc_bais = new ByteArrayInputStream(loc_bin);
      Location loc = (Location) Compression.deserialize(loc_bais, loc_bin.length);
      hw.setLocation(loc);
      return hw;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String vm_save(VirtualMachine vm, Hardware hardware) {
    if (!isValid(7.0f)) return null;
    try {
      //serialize Hardware
      ByteArrayOutputStream hw_baos = new ByteArrayOutputStream();
      if (!Compression.serialize(hw_baos, hardware)) return null;
      String hw_hex = new String(Base16.encode(hw_baos.toByteArray()));

      //serialize Location
      ByteArrayOutputStream loc_baos = new ByteArrayOutputStream();
      if (!Compression.serialize(loc_baos, hardware.getLocation())) return null;
      String loc_hex = new String(Base16.encode(loc_baos.toByteArray()));

      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/vm_save?token=" + token + "&hardware=" + hw_hex + "&loc=" + loc_hex);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String vm_disk_create(Disk disk, int flags) {
    if (!isValid(7.0f)) return null;
    try {
      //serialize Disk
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (!Compression.serialize(baos, disk)) return null;
      String hex = new String(Base16.encode(baos.toByteArray()));
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/vm_disk_create?token=" + token + "&disk=" + hex + "&flags=" + flags);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String vm_disk_resize(Disk disk) {
    if (!isValid(7.0f)) return null;
    try {
      //serialize Disk
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      if (!Compression.serialize(baos, disk)) return null;
      String hex = new String(Base16.encode(baos.toByteArray()));
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/vm_disk_resize?token=" + token + "&disk=" + hex);
      https.close();
      if (data == null || data.length == 0) return null;
      return new String(data);
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public String[] browse_list(String path) {
    if (!isValid(7.0f)) return null;
    try {
      String enpath = JF.encodeURL(path);
      HTTPS https = new HTTPS();
      if (!https.open(host)) throw new Exception("connect failed");
      byte[] data = https.get("/api/browse_list?token=" + token + "&path=" + enpath);
      https.close();
      if (data == null) return null;
      return new String(data).split("\n");
    } catch (Exception e) {
      JFLog.log(e);
      return null;
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
