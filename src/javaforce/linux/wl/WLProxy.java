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

  private String proxy_socket_addr;
  private UnixSocket proxy_socket;  //wayland-99
  private boolean active;
  private Server server;
  private String real_wayland_display;

  private static int log = 99;

  public boolean start(String real_wayland_display) {
    if (debug) {
      JFLog.init(log, System.getenv("HOME") + "/wlproxy.log", true);
    }

    this.real_wayland_display = real_wayland_display;

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
    if (proxy_socket != null) {
      try { proxy_socket.close(); } catch (Exception e) {}
      proxy_socket = null;
    }
    new File(proxy_socket_addr).delete();
    stopSessions();
  }

  private void stopSessions() {
    for(Session session : sessions) {
      session.cancel();
    }
  }

  public void setLog(int log) {
    this.log = log;
  }

  private ArrayList<Session> sessions = new ArrayList<>();
  private Object lock = new Object();

  private class Session extends Thread {

    public String real_socket_addr;
    public UnixSocket real_socket;  //wayland-0

    public UnixSocket client;

    public Reader client_proxy;
    public Reader proxy_client;

    public void run() {
      try { client_proxy.join(); } catch (Exception e) {}
      try { proxy_client.join(); } catch (Exception e) {}
      synchronized (lock) {
        sessions.remove(this);
      }
    }

    public void cancel() {
      if (client != null) {
        try { client.close(); } catch (Exception e) {}
        client = null;
      }
      if (real_socket != null) {
        try { real_socket.close(); } catch (Exception e) {}
        real_socket = null;
      }
    }
  }

  public class Server extends Thread {
    public void run() {
      int session_counter = 100;
      while (active) {
        try {
          UnixSocket client = proxy_socket.accept();
          if (client == null) {
            continue;
          }

          Session session = new Session();

          String real_path = System.getenv("XDG_RUNTIME_DIR");
          if (real_path == null || real_wayland_display == null) {
            JFLog.log(log, "WLProxy:socket not found");
            continue;
          }
          if (!real_path.endsWith("/")) {
            real_path += "/";
          }
          String temp_path = real_path + "wayland-" + (session_counter++);
          real_path += real_wayland_display;
          JFLog.log(log, "WLProxy:server.socket=" + real_path);

          try {
            session.real_socket_addr = real_path;
            session.real_socket = new UnixSocket();
            if (!session.real_socket.open()) throw new Exception("Unable to alloc unix socket");
            session.real_socket.bind(temp_path);  //may not be necessary
            if (!session.real_socket.connect(real_path)) throw new Exception("Unable to connect to real wayland socket");
          } catch (Exception e) {
            JFLog.log(log, e);
            if (session.real_socket != null) {
              try {session.real_socket.close();} catch (Exception e2) {}
            }
            session.real_socket = null;
            continue;
          }

          session.client = client;
          session.client_proxy = new Reader('>', session.client, session.real_socket);
          session.client_proxy.start();
          session.proxy_client = new Reader('<', session.real_socket, session.client);
          session.proxy_client.start();
          synchronized (lock) {
            sessions.add(session);
          }
          session.start();
        } catch (Exception e) {
          JFLog.log(log, e);
        }
      }
    }
  }

  public class Reader extends Thread implements WLNotify {
    private UnixSocket src;
    private UnixSocket dst;
    private byte[] data = new byte[64 * 1024];  //max wayland packet size
    private int data_offset;
    private int[] data_len = new int[1];
    private int[] fds = new int[128];
    private int fds_offset;
    private int[] fds_len = new int[1];
    private char dir;
    private WLClient client = new WLClient(this);
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
          int id = LE.getuint32(data, 0);
          int opcode = LE.getuint16(data, 4);
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
          //process packet
          switch (dir) {
            case '>':
              //client to real wayland
              break;
            case '<':
              //real wayland to client
              client.dispatch(id, opcode, toread, data);
              break;
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
            throw new Exception(dir + ":WLProxy:write() failed");
          }
          if (fds_len[0] > 0) {
            //close fds received after they have been transferred
            src.close(fds, 0, fds_len[0]);
          }
        } catch (Exception e) {
          JFLog.log(log, e);
          break;
        }
      }
      active = false;
      src.close();
      dst.close();
    }

    public void onEvent(String cls, String method, Object[] args) {
      JFLog.log(log, "onEvent:" + cls + "." + method);
      switch (cls) {
        case "wl_registry": {
          switch (method) {
            case "global": {
              int name = (Integer)args[0];
              String iface = (String)args[1];
              int ver = (Integer)args[2];
              switch (iface) {
                case "zwlr_foreign_toplevel_manager_v1": {
                  break;
                }
                case "wl_seat": {
                  break;
                }
              }
              break;
            }
          }
          break;
        }
        case "zwlr_foreign_toplevel_manager_v1": {
          break;
        }
        case "zwlr_foreign_toplevel_handle_v1": {
          break;
        }
      }
    }
  }
}
