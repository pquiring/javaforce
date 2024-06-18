package service;

/** Check hosts periodically.
 *
 * @author peter.quiring
 */

import java.util.*;

import javaforce.*;

public class Hosts extends Thread {
  public static Hosts hosts;
  private boolean active = true;
  private Object secs_lock = new Object();
  private int secs60 = 60;

  public static void init() {
    hosts = new Hosts();
    hosts.start();
  }

  public void cancel() {
    active = false;
  }

  public void run() {
    HTTP.setTimeout(5000);
    while (active) {
      synchronized (secs_lock) {
        secs60++;
        if (secs60 > 60) {
          if (!check_hosts) {
            new CheckHosts().start();
          }
          secs60 = 0;
        }
      }
    }
  }

  public boolean check_hosts;

  private String[] gluster_hosts = new String[0];

  public class CheckHosts extends Thread {
    //run every minute
    public void run() {
      check_hosts = true;
      try {
        //get gluster status
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
        gluster_hosts = hosts.toArray(JF.StringArrayType);
      } catch (Exception e) {
        JFLog.log(e);
      }
      try {
        Host[] hosts = Config.current.getHosts();
        for(Host host : hosts) {
          try {
            if (!host.checkValid()) throw new Exception("invalid");
            if (!host.getVersion()) throw new Exception("offline");
            host.online = true;
            if (host.hostname == null) {
              host.hostname = host.getHostname();
              Config.current.save();
            }
            host.gluster = getGlusterState(host.host);
          } catch (Exception e) {
            JFLog.log(e);
            host.online = false;
            host.valid = false;
            host.gluster = false;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      Config.current.validateHosts();
      check_hosts = false;
    }
  }

  private boolean getGlusterState(String host) {
    for(String gluster_host : gluster_hosts) {
      if (gluster_host.equals(host)) return true;
    }
    return false;
  }

  public void check_now() {
    synchronized (secs_lock) {
      secs60 = 60;
    }
  }
}
