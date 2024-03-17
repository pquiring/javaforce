package javaforce.vm;

/** Storage pool registered with libvirt.
 *
 *  Pools are mounted in /volumes
 */

import java.io.*;

import javaforce.*;

public class Storage implements Serializable {
  private static final long serialVersionUID = 1L;

  public Storage(int type, String name, String uuid) {
    this.type = type;
    this.name = name;
    if (uuid == null) {
      this.uuid = UUID.generate();
    } else {
      this.uuid = uuid;
    }
  }

  public int type;
  public String name;
  public String uuid;

  public String host;  //nfs, iscsi
  public String target, initiator;  //iscsi
  public String path;  //nfs, local (device)
  private String user, pass;  //iscsi chap (TODO)

  public static final int TYPE_LOCAL = 1;
  public static final int TYPE_NFS = 2;
  public static final int TYPE_ISCSI = 3;

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
    return nregister(createXML());
  }

  private native static boolean nunregister(String name);
  public boolean unregister() {
    return nunregister(name);
  }

  //virDomainCreate
  private native static boolean nstart(String name);
  public boolean start() {
    return nstart(name);
  }

  //virDomainShutdown()
  private native static boolean nstop(String name);
  public boolean stop() {
    return nstop(name);
  }

  private native static int ngetState(String name);
  public int getState() {
    return ngetState(name);
  }

  public static final int TYPE_EXT4 = 1;

  /** Format local partition or iscsi disk. */
  public native boolean format(String path, int type);

  public String getPath() {
    return "/volumes/" + name;
  }

  private String createXML() {
    switch (type) {
      case TYPE_ISCSI: return createXML_iSCSI(name, host, target, initiator, getPath(), user, pass);
      case TYPE_NFS: return createXML_NFS(name, host, path, getPath());
      case TYPE_LOCAL: return createXML_Local(name, host, path, getPath());
    }
    return null;
  }

  public static String createXML_iSCSI(String name, String host, String target, String initiator, String mountPath, String chap_user, String chap_pass) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type=\"iscsi-direct\" xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <source>");
    sb.append("    <host name=\"" + host + "\"/>");
    sb.append("    <device path=\"" + target + "\"/>");
    sb.append("    <initiator>");
    sb.append("      <iqn name=\"" + initiator + "\"/>");
    sb.append("    </initiator>");
    if (chap_user != null && chap_pass != null) {
      //TODO : pass libsecret object with actual 'pass'
      //SEE : https://libvirt.org/formatsecret.html
      sb.append("      <auth type='chap' username='" + chap_user + "'>");
      sb.append("        <secret usage='libvirtiscsi'/>");
      sb.append("      </auth>");
    }
    sb.append("  </source>");
    sb.append("  <target>");
    sb.append("    <path>" + mountPath + "</path>");
    sb.append("  </target>");
    sb.append("</pool>");
    return sb.toString();
  }

  public static String createXML_NFS(String name, String host, String srcPath, String mountPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type=\"netfs\" xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <source>");
    sb.append("    <host name=\"" + host + "\"/>");
    sb.append("    <device path=\"" + srcPath + "\"/>");
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

  public static String createXML_Local(String name, String host, String localDevice, String mountPath) {
    StringBuilder sb = new StringBuilder();
    sb.append("<pool type=\"logical\" xmlns:fs='http://libvirt.org/schemas/storagepool/fs/1.0'>");
    sb.append("  <name>" + name + "</name>");
    sb.append("  <source>");
    sb.append("    <device path=\"" + localDevice + "\"/>");
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

  private static void usage() {
    JFLog.log("Usage: Storage {command} [args]");
    JFLog.log("  mount {type} ...");
    JFLog.log("    mount iscsi {host} {target} {initiator}");
    JFLog.log("    mount nfs {host} {srcPath}");
    JFLog.log("    mount local {device}");
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
            if (args.length < 6) usage();
            Storage store = new Storage(TYPE_ISCSI, args[1], null);
            store.host = args[2];
            store.target = args[3];
            store.initiator = args[4];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
          case "nfs": {
            if (args.length < 7) usage();
            Storage store = new Storage(TYPE_NFS, args[1], null);
            store.host = args[2];
            store.path = args[3];
            boolean res = store.register();
            JFLog.log("res=" + res);
            break;
          }
          case "local": {
            if (args.length < 5) usage();
            Storage store = new Storage(TYPE_LOCAL, args[1], null);
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
