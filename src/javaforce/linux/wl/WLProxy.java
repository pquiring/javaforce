package javaforce.linux.wl;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import javaforce.*;

/** WLProxy.
 *
 * Fake Wayland server that redirects to real Wayland server.
 *
 * @author pquiring
 */

public class WLProxy {
  private SocketChannel real_socket;  //wayland-0
  private SocketChannel proxy_socket;  //wayland-99
  private Reader real_proxy;
  private Reader proxy_real;
  private boolean active;

  public boolean start() {
    String real_path = System.getenv("XDG_RUNTIME_DIR");
    String real_wayland_display = System.getenv("WAYLAND_DISPLAY");
    if (real_path == null || real_wayland_display == null) {
      JFLog.log("WLProxy:socket not found");
      return false;
    }
    if (!real_path.endsWith("/")) {
      real_path += "/";
    }
    real_path += real_wayland_display;
    JFLog.log("WLProxy:server.socket=" + real_path);

    try {
      UnixDomainSocketAddress real_addr = UnixDomainSocketAddress.of(real_path);
      real_socket = SocketChannel.open(StandardProtocolFamily.UNIX);
      real_socket.connect(real_addr);
    } catch (Exception e) {
      JFLog.log(e);
      if (real_socket != null) {
        try {real_socket.close();} catch (Exception e2) {}
      }
      real_socket = null;
      return false;
    }

    String proxy_path = System.getenv("XDG_RUNTIME_DIR");
    String proxy_wayland_display = "wayland-99";
    if (proxy_path == null || proxy_wayland_display == null) {
      JFLog.log("WLProxy:socket not found");
      return false;
    }
    if (!proxy_path.endsWith("/")) {
      proxy_path += "/";
    }
    proxy_path += proxy_wayland_display;
    JFLog.log("WLProxy:proxy.socket=" + proxy_path);

    try {
      UnixDomainSocketAddress proxy_addr = UnixDomainSocketAddress.of(proxy_path);
      proxy_socket = SocketChannel.open(StandardProtocolFamily.UNIX);
      proxy_socket.bind(proxy_addr);
    } catch (Exception e) {
      JFLog.log(e);
      if (proxy_socket != null) {
        try {proxy_socket.close();} catch (Exception e2) {}
      }
      proxy_socket = null;
      return false;
    }

    active = true;

    //forward traffic
    proxy_real = new Reader(proxy_socket, real_socket, true);
    proxy_real.start();

    real_proxy = new Reader(real_socket, proxy_socket, false);
    real_proxy.start();

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
  }

  public class Reader extends Thread {
    private SocketChannel src;
    private SocketChannel dst;
    private boolean monitor;
    private byte[] packet = new byte[128 * 1024];  //max unix socket packet size
    public Reader(SocketChannel src, SocketChannel dst, boolean monitor) {
      this.src = src;
      this.dst = dst;
      this.monitor = monitor;
    }
    public void run() {
      ByteBuffer bb = ByteBuffer.wrap(packet);
      while (active) {
        try {
          bb.clear();
          src.read(bb);
          if (monitor) {
            //TODO
          }
          dst.write(bb);
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
}
