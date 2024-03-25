package javaforce.service;

/**
 * Mini web service
 *
 * Created : Aug 23, 2013
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import javax.net.ssl.*;

import javaforce.*;

public class WebServer {
  private WebHandler api;
  private WebSocketHandler wsapi;
  private ServerSocket ss;
  private boolean active = true;
  private ArrayList<Connection> clients = new ArrayList<>();
  private Object clientsLock = new Object();

  public static boolean config_enable_gzip = true;
  public static boolean debug = false;

  /** Start web server on non-secure port. */
  public boolean start(WebHandler api, int port) {
    return start(api, port, null);
  }

  /** Start web server on non-secure or secure port.
   * Random keys are generated for secure ports.
   * This method is highly deprecated, please use start() with KeyMgmt keys.
   */
  @Deprecated
  public boolean start(WebHandler api, int port, boolean secure) {
    if (secure) {
      KeyMgmt keys = null;
      try {
        String dname = "CN=javaforce.sourceforge.net, O=server, OU=webserver, C=CA";
        String keyfile = JF.getConfigPath() + "/" + System.getProperty("java.app.name") + ".key";
        String password = "password";
        if (new File(keyfile).exists()) {
          //load existing keys
          keys = new KeyMgmt();
          FileInputStream fis = new FileInputStream(keyfile);
          keys.open(fis, password.toCharArray());
          fis.close();
        } else {
          //generate random keys
          keys = KeyMgmt.create(keyfile, "webserver", dname, password);
        }
      } catch (Exception e) {
        JFLog.log(e);
        return false;
      }
      return start(api, port, keys);
    } else {
      return start(api, port, null);
    }
  }

  /** Start web server on secure port using provided keys. */
  public boolean start(WebHandler api, int port, KeyMgmt keys) {
    this.api = api;
    try {
      if (keys != null) {
        ss = JF.createServerSocketSSL(port, keys);
      } else {
        ss = new ServerSocket(port);
      }
      new Server(this).start();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }
  public void setWebSocketHandler(WebSocketHandler wsapi) {
    this.wsapi = wsapi;
  }
  public void stop() {
    active = false;
    if (ss != null) {
      try { ss.close(); } catch (Exception e) {}
      ss = null;
    }
    //NOTE:this could be Connection thread callback so stop threads in another thread
    new Thread() {public void run() {
      while (clients.size() > 0) {
        synchronized (clientsLock) {
          if (clients.size() == 0) break;
          Connection conn = clients.get(0);
          conn.cancel();
        }
        JF.sleep(100);
      }
    }}.start();
  }
  private static class Server extends Thread {
    private WebServer web;
    public Server(WebServer web) {
      this.web = web;
    }
    public void run() {
      setName("WebServer.Server");
      Socket s;
      while (web.active) {
        try {
          s = web.ss.accept();
          Connection conn = new Connection(web, s);
          synchronized (web.clientsLock) {
            web.clients.add(conn);
          }
          conn.start();
        } catch (SocketException e) {
          if (debug) JFLog.log("WebServer.Server:disconnected");
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
  private static class Connection extends Thread {
    private Socket s;
    private InputStream is;
    private WebServer web;
    public Connection(WebServer web, Socket s) {
      this.web = web;
      this.s = s;
    }
    public void run() {
      setName("WebServer.Connection");
      //read request and pass to WebHandler
      try {
        StringBuilder request = new StringBuilder();
        is = s.getInputStream();
        while (web.active && s.isConnected()) {
          int ch = is.read();
          if (ch == -1) break;
          request.append((char)ch);
          if (request.toString().endsWith("\r\n\r\n")) {
            WebRequest req = new WebRequest();
            req.request = request.toString();
            req.fields = req.request.split("\r\n");
            req.is = is;
            req.serverIP = s.getLocalAddress().getHostAddress();
            if (req.serverIP.equals("0:0:0:0:0:0:0:1")) req.serverIP = "127.0.0.1";
            req.serverPort = s.getLocalPort();
            req.remoteIP = s.getInetAddress().getHostAddress();
            req.remotePort = s.getPort();
            WebResponse res = new WebResponse();
            res.os = s.getOutputStream();
            req.fields0 = req.fields[0].split(" ");
            req.init(res);
            if (isWebSocketRequest(req)) {
              WebSocket socket = new WebSocket(s.getInetAddress().getHostAddress());
              socket.is = is;
              socket.os = res.os;
              socket.url = req.fields0[1];
              if (web.wsapi != null && web.wsapi.doWebSocketConnect(socket)) {
                sendWebSocketAccepted(req, res);
                processWebSocket(socket);
              } else {
                sendWebSocketDenied(req, res);
              }
              break;
            }
            else if (req.fields0[0].equals("GET")) {
              req.method = "GET";
              web.api.doGet(req, res);
            }
            else if (req.fields0[0].equals("POST")) {
              req.method = "POST";
              web.api.doPost(req, res);
            }
            else {
              res.setStatus(501, "Error - Unsupported Method");
            }
            res.writeAll(req);
            if (req.fields0[2].equals("HTTP/1.0")) break;
            request.setLength(0);
          }
        }
        if (s != null) {
          s.close();
          s = null;
        }
      } catch (SSLHandshakeException ssl) {
        if (debug) JFLog.log("WebServer.Connection:SSL error");
      } catch (SocketException e) {
        if (debug) JFLog.log("WebServer.Connection:disconnected");
      } catch (Exception e) {
        e.printStackTrace();
      }
      synchronized (web.clientsLock) {
        web.clients.remove(this);
      }
    }
    public void cancel() {
      if (s != null) {
        try {s.close();} catch (Exception e) {}
        s = null;
      }
    }
    private boolean isWebSocketRequest(WebRequest req) {
      boolean upgrade = false;
      boolean websocket = false;
      boolean haveKey = false;
      for(int a=0;a<req.fields.length;a++) {
        String field = req.fields[a];
        if (field.startsWith("Connection:") && field.indexOf("Upgrade") != -1) upgrade = true;
        if (field.startsWith("Upgrade:") && field.indexOf("websocket") != -1) websocket = true;
        if (field.startsWith("Sec-WebSocket-Key:")) haveKey = true;
      }
      return upgrade && websocket && haveKey;
    }
    private String encodeKey(String inKey) {
      //input: Sec-WebSocket-Key: inkey
      //output: Sec-WebSocket-Accept: outkey
      // outkey = base64(SHA1(inkey + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11'))
      inKey += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
      MessageDigest md = null;
      try {
        md = MessageDigest.getInstance("SHA-1");
      }
      catch(NoSuchAlgorithmException e) {
        JFLog.log(e);
      }
      byte[] sha1 = md.digest(inKey.getBytes());
      String base64 = new String(javaforce.Base64.encode(sha1));
      return base64;
    }
    private void sendWebSocketAccepted(WebRequest req, WebResponse res) {
      res.setStatus(101, "Switching Protocols");
      res.addHeader("Upgrade: websocket");
      res.addHeader("Connection: Upgrade");
      String inKey = null;
      String[] protocols = null;
      for(int a=0;a<req.fields.length;a++) {
        String field = req.fields[a];
        if (field.startsWith("Sec-WebSocket-Key:")) {
          inKey = field.substring(18).trim();
        }
        if (field.startsWith("Sec-WebSocket-Protcol:")) {
          protocols = field.substring(22).trim().split(",");
        }
      }
      String outKey = encodeKey(inKey);
      res.addHeader("Sec-WebSocket-Accept: " + outKey);
      try {
        res.writeAll(req);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    private void sendWebSocketDenied(WebRequest req, WebResponse res) {
      res.setStatus(403, "Access Denied");
      try {
        res.writeAll(req);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    private void processWebSocket(WebSocket socket) {
      //keep reading packets and deliver to WebHandler
      byte[] maskKey = new byte[4];
      try {
        while (web.active) {
          int opcode = socket.is.read();
          if (opcode == -1) throw new Exception("socket error");
          boolean fin = (opcode & 0x80) == 0x80;
          opcode &= 0xf;
          if (opcode == WebSocket.TYPE_CLOSE) break; //closed
          long length = 0;
          int len7 = socket.is.read();
          if (len7 == -1) throw new Exception("socket error");
          boolean hasMask = (len7 & WebSocket.MASK) == WebSocket.MASK;
          len7 &= 0x7f;
          switch (len7) {
            case 126:  //16bits = payload
              for(int a=0;a<2;a++) {
                int len8 = socket.is.read();
                if (len8 == -1) throw new Exception("socket error");
                length <<= 8;
                length |= len8;
              }
              break;
            case 127:  //64bits = payload
              for(int a=0;a<8;a++) {
                long len8 = socket.is.read();
                if (len8 == -1) throw new Exception("socket error");
                length <<= 8;
                length |= len8;
              }
              break;
            default:
              length = len7;
          }
          if (hasMask) {
            for(int a=0;a<4;a++) {
              int mask8 = socket.is.read();
              if (mask8 == -1) throw new Exception("socket error");
              maskKey[a] = (byte)mask8;
            }
          } else {
            throw new Exception("WebSocket message without mask");
          }
          if (length > 16777216) {
            throw new Exception("WebSocket message > 16MB");
          }
          //now read data
          byte[] data = JF.readAll(socket.is, (int)length);
          //unmask data
          for(int a=0;a<length;a++) {
            data[a] ^= maskKey[a % 4];
          }
          if (opcode == WebSocket.TYPE_PING) {
            //ping message
            socket.write(data, WebSocket.TYPE_PONG);
            continue;
          }
          if (opcode > 0x8) continue;  //other control message
          web.wsapi.doWebSocketMessage(socket, data, opcode);
        }
      } catch (SocketException e) {
        if (debug) JFLog.log("WebServer.Websocket:disconnected");
      } catch (Exception e) {
        JFLog.log(e);
      }
      web.wsapi.doWebSocketClosed(socket);
    }
  }
  /** Returns a chunk header for a block of data for transmission in Transfer-Encoding: chunked
   * Make sure to send \r\n after actual block of data.
   */
  public static byte[] chunkHeader(byte[] in) {
    return String.format("%x\r\n", in.length).getBytes();
  }
}
