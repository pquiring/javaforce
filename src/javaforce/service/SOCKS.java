package javaforce.service;

/** Socks 4 Server
 *
 * No auth
 * No config
 *
 * Port 1080
 *
 * https://en.wikipedia.org/wiki/SOCKS
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;

public class SOCKS extends Thread {

  private ServerSocket ss;
  private volatile boolean active;
  private static ArrayList<Session> sessions = new ArrayList<Session>();
  private static Object lock = new Object();
  private static boolean debug = false;

  public static void addSession(Session sess) {
    synchronized(lock) {
      sessions.add(sess);
    }
  }

  public static void removeSession(Session sess) {
    synchronized(lock) {
      sessions.remove(sess);
    }
  }

  public void run() {
    JFLog.init(JF.getLogPath() + "/jfsocks.log", true);
    try {
      ss = new ServerSocket(1080);
      active = true;
      while (active) {
        Socket s = ss.accept();
        Session sess = new Session(s);
        addSession(sess);
        sess.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void close() {
    active = false;
    try { ss.close(); } catch (Exception e) {}
    synchronized(lock) {
      Session[] list = sessions.toArray(new Session[0]);
      for(int a=0;a<list.length;a++) {
        list[a].close();
      }
      sessions.clear();
    }
  }

  public static class Session extends Thread {
    private Socket c;
    private Socket o;
    private ProxyData pd1, pd2;
    public Session(Socket s) {
      c = s;
    }
    public void close() {
      try { c.close(); } catch (Exception e) {}
      if (o != null) {
        try { o.close(); } catch (Exception e) {}
      }
      if (pd1 != null) {
        pd1.close();
      }
      if (pd2 != null) {
        pd2.close();
      }
    }
    public void run() {
      //request = 0x04 0x01 port16 ip32 user_id_null_terminated...
      //reply   = 0x00 0x5a reserved[6]   //0x5b = failed
      byte[] req = new byte[1500];
      byte[] reply = new byte[8];
      int reqSize = 0;
      boolean connected = false;
      InputStream cis = null;
      OutputStream cos = null;
      try {
        if (debug) JFLog.log("Session start");
        cis = c.getInputStream();
        cos = c.getOutputStream();
        //read request
        while (c.isConnected()) {
          int read = cis.read(req, reqSize, 1500 - reqSize);
          if (read < 0) throw new Exception("bad read");
          reqSize += read;
          if (reqSize > 8 && req[reqSize] == 0) break;  //valid request size
        }
        if (req[0] != 0x04) throw new Exception("bad request:not SOCKS4 request");
        if (req[1] != 0x01) throw new Exception("bad request:not open socket request");
        int port = BE.getuint16(req, 2);
        String ip4 = String.format("%d.%d.%d.%d", req[4], req[5], req[6], req[7]);
        o = new Socket(ip4, port);
        connected = true;
        reply[0] = 0x00;
        reply[1] = 0x5a;  //success
        cos.write(reply);
        //now just proxy data back and forth
        pd1 = new ProxyData(c,o,"1");
        pd1.start();
        pd2 = new ProxyData(o,c,"2");
        pd2.start();
        pd1.join();
        pd2.join();
        if (debug) JFLog.log("Session end");
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        if (!connected) {
          reply[0] = 0x00;
          reply[1] = 0x5b;  //failed
          try {cos.write(reply);} catch (Exception e2) {}
        }
        close();
      }
      removeSession(this);
    }
  }

  public static class ProxyData extends Thread {
    private Socket sRead;
    private Socket sWrite;
    private volatile boolean active;
    private String name;
    public ProxyData(Socket sRead, Socket sWrite, String name) {
      this.sRead = sRead;
      this.sWrite = sWrite;
      this.name = name;
    }
    public void run() {
      try {
        InputStream is = sRead.getInputStream();
        OutputStream os = sWrite.getOutputStream();
        byte[] buf = new byte[1500];
        active = true;
        while (active) {
          int read = is.read(buf);
          if (read < 0) throw new Exception("bad read:pd" + name);
          if (read > 0) {
            os.write(buf, 0, read);
          }
        }
      } catch (Exception e) {
        try {sRead.close();} catch (Exception e2) {}
        try {sWrite.close();} catch (Exception e2) {}
        if (debug) JFLog.log(e);
      }
    }
    public void close() {
      active = false;
    }
  }

  private static SOCKS socks;

  public static void serviceStart(String[] args) {
    socks = new SOCKS();
    socks.start();
  }

  public static void serviceStop() {
    socks.close();
  }
}
