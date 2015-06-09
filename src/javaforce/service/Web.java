package javaforce.service;

/**
 * Mini web service
 *
 * Created : Aug 23, 2013
 */

import java.io.*;
import java.net.*;
import java.security.*;
import javax.net.ssl.*;

import javaforce.*;

public class Web {
  private WebHandler api;
  private WebSocketHandler wsapi;
  private ServerSocket ss;
  private boolean active = true;

  public static boolean config_enable_gzip = true;

  public boolean start(WebHandler api, int port, boolean secure) {
    this.api = api;
    try {
      if (secure) {
        ss = SSLServerSocketFactory.getDefault().createServerSocket(port);
      } else {
        ss = new ServerSocket(port);
      }
      new Server().start();
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
    try {
      ss.close();
    } catch (Exception e) {
    }
  }
  private class Server extends Thread {
    public void run() {
      Socket s;
      while (active) {
        try {
          s = ss.accept();
          new Connection(s).start();
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
  }
  private class Connection extends Thread {
    private Socket s;
    private InputStream is;
    public void run() {
      //read request and pass to WebHandler
      try {
        StringBuilder request = new StringBuilder();
        is = s.getInputStream();
        while (s.isConnected()) {
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
              WebSocket socket = new WebSocket();
              socket.is = is;
              socket.os = res.os;
              socket.url = req.fields0[1];
              if (wsapi != null && wsapi.doWebSocketConnect(socket)) {
                sendWebSocketAccepted(req, res);
                processWebSocket(socket);
              } else {
                sendWebSocketDenied(req, res);
              }
              break;
            }
            else if (req.fields0[0].equals("GET")) {
              req.method = "GET";
              api.doGet(req, res);
            }
            else if (req.fields0[0].equals("POST")) {
              req.method = "POST";
              api.doPost(req, res);
            }
            else {
              res.setStatus(501, "Error - Unsupported Method");
            }
            res.writeAll(req);
            if (req.fields0[2].equals("HTTP/1.0")) break;
            request.setLength(0);
          }
        }
        s.close();
      } catch (Exception e) {
      }
    }
    public Connection(Socket s) {
      this.s = s;
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
      byte sha1[] = md.digest(inKey.getBytes());
      String base64 = new String(Base64.encode(sha1));
      return base64;
    }
    private void sendWebSocketAccepted(WebRequest req, WebResponse res) {
      res.setStatus(101, "Switching Protocols");
      res.addHeader("Upgrade: websocket");
      res.addHeader("Connection: Upgrade");
      String inKey = null;
      for(int a=0;a<req.fields.length;a++) {
        String field = req.fields[a];
        if (field.startsWith("Sec-WebSocket-Key:")) {
          inKey = field.substring(18).trim();
          break;
        }
      }
      String outKey = encodeKey(inKey);
      res.addHeader("Sec-WebSocket-Accept: " + outKey);
      res.addHeader("Sec-WebSocket-Version: 13");
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
      byte mask[] = new byte[4];
      try {
        while (true) {
          int opcode = socket.is.read();
          if (opcode == -1) throw new Exception("socket error");
          boolean fin = (opcode & 0x80) == 0x80;
          opcode &= 0xf;
          if (opcode == WebSocket.TYPE_CLOSE) break; //closed
          long length = 0;
          int len7 = socket.is.read();
          if (len7 == -1) throw new Exception("socket error");
          boolean hasMask = (len7 & 0x80) == 0x80;
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
              mask[a] = (byte)mask8;
            }
          } else {
            throw new Exception("WebSocket message without mask");
          }
          if (length > 16777216) {
            throw new Exception("WebSocket message > 16MB");
          }
          //now read data
          byte data[] = JF.readAll(socket.is, (int)length);
          //unmask data
          for(int a=0;a<length;a++) {
            data[a] ^= mask[a % 4];
          }
          if (opcode == WebSocket.TYPE_PING) {
            //ping message
            socket.write(data, WebSocket.TYPE_PONG);
            continue;
          }
          if (opcode > 0x8) continue;  //other control message
          wsapi.doWebSocketMessage(socket, data, opcode);
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      wsapi.doWebSocketClosed(socket);
    }
  }
}
