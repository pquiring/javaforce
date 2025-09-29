package service;

/** Ceph Storage
 *
 * Currently supports Ceph-FS.
 *
 * Ceph-iSCSI would require a shared file system which is too complex.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;
import javaforce.webui.tasks.*;

import service.*;

public class Ceph {
  public static boolean exists() {
    return new File("/etc/ceph/ceph.conf").exists();
  }

  public static boolean setup(Task task) {
    Host[] hosts = Config.current.getHosts(Host.TYPE_ON_PREMISE, 3.0f);
    try {
      for(Host host : hosts) {
        if (host.getStorageIP() == null || host.ip_storage == null || host.ip_storage.length() == 0) {
          throw new Exception("ceph:host is missing Storage IP:" + host.hostname);
        }
      }
      for(Host host : hosts) {
        if (!host.setCephStart()) {
          throw new Exception("ceph:host rejected ceph setup:" + host.hostname);
        }
      }
      //bootstrap ceph
      task.setStatus("Running cephadm...");
      {
        ShellProcess sp = new ShellProcess();
        String output = sp.run(new String[] {
          "/usr/sbin/cephadm",
          "bootstrap",
          "--mon-ip" ,
          Config.current.ip_storage,
          "--initial-dashboard-user", "admin",
          "--initial-dashboard-password", Config.passwd.password,
//          "--cleanup-on-failure"  //removed since v19
        }, true);
        int status = sp.getErrorLevel();
        JFLog.log("cephadm:" + output);
        if (status != 0) {
          throw new Exception("ceph:cephadm failed!");
        }
      }
      task.setStatus("Installing sshkeys...");
      File sshkeyfile = new File("/etc/ceph/ceph.pub");
      if (!sshkeyfile.exists()) {
        throw new Exception("ceph:/etc/ceph/ceph.pub not found!");
      }
      FileInputStream sshkeyin = new FileInputStream(sshkeyfile);
      String sshkey = new String(sshkeyin.readAllBytes());
      sshkeyin.close();
      for(Host host : hosts) {
        if (!host.addsshkey(sshkey)) {
          throw new Exception("ceph:failed to copy ssh key to host:" + host);
        }
      }
      task.setStatus("Joining other nodes...");
      String hosts_str;
      {
        StringBuilder hosts_sb = new StringBuilder();
        //add self to hosts list
        hosts_sb.append(Linux.getHostname());
        for(Host host : hosts) {
          ShellProcess sp = new ShellProcess();
          //ceph orch host add {hostname} {ip} _admin [labels...]
          String output = sp.run(new String[] {"/usr/bin/ceph", "orch", "host" , "add", host.hostname, host.ip_storage, "_admin"}, true);
          int status = sp.getErrorLevel();
          JFLog.log("ceph orch host add:" + output);
          if (status != 0) {
            throw new Exception("ceph:ceph orch host add failed!");
          }
          if (hosts_sb.length() > 0) {
            hosts_sb.append(",");
          }
          hosts_sb.append(host.hostname);
        }
        hosts_str = hosts_sb.toString();
      }
      task.setStatus("Adding available disks...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph orch apply osd --all-available-devices
        String output = sp.run(new String[] {"/usr/bin/ceph", "orch", "apply", "osd", "--all-available-devices"}, true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph orch apply osd:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph orch apply osd failed!");
        }
      }
      task.setStatus("Adding monitors...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph orch apply mon cephnode1,cephnode2,cephnode3
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("/usr/bin/ceph");
        cmd.add("orch");
        cmd.add("apply");
        cmd.add("mon");
        cmd.add(hosts_str);
        String output = sp.run(cmd.toArray(JF.StringArrayType), true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph orch apply mon:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph orch apply mon failed!");
        }
      }
      task.setStatus("Adding managers...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph orch apply mgr cephnode1,cephnode2,cephnode3
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("/usr/bin/ceph");
        cmd.add("orch");
        cmd.add("apply");
        cmd.add("mgr");
        cmd.add(hosts_str);
        String output = sp.run(cmd.toArray(JF.StringArrayType), true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph orch apply mgr:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph orch apply mgr failed!");
        }
      }
      task.setStatus("Creating pool...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph osd pool create {pool-name}
        String output = sp.run(new String[] {"/usr/bin/ceph", "osd", "pool", "create", "cephpool"}, true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph osd pool create:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph osd pool create failed!");
        }
      }
      task.setStatus("Creating file system...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph fs new {fs-name} {pool-name} {pool-name}
        String output = sp.run(new String[] {"/usr/bin/ceph", "fs", "new", "cephfs", "cephpool", "cephpool"}, true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph fs new:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph fs new failed!");
        }
      }
      task.setStatus("Adding meta data services...");
      {
        ShellProcess sp = new ShellProcess();
        //ceph orch apply mds {fs-name} cephnode1,cephnode2,cephnode3
        ArrayList<String> cmd = new ArrayList<>();
        cmd.add("/usr/bin/ceph");
        cmd.add("orch");
        cmd.add("apply");
        cmd.add("mds");
        cmd.add("cephfs");
        cmd.add(hosts_str);
        String output = sp.run(cmd.toArray(JF.StringArrayType), true);
        int status = sp.getErrorLevel();
        JFLog.log("ceph orch apply mds:" + output);
        if (status != 0) {
          throw new Exception("ceph:ceph orch apply mds failed!");
        }
      }
      for(Host host : hosts) {
        host.setCephComplete();
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      task.setStatus(e.getMessage());
      for(Host host : hosts) {
        host.setCephComplete();
      }
      return false;
    }
  }
  public static String getStatus() {
    try {
      ShellProcess sp = new ShellProcess();
      String output = sp.run(new String[] {"/usr/bin/ceph", "status"}, true);
      int error_level = sp.getErrorLevel();
      if (error_level != 0) throw new Exception("ceph status failed!");
      output = output.replaceAll("\n", " ");
      int i1 = output.indexOf("HEALTH_");
      if (i1 == -1) throw new Exception("HEALTH not found");
      int i2 = output.indexOf(' ', i1);
      return output.substring(i1, i2);
    } catch (Exception e) {
      return "Unknown";
    }
  }
  public static void main(String[] args) {
    //test addsshkeys
    try {
      Paths.init();
      Config.load();
      Host[] hosts = Config.current.hosts.values().toArray(new Host[0]);
      JFLog.log("Installing sshkeys...");
      File sshkeyfile = new File("/etc/ceph/ceph.pub");
      if (!sshkeyfile.exists()) {
        JFLog.log("ceph:/etc/ceph/ceph.pub not found!");
        return;
      }
      FileInputStream sshkeyin = new FileInputStream(sshkeyfile);
      String sshkey = new String(sshkeyin.readAllBytes());
      sshkeyin.close();
      for(Host host : hosts) {
        if (!host.addsshkey(sshkey)) {
          JFLog.log("ceph:failed to copy ssh key to host:" + host);
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
