package javaforce.linux.wl;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

import javaforce.*;

/** Wayland client.
 *
 * WIP (incomplete)
 *
 * Sends/receives Wayland messages over Unix Socket.
 *
 * @author pquiring
 */

public class WLClient {
  public static boolean debug = true;
  public static boolean debug_io = false;
  public static boolean debug_packet = true;

  private SocketChannel socket;
  private Reader reader;

  private HashMap<Integer, WLObject> objects = new HashMap<>();  //client side objects (id)
  private HashMap<Integer, WLGlobal> globals = new HashMap<>();  //server side objects (name)

  private Object next_id_lock = new Object();
  /** Next client side id. */
  private int next_id = 2;  //1 = reserved for wl_display
  private WLDisplay display;

  public WLClient() {
    display = new WLDisplay(this, 1);
  }

  public boolean connect() {
    String path = System.getenv("XDG_RUNTIME_DIR");
    String wayland_display = System.getenv("WAYLAND_DISPLAY");
    if (path == null || wayland_display == null) {
      JFLog.log("Wayland Client:socket not found");
      return false;
    }
    if (!path.endsWith("/")) {
      path += "/";
    }
    path += wayland_display;
    JFLog.log("Client:socket=" + path);
    try {
      UnixDomainSocketAddress addr = UnixDomainSocketAddress.of(path);
      socket = SocketChannel.open(StandardProtocolFamily.UNIX);
      socket.connect(addr);
      reader = new Reader();
      reader.start();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      if (socket != null) {
        try {socket.close();} catch (Exception e2) {}
      }
      socket = null;
      return false;
    }
  }

  public boolean disconnect() {
    if (socket == null) return false;
    try {
      socket.close();
      socket = null;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public WLDisplay get_display() {
    return display;
  }

  public int read(byte[] data, int offset, int length) {
    try {
      int read = socket.read(ByteBuffer.wrap(data, offset, length));
      if (debug_io) JFLog.log("read=" + read);
      return read;
    } catch (Exception e) {
      JFLog.log(e);
      return -1;
    }
  }

  public boolean write(byte[] data, int offset, int length) {
    if (debug_packet) JFLog.log("write.packet=", data, offset, length);
    try {
      int write = socket.write(ByteBuffer.wrap(data, offset, length));
      if (debug_io) JFLog.log("write=" + write);
      return write == length;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Returns next client side id. */
  public int get_next_id() {
    int id;
    synchronized (next_id_lock) {
      id = next_id++;
    }
    return id;
  }

  /** Add client side WLObject. */
  public void setObject(int id, WLObject obj) {
    objects.put(id, obj);
  }

  /** Get client side WLObject. */
  public WLObject getObject(int id) {
    return objects.get(id);
  }

  /** Remove client side WLObject. */
  public void removeObject(int id) {
    objects.remove(id);
  }

  /** Add server side Object name. */
  public void setGlobal(int name, String iface, int ver) {
    WLGlobal global = new WLGlobal();
    global.name = name;
    global.iface = iface;
    global.ver = ver;
    globals.put(name, global);
  }

  /** Get server side global object. */
  public WLGlobal getGlobal(int name) {
    return globals.get(name);
  }

  /** Remove server side Object name. */
  public void removeGlobal(int name) {
    globals.remove(name);
  }

  private class Reader extends Thread {
    private byte[] pkt = new byte[1024];
    public void run() {
      int pktpos = 0;
      int pktlen = 0;
      try {
        while (socket != null) {
          int read = read(pkt, pktpos, 8 - pktlen);
          if (read > 0) {
            pktpos += read;
            pktlen += read;
          }
          if (pktlen == 8) {
            int id = LE.getuint32(pkt, 0);
            int opcode = LE.getuint16(pkt, 4);
            int size = LE.getuint16(pkt, 6);
            while (pkt.length < size) {
              //grow pkt if needed
              byte[] new_pkt = new byte[pkt.length << 1];
              System.arraycopy(pkt, 0, new_pkt, 0, pkt.length);
              pkt = new_pkt;
            }
            while (pktlen < size) {
              read = read(pkt, pktpos, size - pktlen);
              if (read == -1) throw new Exception("read failed");
              if (read > 0) {
                pktpos += read;
                pktlen += read;
              }
            }
            WLObject object = objects.get(id);
            if (object == null) {
              JFLog.log("Wayland.Client:Error:id not registered:" + id);
              continue;
            }
            if (debug_packet) JFLog.log("read.packet=", pkt, 0, size);
            object.dispatchEvent(opcode, pkt, 8, size);
            pktpos = 0;
            pktlen = 0;
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}
