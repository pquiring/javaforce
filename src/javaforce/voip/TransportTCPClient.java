package javaforce.voip;

/** SIP TCP Transport
 *
 * Client needs to only connect to one endpoint.
 *
 * @author pquiring
 *
 * Created : Jan 29, 2014
 */

import java.net.*;
import java.io.*;

import javaforce.*;

public class TransportTCPClient implements Transport {
  protected boolean active = false;
  protected Socket socket;
  protected OutputStream os;
  protected InputStream is;
  protected InetAddress remotehost;
  protected int remoteport;
  protected String localhost;
  protected int localport;
  private boolean error;
  private TransportInterface iface;

  public static boolean debug = false;

  public String getName() { return "TCP"; }

  public boolean open(String localhost, int localport, TransportInterface iface) {
    this.localhost = localhost;
    this.localport = localport;
    this.iface = iface;
    try {
      if (debug) JFLog.log("TransportTCPClient.open()");
      socket = new Socket();
      socket.setSoLinger(true, 0);  //allow to reuse socket again without waiting
      socket.bind(new InetSocketAddress(localhost, localport));
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public boolean open(Socket socket) {
    this.localhost = socket.getLocalAddress().getHostAddress();
    this.localport = socket.getLocalPort();
    this.socket = socket;
    return true;
  }

  public boolean close() {
    active = false;
    try {
      if (socket != null) {
        socket.close();
        socket = null;
      }
    } catch (Exception e) {
      if (debug) JFLog.log(e);
    }
    return true;
  }

  public boolean send(byte[] data, int off, int len, InetAddress host, int port) {
    try {
      if (!active) {
        connect(host, port);
      }
      os.write(data, off, len);
      os.flush();
    } catch (Exception e) {
      if (debug) JFLog.log(e);
      return false;
    }
    return true;
  }

  private byte[] extra = null;

  private int detectLength(byte[] data, int off, int len) {
    for(int a=0;a<len-3;a++) {
      if (
        (data[off+a+0] == '\r') &&
        (data[off+a+1] == '\n') &&
        (data[off+a+2] == '\r') &&
        (data[off+a+3] == '\n')
      ) {
        return a+4;
      }
    }
    return -1;
  }

  public boolean receive(Packet packet) {
    try {
      if (is == null) {
        packet.length = 0;
        JF.sleep(100);  //wait for first send()
        return false;
      }
      if (extra != null) {
        System.arraycopy(extra, 0, packet.data, 0, extra.length);
        packet.length = extra.length;
        extra = null;
      } else {
        int read = is.read(packet.data);
        if (read == -1) throw new Exception("Transport read failed");
        packet.length = read;
      }
      int plen, tlen;
      do {
        //detect end of packet (double \r\n)
        plen = detectLength(packet.data, 0, packet.length);
        if (plen == -1) {
          //not enough read (frag?)
          int read = is.read(packet.data, packet.length, packet.data.length - packet.length);
          if (read == -1) throw new Exception("Transport read failed");
          packet.length += read;
        }
      } while (active && plen == -1);
      tlen = plen;
      //now find Content-Length:
      String[] msg = new String(packet.data, 0, plen).split("\r\n");
      String clenstr = HTTP.getParameter(msg, "Content-Length");
      if (clenstr == null) HTTP.getParameter(msg, "l");
      if (clenstr != null) {
        int clen = JF.atoi(clenstr);
        tlen += clen;
      }
      while (active && packet.length < tlen) {
        //not enough read (frag?)
        int read = is.read(packet.data, packet.length, packet.data.length - packet.length);
        if (read == -1) throw new Exception("Transport read failed");
        packet.length += read;
      }
      if (packet.length > tlen) {
        //extra read (from next packet)
        extra = new byte[packet.length - tlen];
        System.arraycopy(packet.data, tlen, extra, 0, packet.length - tlen);
        packet.length = tlen;
      }
      //host and port never change
      packet.host = remotehost.getHostAddress();
      packet.port = remoteport;
    } catch (Exception e) {
      error = true;
      if (debug) JFLog.log("TransportTCPClient:Connection lost");
      return false;
    }
    return true;
  }

  public boolean disconnect(String host, int port) {
    return false;
  }

  protected void connect(InetAddress host, int port) throws Exception {
    if (debug) JFLog.log("Connect:" + host.getHostAddress() + ":" + port);
    this.remotehost = host;
    this.remoteport = port;
    socket.connect(new InetSocketAddress(host, port), 5000);
    os = socket.getOutputStream();
    is = socket.getInputStream();
    active = true;
  }

  public boolean error() {
    return error;
  }

  public String[] getClients() {
    return new String[0];
  }

  public void setReceiveBufferSize(int size) {
  }
}
