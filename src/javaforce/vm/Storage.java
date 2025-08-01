package javaforce.vm;

/** Storage pool registered with libvirt.
 *
 *  Pools are mounted in /volumes
 *
 * https://en.wikipedia.org/wiki/Clustered_file_system
 *
 */

import java.io.*;
import java.nio.file.*;
import java.util.*;

import javaforce.*;

public class Storage implements Serializable {
  private static final long serialVersionUID = 1L;

  public Storage(int type, String name, String uuid) {
    this.type = type;
    this.name = name;
    if (uuid == null) {
      this.uuid = JF.generateUUID();
    } else {
      this.uuid = uuid;
    }
  }

  public Storage(int type, String name, String uuid, String path) {
    this.type = type;
    this.name = name;
    if (uuid == null) {
      this.uuid = JF.generateUUID();
    } else {
      this.uuid = uuid;
    }
    this.path = path;
  }

  public int type;
  public String name;
  public String uuid;

  public String host;  //nfs, iscsi, gluster (host)
  public String target;  //iscsi
  public String path;  //nfs, local, gluster (device), cephfs
  public String user;  //iscsi
  //public String pass;  //saved as Secret (stored in /root/secret)

  public static final int TYPE_LOCAL_PART = 1;  //local partition
  public static final int TYPE_LOCAL_DISK = 2;  //local disk
  public static final int TYPE_NFS = 3;
  public static final int TYPE_ISCSI = 4;
  public static final int TYPE_GLUSTER = 5;
  public static final int TYPE_CEPHFS = 6;

  public static final int STATE_OFF = 0;
  public static final int STATE_ON = 1;
  public static final int STATE_BUILD = 2;
  public static final int STATE_ERROR = 3;

  private native static String[] nlist();
  /** Returns list of UUID for Storage units registered. */
  public static String[] list() {
    String[] list = nlist();
    if (list == null) list = new String[0];
    return list;
  }

  private native static boolean nregister(String xml);
  public boolean register() {
    String xml = createXML();
    JFLog.log("Storage.xml=" + xml);
    return nregister(xml);
  }

  private native static boolean nunregister(String name);
  public boolean unregister() {
    return nunregister(name);
  }

  //virDomainCreate
  private native static boolean nstart(String name);
  public boolean start() {
    //create folder under /volumes
    new File(getPath()).mkdir();
    if (type == TYPE_GLUSTER) {
      new File(getGlusterBrick()).mkdirs();
    }
    boolean res = nstart(name);
    if (type == TYPE_GLUSTER && mounted()) {
      new File(getGlusterVolume()).mkdirs();
    }
    return res;
  }

  //virDomainShutdown()
  private native static boolean nstop(String name);
  public boolean stop() {
    boolean res = nstop(name);
    if (res) {
      new File(getPath()).delete();
    }
    return res;
  }

  private native static int ngetState(String name);
  public int getState() {
    return ngetState(name);
  }

  private native static String ngetUUID(String name);
  /** Get libvirt UUID (does NOT match Linux device UUID) */
  public String getUUID() {
    return ngetUUID(name);
  }

  public static String getSystemIQN() {
    // /etc/iscsi/initiatorname.iscsi
    try {
      FileInputStream fis = new FileInputStream("/etc/iscsi/initiatorname.iscsi");
      Properties props = new Properties();
      props.load(fis);
      fis.close();
      return props.getProperty("InitiatorName");
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public static boolean setSystemIQN(String iqn) {
    if (iqn.equals(getSystemIQN())) return true;  //no change
    try {
      FileOutputStream fos = new FileOutputStream("/etc/iscsi/initiatorname.iscsi");
      fos.write("##Modifed by JavaForce\n".getBytes());
      fos.write(("InitiatorName= " + iqn + "\n").getBytes());
      fos.close();
      ShellProcess sp = new ShellProcess();
      sp.run(new String[] {"/usr/bin/systemctl", "restart", "iscsid"}, true);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return false;
  }

  public String getName() {
    return name;
  }

  public String getStateString() {
    int state = getState();
    switch (state) {
      case STATE_OFF: return "off";
      case STATE_ON: return "on";
      case STATE_BUILD: return "building";
      case STATE_ERROR: return "error";
    }
    return "???";
  }

  public String getTypeString() {
    switch (type) {
      case TYPE_LOCAL_PART: return "LocalPart";
      case TYPE_LOCAL_DISK: return "LocalDisk";
      case TYPE_NFS: return "NFS";
      case TYPE_ISCSI: return "iSCSI";
      case TYPE_GLUSTER: return "Gluster";
      case TYPE_CEPHFS: return "CephFS";
    }
    return "???";
  }

  public String[] getStates() {
    String size, free;
    boolean on = getState() == STATE_ON;
    boolean mounted = mounted();
    if (on && mounted) {
      size = getTotalSize().toString();
      free = getFreeSize().toString();
    } else {
      if (on) {
        size = getDeviceSize().toString();
      } else {
        size = "n/a";
      }
      free = "n/a";
    }
    return new String[] {name, getTypeString(), getStateString(), Boolean.toString(mounted), size, free};
  }

  private String getiSCSIPath() {
    // ip-HOST:3260-iscsi-TARGET-lun-#
    int lun = 1;  //TODO : how to determine lun?
    return String.format("ip-%s:3260-iscsi-%s-lun-%d", host, target, lun);
  }

  /** Get device name. */
  private String getDevice() {
    switch (type) {
      case TYPE_LOCAL_PART: return path;
      case TYPE_LOCAL_DISK: return path;
      case TYPE_NFS: return host + ":" + path;
      case TYPE_ISCSI: return "/dev/disk/by-path/" + getiSCSIPath();
      case TYPE_GLUSTER: return path;
      case TYPE_CEPHFS: return path;
    }
    return null;
  }

  private String getDevice2() {
    switch (type) {
      case TYPE_GLUSTER: return host + ":/" + name;
    }
    return null;
  }

  private static String resolveLinks(String file) {
    Path path = new File(file).toPath();
    if (Files.isSymbolicLink(path)) {
      try {
        String resolved = path.toRealPath().toString();
        return resolved;
      } catch (Exception e) {
        JFLog.log(e);
        return file;
      }
    } else {
      return file;
    }
  }

  /** Returns UUID of a disk device in /dev. */
  public static String getDiskUUID(String dev) {
    //lsblk available options : UUID, PARTUUID (Linux uses UUID in /dev/disk/by-uuid)
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/lsblk", "-o", "UUID", dev}, true);
    if (sp.getErrorLevel() != 0) {
      JFLog.log("Error:" + output);
      return null;
    }
    /*
    UUID
    8-4-4-4-12
    */
    String[] lns = output.split("\n");
    return lns[1].trim();
  }

  public static String[] listLocalParts() {
    /*
NAME TYPE  SIZE MOUNTPOINTS
sda  disk  100G
sda1 part  512M /boot/efi
sda2 part 98.5G /
sda3 part  976M [SWAP]
sdb  disk 10.8T
sdb1 part 10.8T /volumes/sdb1
sdd  disk    1T /volumes/ocfs2
sde  disk    1T
sr0  rom  1024M
    */
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/lsblk", "-l", "-o", "NAME,TYPE,SIZE,MOUNTPOINTS"}, true);
    if (sp.getErrorLevel() != 0) {
      JFLog.log("Error:" + output);
      return null;
    }
    String[] lns = output.split("\n");
    ArrayList<String> parts = new ArrayList<>();
    for(int a=1;a<lns.length;a++) {
      String[] fs = lns[a].split("[ ]+");
      //NAME,TYPE,SIZE,MOUNTPOINTS
      String name = fs[0];
      String type = fs[1];
      String size = fs[2];
      if (fs.length > 3) continue;  //already mounted
      if (!type.equals("part")) continue;  //not a partition
      parts.add("/dev/" + name);
    }
    return parts.toArray(JF.StringArrayType);
  }

  /** Mount iSCSI,Gluster,Ceph pools. start() will already mount other types. */
  public boolean mount() {
    if (!isMountedManually()) return false;
    String dev = null;
    String mount = null;
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("/usr/bin/mount");
    switch (type) {
      case TYPE_ISCSI: {
        dev = getDevice();
        mount = getPath();
        break;
      }
      case TYPE_GLUSTER: {
        dev = getDevice2();
        mount = getPath();
        cmd.add("-t");
        cmd.add("glusterfs");
        break;
      }
      case TYPE_CEPHFS: {
        dev = getDevice();
        mount = getPath();
        cmd.add("-t");
        cmd.add("ceph");
        break;
      }
    }
    cmd.add(dev);
    cmd.add(mount);
    new File(mount).mkdir();
    ShellProcess sp = new ShellProcess();
    JFLog.log("cmd=" + JF.join(" ", cmd.toArray(JF.StringArrayType)));
    String output = sp.run(cmd.toArray(JF.StringArrayType), true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

  /** Unmount iSCSI, Gluster pools. stop() will already unmount other types. */
  public boolean unmount() {
    if (type != TYPE_ISCSI && type != TYPE_GLUSTER) return false;
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/bin/umount", getDevice()}, true);
    JFLog.log(output);
    new File(getPath()).delete();
    return sp.getErrorLevel() == 0;
  }

  public boolean mounted() {
    String dev = null;
    String path = getPath();
    switch (type) {
      default:
        dev = getDevice();
        dev = resolveLinks(dev);
        break;
      case TYPE_GLUSTER:
        dev = getDevice2();
      break;
    }
    try {
      FileInputStream fis = new FileInputStream("/proc/mounts");
      byte[] data = fis.readAllBytes();
      fis.close();
      String[] lns = new String(data).split("\n");
      for(String ln : lns) {
        //device mount ext4 rw,nosuid,nodev,noexec,relatime 0 0
        if (ln.length() == 0) continue;
        String[] fs = ln.split("[ ]");
        if (fs[0].equals(dev)) return true;
        if (fs.length == 1) continue;
        if (fs[1].equals(path)) return true;
      }
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return false;
  }

  public static final int FORMAT_EXT4 = 1;  //local only
  public static final int FORMAT_GFS2 = 2;  //remote distributed (red hat)
  public static final int FORMAT_OCFS2 = 3;  //remote distributed (oracle)
  public static final int FORMAT_XFS = 4;  //gluster only

  public static String getFormatString(int fmt) {
    switch (fmt) {
      case FORMAT_EXT4: return "ext4";
      case FORMAT_GFS2: return "gfs2";
      case FORMAT_OCFS2: return "ocfs2";
      case FORMAT_XFS: return "xfs";
    }
    return null;
  }

  /** Format local partition or iSCSI target. */
  public boolean format(int fmt) {
    if (type == TYPE_NFS) return false;  //can not format NFS
    if (type == TYPE_CEPHFS) return false;  //can not format CephFS
    if (fmt < 1 || fmt > 4) return false;
    ArrayList<String> cmd = new ArrayList<>();
    switch (fmt) {
      default:
        return false;
      case FORMAT_EXT4:
        for(String str : new String[] {"/usr/sbin/mkfs", "-t", getFormatString(fmt), "-F", getDevice()}) {
          cmd.add(str);
        }
        break;
      case FORMAT_OCFS2:
        for(String str : new String[] {"/usr/sbin/mkfs", "-t", getFormatString(fmt), "-F", getDevice()}) {
          cmd.add(str);
        }
        break;
      case FORMAT_GFS2:
        for(String str : new String[] {"/usr/sbin/mkfs", "-t", getFormatString(fmt), "-O", "-p", "lock_dlm", "-t", "jfkvm:" + name, "-j", "3", getDevice()}) {
          cmd.add(str);
        }
        break;
      case FORMAT_XFS:
        for(String str : new String[] {"/usr/sbin/mkfs", "-t", getFormatString(fmt), "-f", getDevice()}) {
          cmd.add(str);
        }
        break;
    }
    ShellProcess sp = new ShellProcess();
    JFLog.log("cmd=" + JF.join(" ", cmd.toArray(JF.StringArrayType)));
    String output = sp.run(cmd.toArray(JF.StringArrayType), true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

  /** Returns mount path. */
  public String getPath() {
    return "/volumes/" + name;
  }

  /** Returns gluster brick path. */
  public String getGlusterBrick() {
    return "/gluster/volumes/" + name;
  }

  /** Returns gluster volume path within brick path. */
  public String getGlusterVolume() {
    return "/gluster/volumes/" + name + "/" + name;
  }

  public static boolean format_supported(int fmt) {
    switch (fmt) {
      case FORMAT_EXT4: return true;
      case FORMAT_GFS2: return new File("/usr/sbin/mkfs.gfs2").exists();
      case FORMAT_OCFS2: return new File("/usr/sbin/mkfs.ocfs2").exists();
      case FORMAT_XFS: return new File("/usr/sbin/mkfs.xfs").exists();
    }
    return false;
  }

  /** Get device size. */
  public Size getDeviceSize() {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/sbin/blockdev", "--getsize64", getDevice()}, true);
    output = JF.filter(output, JF.filter_numeric);
    if (output.length() == 0) return new Size(0);
    return new Size(Long.valueOf(output));
  }

  /** Get mounted file system total size. */
  public Size getTotalSize() {
    try {
      File file = new File(getPath());
      return new Size(file.getTotalSpace());
    } catch (Exception e) {
      JFLog.log(e);
      return new Size(0);
    }
  }

  /** Get mounted file system free size. */
  public Size getFreeSize() {
    try {
      File file = new File(getPath());
      return new Size(file.getFreeSpace());
    } catch (Exception e) {
      JFLog.log(e);
      return new Size(0);
    }
  }

  /** Gets size of files in folder (not recursive) */
  public Size getFolderSize(String folder) {
    File[] files = new File(getPath() + "/" + folder).listFiles();
    long total = 0;
    for(File file : files) {
      if (file.isDirectory()) continue;
      total += file.length();
    }
    return new Size(total);
  }

  public boolean gluster_volume_create(String[] hosts) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("/usr/sbin/gluster");
    cmd.add("volume");
    cmd.add("create");
    cmd.add(name);
    cmd.add("replica");
    cmd.add(Integer.toString(hosts.length));
    for(String host : hosts) {
      cmd.add(host + ":" + getGlusterVolume());
    }
    cmd.add("force");
    JFLog.log("cmd=" + JF.join(" ", cmd.toArray(JF.StringArrayType)));
    String output = sp.run(cmd.toArray(JF.StringArrayType), true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

  public boolean isMountedManually() {
    if (type == TYPE_ISCSI) return true;
    if (type == TYPE_GLUSTER) return true;
    if (type == TYPE_CEPHFS) return true;
    return false;
  }

  private String createXML() {
    switch (type) {
      case TYPE_ISCSI: return createXML_iSCSI(name, uuid, host, target, user);
      case TYPE_NFS: return createXML_NFS(name, uuid, host, path, getPath());
      case TYPE_LOCAL_PART: return createXML_Local_Part(name, uuid, path, getPath());
      case TYPE_LOCAL_DISK: return createXML_Local_Disk(name, uuid, path, getPath());
      case TYPE_GLUSTER: return createXML_Gluster(name, uuid, path, getGlusterBrick());
      case TYPE_CEPHFS: return createXML_Ceph(name, uuid, path, getPath());
    }
    return null;
  }

  private static String createXML_iSCSI(String name, String uuid, String host, String target, String chap_user) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='iscsi' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    sb.append("  <source>");
    sb.append("    <host name='" + host + "'/>");
    sb.append("    <device path='" + target + "'/>");
    if (chap_user != null) {
      sb.append("      <auth type='chap' username='" + chap_user + "'>");
      sb.append("        <secret type='iscsi' usage='" + name + "'/>");
      sb.append("      </auth>");
    }
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>/dev/disk/by-path</path>");
    sb.append("  </target>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static String createXML_NFS(String name, String uuid, String host, String srcPath, String mountPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='netfs' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    sb.append("  <source>");
    sb.append("    <host name='" + host + "'/>");
    sb.append("    <dir path='" + srcPath + "'/>");
    sb.append("    <format type='nfs'/>");
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>" + mountPath + "</path>");
    sb.append("  </target>");
    sb.append("  <fs:mount_opts>");
    sb.append("    <fs:option name='noexec'/>");
    sb.append("    <fs:option name='nosuid'/>");
    sb.append("    <fs:option name='nodev'/>");
    sb.append("  </fs:mount_opts>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static String createXML_Local_Part(String name, String uuid, String localDevice, String mountPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='fs' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    sb.append("  <source>");
    sb.append("    <device path='" + localDevice + "'/>");
    sb.append("    <format type='ext4'/>");
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>" + mountPath + "</path>");
    sb.append("  </target>");
    sb.append("  <fs:mount_opts>");
    sb.append("    <fs:option name='noexec'/>");
    sb.append("    <fs:option name='nosuid'/>");
    sb.append("    <fs:option name='nodev'/>");
    sb.append("  </fs:mount_opts>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static String createXML_Local_Disk(String name, String uuid, String localDevice, String mountPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='disk' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    sb.append("  <source>");
    sb.append("    <device path='" + localDevice + "'/>");
    sb.append("    <format type='gpt'/>");
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>/dev</path>");
    sb.append("  </target>");
    sb.append("  <fs:mount_opts>");
    sb.append("    <fs:option name='noexec'/>");
    sb.append("    <fs:option name='nosuid'/>");
    sb.append("    <fs:option name='nodev'/>");
    sb.append("  </fs:mount_opts>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static String createXML_Gluster(String name, String uuid, String localDevice, String brickPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='fs' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    sb.append("  <source>");
    sb.append("    <device path='" + localDevice + "'/>");
    sb.append("    <format type='xfs'/>");
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>" + brickPath + "</path>");
    sb.append("  </target>");
    sb.append("  <fs:mount_opts>");
    sb.append("    <fs:option name='noexec'/>");
    sb.append("    <fs:option name='nosuid'/>");
    sb.append("    <fs:option name='nodev'/>");
    sb.append("  </fs:mount_opts>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static String createXML_Ceph(String name, String uuid, String path, String mountPath) {
    //libvirt currently does NOT support ceph, so a fake "dir" is used for now.
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type='dir' xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <uuid>" + uuid + "</uuid>");
    if (false) {
      sb.append("  <source>");
      sb.append("    <device path='" + path + "'/>");
      sb.append("    <format type='ceph/>");
      sb.append("  </source>");
    }
    sb.append("  <target>");
    sb.append("    <path>" + mountPath + "</path>");
    sb.append("  </target>");
    sb.append("  <fs:mount_opts>");
    sb.append("    <fs:option name='noexec'/>");
    sb.append("    <fs:option name='nosuid'/>");
    sb.append("    <fs:option name='nodev'/>");
    sb.append("  </fs:mount_opts>");
    sb.append("</pool>");
    return sb.toString();
  }

  private static void usage() {
    JFLog.log("Usage: Storage {command} [args]");
    JFLog.log("  mount {type} ...");
    JFLog.log("    mount part {device}");
//    JFLog.log("    mount disk {device}");
    JFLog.log("    mount iscsi {host} {target}");
    JFLog.log("    mount nfs {host} {srcPath}");
//    Gluster ???
    JFLog.log("    mount cephfs {device}");
    JFLog.log("  unmount {mountPath}");
    System.exit(1);
  }

  public static void main(String[] args) {
    if (args.length == 0) {
      usage();
    }
    switch (args[0]) {
      case "mount":
        if (args.length < 3) usage();
        switch (args[1]) {
          case "iscsi": {
            if (args.length < 4) usage();
            Storage store = new Storage(TYPE_ISCSI, args[1], null);
            store.host = args[2];
            store.target = args[3];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
          case "nfs": {
            if (args.length < 4) usage();
            Storage store = new Storage(TYPE_NFS, args[1], null);
            store.host = args[2];
            store.path = args[3];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
          case "part": {
            if (args.length < 3) usage();
            Storage store = new Storage(TYPE_LOCAL_PART, args[1], null);
            store.path = args[2];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
/*
          case "disk": {
            if (args.length < 3) usage();
            Storage store = new Storage(TYPE_LOCAL_DISK, args[1], null);
            store.path = args[2];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
*/
          case "cephfs": {
            if (args.length < 3) usage();
            Storage store = new Storage(TYPE_CEPHFS, args[1], null);
            store.path = args[2];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
        }
        break;
      case "unmount":
        if (args.length < 2) usage();
        try {
          Storage store = new Storage(0, args[1], null);
          boolean res = store.unregister();
          JFLog.log("res=" + res);
        } catch (Exception e) {
          JFLog.log(e);
        }
        break;
      default:
        JFLog.log("Unknown command:" + args[0]);
        break;
    }
  }
}
