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
          session.client_proxy = new Reader('>', client, real_socket);
          session.client_proxy.start();
          session.proxy_client = new Reader('<', real_socket, client);
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
    private byte[] data = new byte[64 * 1024];  //max wayland packet size
    private int data_offset;
    private int[] data_len = new int[1];
    private int[] fds = new int[128];
    private int fds_offset;
    private int[] fds_len = new int[1];
    private char dir;
    public Reader(char dir, UnixSocket src, UnixSocket dst) {
      this.dir = dir;
      this.src = src;
      this.dst = dst;
    }
    public void run() {
      while (active) {
        try {
          data_offset = 0;
          data_len[0] = 8;
          fds_offset = 0;
          fds_len[0] = fds.length;
          //read header (8 bytes)
          int toread = 8;
          int actread = 0;
          while (actread < toread) {
            boolean read = src.read(data, data_offset, data_len, fds, fds_offset, fds_len);
            if (debug) {
              JFLog.log(log, dir + ": read:" + data_len[0] + "," + fds_len[0]);
            }
            if (!read) {
              throw new Exception(dir + ":WLProxy:Error:read() failed");
            }
            if (data_len[0] == 0) {
              throw new Exception(dir + ":WLProxy:Error:read==0:src disconnected");
            }
            actread += data_len[0];
            data_offset += data_len[0];
            data_len[0] = toread - actread;
            fds_offset += fds_len[0];
            fds_len[0] = fds.length - fds_offset;
          }
          if (false) {
            int obj_id = LE.getuint32(data, 0);
            int opcode = LE.getuint16(data, 4);
          }
          toread = LE.getuint16(data, 6);  //packet size including header
          if (debug) {
            JFLog.log(log, dir + ":packet.length=" + toread);
          }
          //read full packet
          data_len[0] = toread - actread;
          while (actread < toread) {
            boolean read = src.read(data, data_offset, data_len, fds, fds_offset, fds_len);
            if (debug) {
              JFLog.log(log, dir + ": read:" + data_len[0] + "," + fds_len[0]);
            }
            if (!read) {
              throw new Exception(dir + ":WLProxy:Error:read() failed");
            }
            if (data_len[0] == 0) {
              throw new Exception(dir + ":WLProxy:Error:read==0:src disconnected");
            }
            actread += data_len[0];
            data_offset += data_len[0];
            data_len[0] = toread - actread;
            fds_offset += fds_len[0];
            fds_len[0] = fds.length - fds_offset;
          }
          //write full packet (with any fds read)
          data_offset = 0;
          data_len[0] = toread;
          fds_len[0] = fds_offset;
          fds_offset = 0;
          boolean write = dst.write(data, data_offset, data_len, fds, fds_offset, fds_len);
          if (debug) {
            JFLog.log(log, dir + ":write:" + data_len[0] + "," + fds_len[0]);
          }
          if (!write) {
            JFLog.log(log, "WLProxy:write() failed");
          }
          if (fds_len[0] > 0) {
            //close fds received after they have been transferred
            src.close(fds, 0, fds_len[0]);
          }
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
      active = false;
      src.close();
      dst.close();
    }
  }
}
