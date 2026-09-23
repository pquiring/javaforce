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
  public static boolean debug_rw = false;

  private String proxy_socket_addr;
  private UnixSocket proxy_socket;  //wayland-99
  private boolean active;
  private Server server;
  private String real_wayland_display;

  private static int log = 99;

  private WLNotify notify;

  public WLProxy(WLNotify notify) {
    this.notify = notify;
  }

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

  public void log(String msg) {
    JFLog.log(log, msg);
  }

  public void log(String msg, byte[] data, int offset, int length) {
    JFLog.log(log, msg, data, offset, length);
  }

  public void log(Exception e) {
    JFLog.log(log, e);
  }

  private ArrayList<Session> sessions = new ArrayList<>();
  private Object lock = new Object();

  /** Returns number of sessions.  There typically is only one active. */
  public int getSessionCount() {
    return sessions.size();
  }

  public Session getSession(int idx) {
    return sessions.get(idx);
  }

  /** Gets the WLClient.
   */
  public WLClient getClient() {
    return sessions.get(0).client;
  }

  public class Session extends Thread {

    public String real_socket_addr;
    public UnixSocket real_socket;  //wayland-0

    public UnixSocket client_socket;

    public Reader client_proxy;
    public Reader proxy_client;

    private WLClient client = new WLClient(notify);

    public void run() {
      try { client_proxy.join(); } catch (Exception e) {}
      try { proxy_client.join(); } catch (Exception e) {}
      synchronized (lock) {
        sessions.remove(this);
      }
    }

    public void cancel() {
      if (client_socket != null) {
        try { client_socket.close(); } catch (Exception e) {}
        client_socket = null;
      }
      if (real_socket != null) {
        try { real_socket.close(); } catch (Exception e) {}
        real_socket = null;
      }
    }

    public WLDisplay get_display() {
      return client.get_display();
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
          session.client.setLog(log);

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

          //set WLClient socket to allow injecting requests
          session.client.setSocket(session.real_socket);
          session.client.set_enable_write(false);

          session.client_socket = client;
          session.client_proxy = new Reader('>', session, session.client_socket, session.real_socket);
          session.client_proxy.start();
          session.proxy_client = new Reader('<', session, session.real_socket, session.client_socket);
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

  public class Reader extends Thread {
    private Session session;
    private UnixSocket src;
    private UnixSocket dst;
    private byte[] data = new byte[64 * 1024];  //max wayland packet size
    private int data_offset;
    private int[] data_len = new int[1];
    private int[] fds = new int[128];
    private int fds_offset;
    private int[] fds_len = new int[1];
    private char dir;
    public Reader(char dir, Session session, UnixSocket src, UnixSocket dst) {
      this.dir = dir;
      this.session = session;
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
          int pktlen = 8;
          int actread = 0;
          while (actread < pktlen) {
            boolean read = src.read(data, data_offset, data_len, fds, fds_offset, fds_len);
            if (debug_rw) {
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
            data_len[0] = pktlen - actread;
            fds_offset += fds_len[0];
            fds_len[0] = fds.length - fds_offset;
          }
          int id = LE.getuint32(data, 0);
          int opcode = LE.getuint16(data, 4);
          pktlen = LE.getuint16(data, 6);  //packet size including header
          if (debug_rw) {
            JFLog.log(log, dir + ":packet.length=" + pktlen);
          }
          //read full packet
          data_len[0] = pktlen - actread;
          while (actread < pktlen) {
            boolean read = src.read(data, data_offset, data_len, fds, fds_offset, fds_len);
            if (debug_rw) {
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
            data_len[0] = pktlen - actread;
            fds_offset += fds_len[0];
            fds_len[0] = fds.length - fds_offset;
          }
          //process packet
          switch (dir) {
            case '>':
              //client to real wayland
              if (debug) JFLog.log(log, "request:" + id + "." + opcode);
              //NOTE : the client does not have any open sockets so these requests are simulated just to track the session state
              if (!session.client.dispatchRequest(id, opcode, pktlen, data)) {
                JFLog.log(log, "WLProxy:Error:failed to dispatchRequest:" + id + "." + opcode);
              }
              break;
            case '<':
              //real wayland to client
              if (debug) JFLog.log(log, "event:" + id + "." + opcode);
              if (!session.client.dispatchEvent(id, opcode, pktlen, data)) {
                JFLog.log(log, "WLProxy:Error:failed to dispatchEvent:" + id + "." + opcode);
              }
              break;
          }
          //write full packet (with any fds read)
          data_offset = 0;
          data_len[0] = pktlen;
          fds_len[0] = fds_offset;
          fds_offset = 0;
          boolean write = dst.write(data, data_offset, data_len, fds, fds_offset, fds_len);
          if (debug_rw) {
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
  }
}
