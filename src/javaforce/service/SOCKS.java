package javaforce.service;

/** Socks 4a Server
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

import javax.net.ssl.*;

import javaforce.*;

public class SOCKS extends Thread {
  public final static String busPack = "net.sf.jfsocks";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfsocks.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfsocks.log";
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33008;
    } else {
      return 777;
    }
  }

  private ServerSocket ss;
  private volatile boolean active;
  private static ArrayList<Session> sessions = new ArrayList<Session>();
  private static Object lock = new Object();
  private static boolean debug = false;
  private static boolean socks4, socks5;
  private int port;
  private boolean secure;
  private static ArrayList<String> user_pass_list = new ArrayList<String>();

  public SOCKS() {
    port = 1080;
  }

  public SOCKS(int port, boolean secure) {
    this.port = port;
    this.secure = secure;
  }

  public void addUserPass(String user, String pass) {
    String user_pass = user + ":" + pass;
    user_pass_list.add(user_pass);
  }

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
      loadConfig();
      if (secure) {
        ss = SSLServerSocketFactory.getDefault().createServerSocket(port);
      } else {
        ss = new ServerSocket(port);
      }
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

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "port=1080\n"
    + "secure=false\n"
    + "socks4=true\n"
    + "socks5=false\n"
    + "#auth=user:pass\n";

  private String config;

  private void loadConfig() {
    JFLog.log("loadConfig");
    Section section = Section.None;
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim().toLowerCase();
        int idx = ln.indexOf('#');
        if (idx != -1) ln = ln.substring(0, idx).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        switch (section) {
          case None:
          case Global:
            if (ln.startsWith("port=")) {
              port = Integer.valueOf(ln.substring(5));
            }
            if (ln.startsWith("secure=")) {
              secure = ln.substring(7).equals("true");
            }
            if (ln.startsWith("socks4=")) {
              socks4 = ln.substring(7).equals("true");
            }
            if (ln.startsWith("socks5=")) {
              socks5 = ln.substring(7).equals("true");
            }
            if (ln.startsWith("auth=")) {
              String user_pass = ln.substring(5);
              user_pass_list.add(user_pass);
            }
            break;
        }
      }
      br.close();
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      JFLog.log("config not found, creating defaults.");
      port = 1080;
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(defaultConfig.getBytes());
        fos.close();
        config = defaultConfig;
      } catch (Exception e2) {
        JFLog.log(e2);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public static class Session extends Thread {
    private Socket c;
    private Socket o;
    private ProxyData pd1, pd2;
    private boolean connected = false;
    private InputStream cis = null;
    private OutputStream cos = null;
    private byte[] req = new byte[1500];
    private int reqSize = 0;
    public Session(Socket s) {
      c = s;
    }
    public void close() {
      if (c != null) {
        try { c.close(); } catch (Exception e) {}
      }
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
      //request = 0x04 0x01 port16 ip32 user_id_null [domain_name_null]
      //reply   = 0x00 0x5a reserved[6]   //0x5b = failed
      try {
        if (debug) JFLog.log("Session start");
        cis = c.getInputStream();
        cos = c.getOutputStream();
        //read request
        while (c.isConnected()) {
          int read = cis.read(req, reqSize, 1500 - reqSize);
          if (read < 0) throw new Exception("bad read");
          reqSize += read;
          if (reqSize == 0) continue;
          if (req[0] == 0x04) {
            if (reqSize < 8) continue;
            String ip3 = String.format("%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff);
            if (ip3.equals("0.0.0")) {
              //domain request
              //look for user_id_null and domain_null
              int null_count = 0;
              for(int a=8;a<reqSize;a++) {
                if (req[a] == 0) null_count++;
              }
              if (null_count == 2) break;
              if (null_count > 2) throw new Exception("SOCKS4:bad request:too many nulls:expect=2");
            } else {
              //ip4 request
              //look for user_id_null
              int null_count = 0;
              for(int a=8;a<reqSize;a++) {
                if (req[a] == 0) null_count++;
              }
              if (null_count == 1) break;
              if (null_count > 1) throw new Exception("SOCKS4:bad request:too many nulls:expect=1");
            }
          } else if (req[0] == 0x05) {
            if (reqSize < 3) continue;
            int nauth = req[1] & 0xff;
            if (reqSize == nauth + 2) break;
          } else {
            throw new Exception("bad request:not SOCKS4/5 request");
          }
        }
        switch (req[0]) {
          case 0x04: socks4(); break;
          case 0x05: socks5(); break;
          default: throw new Exception("bad request:not SOCKS4/5 request");
        }
      } catch (Exception e) {
        if (debug) JFLog.log(e);
        if (!connected) {
          byte[] reply = new byte[8];
          reply[0] = 0x00;
          reply[1] = 0x5b;  //failed
          try {cos.write(reply);} catch (Exception e2) {}
        }
      }
      close();
      removeSession(this);
    }

    private void socks4() throws Exception {
      if (req[1] != 0x01) throw new Exception("SOCKS4:bad request:not open socket request");
      if (!socks4) throw new Exception("SOCKS4:not enabled");
      int port = BE.getuint16(req, 2);
      String user_id;  //ignored
      String ip3 = String.format("%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff);
      if (ip3.equals("0.0.0")) {
        int user_null = -1;
        int domain_null = -1;
        for(int a=8;a<reqSize;a++) {
          if (req[a] == 0) {
            if (user_null == -1) {
              user_null = a;
            } else {
              domain_null = a;
            }
          }
        }
        user_id = new String(req, 8, user_null - 8);
        String domain = new String(req, 8, domain_null - 8);
        o = new Socket(domain, port);
      } else {
        String ip4 = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff);
        o = new Socket(ip4, port);
      }
      connected = true;
      byte[] reply = new byte[8];
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
    }

    private void socks5() throws Exception {
      if (!socks5) throw new Exception("SOCKS5:not enabled");
      //req = 0x05 nauth auth_types[]
      int nauth = req[1] & 0xff;
      boolean auth_type_2 = false;
      for(int a=0;a<nauth;a++) {
        if (req[a+2] == 0x02) {
          auth_type_2 = true;
          break;
        }
      }
      if (!auth_type_2) throw new Exception("SOCKS5:auth not supported");
      byte[] reply = new byte[2];
      reply[0] = 0x05;
      reply[1] = 0x02;
      cos.write(reply);
      //read username/password
      reqSize = 0;
      while (c.isConnected()) {
        int read = cis.read(req, reqSize, 1500 - reqSize);
        if (read < 0) throw new Exception("bad read");
        reqSize += read;
        if (reqSize < 5) continue;
        if (req[0] != 0x01) throw new Exception("SOCKS5:invalid auth request");
        int user_len = req[1] & 0xff;
        if (reqSize < 3 + user_len) continue;
        int pass_len = req[2 + user_len] & 0xff;
        if (reqSize < 3 + user_len + pass_len) continue;
        break;
      }
      int user_len = req[1] & 0xff;
      int pass_len = req[2 + user_len] & 0xff;
      String user = new String(req, 2, user_len);
      String pass = new String(req, 2 + user_len, pass_len);
      String user_pass = user + ":" + pass;
      if (user_pass_list.contains(user_pass)) {
        throw new Exception("SOCKS5:user/pass not authorized");
      }
      reply = new byte[2];
      reply[0] = 0x01;  //version 1
      reply[1] = 0x00;  //authorized
      cos.write(reply);
      //read connect request
      reqSize = 0;
      while (c.isConnected()) {
        int read = cis.read(req, reqSize, 9 - reqSize);
        if (read < 0) throw new Exception("bad read");
        reqSize += read;
        if (reqSize < 9) continue;
        int dest_type = req[3];
        if (dest_type == 0x01) {
          //ip4
          if (reqSize == 9) break;
        } else if (dest_type == 0x03) {
          //domain name
          int domain_len = req[4];
          if (reqSize == 6 + domain_len) break;
        } else if (dest_type == 0x04) {
          throw new Exception("SOCKS5:IP6 not supported");
        } else {
          throw new Exception("SOCKS5:dest_type not supported:" + dest_type);
        }
        break;
      }
      if (req[0] != 0x05) throw new Exception("SOCKS5:bad connection request:version != 0x05");
      if (req[1] != 0x01) throw new Exception("SOCKS5:bad connection request:cmd not supported:" + req[1]);
      //req[2] = reserved
      String dest = null;
      int port = BE.getuint16(req, reqSize - 2);
      switch (req[3]) {
        case 0x01: dest = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff); break;
        case 0x03: dest = new String(req, 4, req[4] & 0xff); break;
        default: throw new Exception("SOCKS5:bad connection request:addr type not supported:" + req[3]);
      }
      reply = new byte[reqSize];
      System.arraycopy(req, 0, reply, 0, reqSize);
      reply[1] = 0x00;  //success
      cos.write(reply);
      o = new Socket(dest, port);
      connected = true;
      //now just proxy data back and forth
      pd1 = new ProxyData(c,o,"1");
      pd1.start();
      pd2 = new ProxyData(o,c,"2");
      pd2.start();
      pd1.join();
      pd2.join();
      if (debug) JFLog.log("Session end");
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
