package service;

/** Gluster API
 *
 * [deprecated]
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Gluster {
  public static boolean exists() {
    return new File("/var/run/glusterd.pid").exists();
  }

  public static String getStatus() {
    try {
      ShellProcess sp = new ShellProcess();
      String output = sp.run(new String[] {"/usr/sbin/gluster", "volume", "info"}, true);
      int error_level = sp.getErrorLevel();
      if (error_level != 0) throw new Exception("gluster status failed!");
      String[] lns = output.split("\n");
      for(String ln : lns) {
        if (ln.startsWith("Status:")) {
          return ln.substring(7).trim();
        }
      }
      return "Unknown";
    } catch (Exception e) {
      return "Unknown";
    }
  }

  public static String[] getPeers() {
    ShellProcess sp = new ShellProcess();
    String out = sp.run(new String[] {"/usr/sbin/gluster", "peer", "status"}, true);
    String[] lns = out.split("\n");
    ArrayList<String> hosts = new ArrayList<>();
    for(String ln : lns) {
      if (ln.startsWith("Hostname:")) {
        ln = ln.substring(9);
        hosts.add(ln.trim());
      }
    }
    return hosts.toArray(JF.StringArrayType);
  }

  public static boolean ready() {
    Host[] hosts = Config.current.getHosts();
    String[] peers = Gluster.getPeers();
    for(Host host : hosts) {
      if (!host.online) return false;
      if (!host.valid) return false;
      boolean peer_ok = false;
      for(String peer : peers) {
        if (peer.equals(host.host)) {
          peer_ok = true;
          break;
        }
      }
      if (!peer_ok) return false;
    }
    return true;
  }

  public static boolean probe(String host) {
    ShellProcess sp = new ShellProcess();
    String output = sp.run(new String[] {"/usr/sbin/gluster", "peer", "probe", host}, true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

  public static boolean volume_create(String[] hosts, String name, String gluster_volume) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("/usr/sbin/gluster");
    cmd.add("volume");
    cmd.add("create");
    cmd.add(name);
    cmd.add("replica");
    cmd.add(Integer.toString(hosts.length));
    for(String host : hosts) {
      cmd.add(host + ":" + gluster_volume);
    }
    cmd.add("force");
    JFLog.log("cmd=" + JF.join(" ", cmd.toArray(JF.StringArrayType)));
    String output = sp.run(cmd.toArray(JF.StringArrayType), true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

  public static boolean volume_start(String name) {
    ShellProcess sp = new ShellProcess();
    ArrayList<String> cmd = new ArrayList<>();
    cmd.add("/usr/sbin/gluster");
    cmd.add("volume");
    cmd.add("start");
    cmd.add(name);
    JFLog.log("cmd=" + JF.join(" ", cmd.toArray(JF.StringArrayType)));
    String output = sp.run(cmd.toArray(JF.StringArrayType), true);
    JFLog.log(output);
    return sp.getErrorLevel() == 0;
  }

}
