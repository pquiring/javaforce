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
      JF.sleep(1000);
    }
  }

  public boolean check_hosts;

  public class CheckHosts extends Thread {
    //run every minute
    public void run() {
      check_hosts = true;
      boolean gluster = Gluster.exists();
      boolean ceph = Ceph.exists();
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
            if (gluster) {
              host.gluster_status = host.getGlusterStatus();
            } else {
              host.gluster_status = "false";
            }
            if (ceph) {
              host.ceph_status = host.getCephStatus();
            } else {
              host.ceph_status = "false";
            }
          } catch (Exception e) {
            JFLog.log(e);
            host.online = false;
            host.valid = false;
            host.gluster_status = "false";
            host.ceph_status = "false";
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      Config.current.validateHosts();
      check_hosts = false;
    }
  }

  public void check_now() {
    synchronized (secs_lock) {
      secs60 = 60;
    }
  }
}
