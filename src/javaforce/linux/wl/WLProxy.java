package javaforce.linux.wl;

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;

/** WLProxy.
 *
 * Fake Wayland server that redirects to real Wayland server.
 *
 * @author pquiring
 */

public class WLProxy {
  public static boolean debug = false;

  private String real_socket_addr;
  private UnixSocket real_socket;  //wayland-0
  private String proxy_socket_addr;
  private UnixSocket proxy_socket;  //wayland-99
  private boolean active;
  private Server server;

  private static int log = 99;

  public boolean start(String real_wayland_display) {
    if (debug) {
      JFLog.init(log, System.getenv("HOME") + "/wlproxy.log", true);
    }
    String real_path = System.getenv("XDG_RUNTIME_DIR");
    if (real_path == null || real_wayland_display == null) {
      JFLog.log(log, "WLProxy:socket not found");
      return false;
    }
    if (!real_path.endsWith("/")) {
      real_path += "/";
    }
    String temp_path = real_path + "wayland-90";
    real_path += real_wayland_display;
    JFLog.log(log, "WLProxy:server.socket=" + real_path);

    try {
      real_socket_addr = real_path;
      real_socket = new UnixSocket();
      if (!real_socket.open()) throw new Exception("Unable to alloc unix socket");
      real_socket.bind(temp_path);  //maybe not be necessary
      if (!real_socket.connect(real_path)) throw new Exception("Unable to connect to real wayland socket");
    } catch (Exception e) {
      JFLog.log(log, e);
      if (real_socket != null) {
        try {real_socket.close();} catch (Exception e2) {}
      }
      real_socket = null;
      return false;
    }

    String proxy_path = System.getenv("XDG_RUNTIME_DIR");
    String proxy_wayland_display = "wayland-99";
    if (proxy_path == null || proxy_wayland_display == null) {
      JFLog.log(log, "WLProxy:socket not found");
      return false;
    }
    if (!proxy_path.endsWith("/")) {
      proxy_path += "/";
    }
    proxy_path += proxy_wayland_display;
    JFLog.log(log, "WLProxy:proxy.socket=" + proxy_path);

    try {
      proxy_socket_addr = proxy_path;
      proxy_socket = new UnixSocket();
      if (!proxy_socket.open()) throw new Exception("Unable to alloc unix socket");
      if (!proxy_socket.bind(proxy_path)) throw new Exception("Unable to bind to socket:" + proxy_path);
      if (!proxy_socket.listen()) throw new Exception("Unable to listen on unix socket");
    } catch (Exception e) {
      JFLog.log(log, e);
      if (proxy_socket != null) {
        try {proxy_socket.close();} catch (Exception e2) {}
      }
      proxy_socket = null;
      return false;
    }

    active = true;

    server = new Server();
    server.start();

    return true;
  }

  public void stop() {
    active = false;
    if (real_socket != null) {
      try { real_socket.close(); } catch (Exception e) {}
      real_socket = null;
    }
    if (proxy_socket != null) {
      try { proxy_socket.close(); } catch (Exception e) {}
      proxy_socket = null;
    }
    new File(proxy_socket_addr).delete();
  }

  public void setLog(int log) {
    this.log = log;
  }

  private ArrayList<Session> sessions = new ArrayList<>();

  private class Session {
    UnixSocket client;
    Reader client_proxy;
    Reader proxy_client;
  }

  public class Server extends Thread {
    public void run() {
      while (active) {
        try {
          UnixSocket client = proxy_socket.accept();
          if (client == null) {
            continue;
          }
          Session session = new Session();
          session.client = client;
          session.client_proxy = new Reader(client, real_socket, true);
          session.client_proxy.start();
          session.proxy_client = new Reader(real_socket, client, true);
          session.proxy_client.start();
          sessions.add(session);
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
    }
  }

  public class Reader extends Thread {
    private UnixSocket src;
    private UnixSocket dst;
    private boolean monitor;
    private int[] data_len = new int[1];
    private byte[] data = new byte[128 * 1024];  //max unix socket packet size
    private int[] fds_len = new int[1];
    private int[] fds = new int[128];
    public Reader(UnixSocket src, UnixSocket dst, boolean monitor) {
      this.src = src;
      this.dst = dst;
      this.monitor = monitor;
    }
    public void run() {
      while (active) {
        try {
          data_len[0] = data.length;
          fds_len[0] = fds.length;
          src.read(data_len, data, fds_len, fds);
          if (debug) {
            JFLog.log(log, (monitor ? ">" : "<") + ": read:" + data_len[0] + "," + fds_len[0]);
          }
          if (monitor) {
            //TODO
          }
          dst.write(data_len, data, fds_len, fds);
          if (debug) {
            JFLog.log(log, (monitor ? ">" : "<") + ":write:" + data_len[0] + "," + fds_len[0]);
          }
          if (fds_len[0] > 0) {
            //close fds received after they have been transferred
            src.close(fds_len[0], fds);
          }
        } catch (Exception e) {
          JFLog.log(log, e);
          return;
        }
      }
    }
  }
}
