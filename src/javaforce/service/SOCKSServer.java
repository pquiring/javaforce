package javaforce.service;

/** Socks 4/4a/5 Server
 *
 * Default Port 1080
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
import javaforce.net.*;
import javaforce.jbus.*;

public class SOCKSServer {
  public final static String busPack = "net.sf.jfsocks";

  public static boolean debug = false;

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
  private Server server;
  private ArrayList<SocksWorker> socks_workers = new ArrayList<SocksWorker>();
  private ArrayList<ForwardLocalWorker> forward_local_workers = new ArrayList<ForwardLocalWorker>();
  private ArrayList<ForwardRemoteWorker> forward_remote_workers = new ArrayList<ForwardRemoteWorker>();
  private Object lock = new Object();
  private boolean socks4 = true, socks5 = false;
  private boolean socks_bind = false;
  private int socks_bind_timeout = (60 * 60 * 1000);  //default 60 mins
  private int forward_remote_timeout = (5 * 60 * 1000);  //default 5 mins (Note: TIME_WAIT is 4 mins)
  private IP4Port bind = new IP4Port();
  private IP4Port bind_cmd = new IP4Port();
  private boolean secure = false;
  private boolean secure_verify = false;
  private ArrayList<String> user_pass_list;
  private ArrayList<Subnet4> subnet_dest_list;
  private ArrayList<Subnet4> subnet_src_list;
  private ArrayList<ForwardLocal> forward_local_list;
  private ArrayList<ForwardRemote> forward_remote_list;
  private KeyMgmt keys = new KeyMgmt();

  public SOCKSServer() {
  }

  public SOCKSServer(int port, boolean secure) {
    bind.port = port;
    this.secure = secure;
  }

  private class ForwardLocal {
    public IP4Port from = new IP4Port();
    public IP4Port to = new IP4Port();
    public boolean set_from(String ip_port) {
      int idx = ip_port.indexOf(':');
      if (idx == -1) return false;
      String ip = ip_port.substring(0, idx);
      String port = ip_port.substring(idx+1);
      if (!from.setIP(ip)) return false;
      if (!from.setPort(port)) return false;
      return true;
    }
    public boolean set_to(String ip_port) {
      int idx = ip_port.indexOf(':');
      if (idx == -1) return false;
      String ip = ip_port.substring(0, idx);
      String port = ip_port.substring(idx+1);
      if (!to.setIP(ip)) return false;
      if (!to.setPort(port)) return false;
      return true;
    }
    public String toString() {
      return from.toString() + " -> " + to.toString();
    }
  }

  /** Logs an exception and returns false if it was a Socket Exception. */
  private boolean log(Exception e) {
    if (e instanceof SocketException) {
      JFLog.log("Connection lost (SocketException)");
      return false;
    }
    if (e instanceof SSLException) {
      JFLog.log("Connection lost (SSLException)");
      return false;
    }
    JFLog.log(e);
    return true;
  }

  private class ForwardRemote {
    public String user, pass;
    public IP4Port socks = new IP4Port();
    public IP4 from = new IP4();
    public int port;
    public IP4Port to = new IP4Port();
    public boolean secure;
    public boolean set_socks(String ip_port) {
      int idx = ip_port.indexOf(':');
      if (idx == -1) return false;
      String ip = ip_port.substring(0, idx);
      String port = ip_port.substring(idx+1);
      if (!socks.setIP(ip)) return false;
      if (!socks.setPort(port)) return false;
      return true;
    }
    public boolean set_from(String host) {
      return from.setIP(host);
    }
    public void set_port(int port) {
      this.port = port;
    }
    public boolean set_to(String ip_port) {
      int idx = ip_port.indexOf(':');
      if (idx == -1) return false;
      String ip = ip_port.substring(0, idx);
      String port = ip_port.substring(idx+1);
      if (!to.setIP(ip)) return false;
      if (!to.setPort(port)) return false;
      return true;
    }
    public String toString() {
      return socks.toString() + " bind port " + port + " -> " + to.toString();
    }
  }

  public void addUserPass(String user, String pass) {
    String user_pass = user + ":" + pass;
    user_pass_list.add(user_pass);
  }

  public void addSession(SocksWorker sess) {
    synchronized(lock) {
      socks_workers.add(sess);
    }
  }

  public void removeSession(SocksWorker sess) {
    synchronized(lock) {
      socks_workers.remove(sess);
    }
  }

  public static String getKeyFile() {
    return JF.getConfigPath() + "/jfsocks.key";
  }

  private class Server extends Thread {
    public boolean active;
    public void run() {
      JFLog.append(JF.getLogPath() + "/jfsocks.log", true);
      JFLog.setRetention(30);
      JFLog.log("SOCKS : Starting service");
      active = true;
      try {
        loadConfig();
        busClient = new JBusClient(busPack, new JBusMethods());
        busClient.setPort(getBusPort());
        busClient.start();
        if (secure) {
          JFLog.log("CreateServerSocketSSL");
          keys.setRootAlias("jfsocks");
          if (new File(getKeyFile()).exists()) {
            FileInputStream fis = new FileInputStream(getKeyFile());
            keys.open(fis, "password");
            fis.close();
          } else {
            JFLog.log("Warning:Server SSL Keys not generated!");
          }
          ss = JF.createServerSocketSSL(keys);
          if (secure_verify) {
            JF.clientKeys = keys;
            JF.clientKeysAlias = "jfsocks";
            ((SSLServerSocket)ss).setNeedClientAuth(true);
          }
        } else {
          ss = new ServerSocket();
        }
        ss.bind(bind.toInetSocketAddress());
        active = true;
        for(ForwardLocal fl : forward_local_list) {
          ForwardLocalWorker flw = new ForwardLocalWorker(fl);
          flw.start();
          forward_local_workers.add(flw);
        }
        for(ForwardRemote fr : forward_remote_list) {
          ForwardRemoteWorker frw = new ForwardRemoteWorker(fr);
          frw.start();
          forward_remote_workers.add(frw);
        }
        while (active) {
          try {
            Socket s = ss.accept();
            InetSocketAddress sa = (InetSocketAddress)s.getRemoteSocketAddress();
            String src_ip = sa.getAddress().getHostAddress();
            if (src_ip.equals("0:0:0:0:0:0:0:1")) {
              //IP6 localhost
              src_ip = "127.0.0.1";
            }
            if (!ip_src_allowed(src_ip)) {
              JFLog.log("SOCKS:Source IP blocked:" + src_ip);
              s.close();
              continue;
            }
            SocksWorker sess = new SocksWorker(s);
            addSession(sess);
            sess.start();
          } catch (Exception e) {
            log(e);
          }
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  public void start() {
    stop();
    server = new Server();
    server.start();
  }

  public void stop() {
    if (server == null) return;
    server.active = false;
    try { ss.close(); } catch (Exception e) {}
    synchronized(lock) {
      SocksWorker[] socks = socks_workers.toArray(new SocksWorker[0]);
      for(SocksWorker s : socks) {
        s.close();
      }
      socks_workers.clear();

      ForwardLocalWorker[] locals = forward_local_workers.toArray(new ForwardLocalWorker[0]);
      for(ForwardLocalWorker f : locals) {
        f.close();
      }
      forward_local_workers.clear();

      ForwardRemoteWorker[] remotes = forward_remote_workers.toArray(new ForwardRemoteWorker[0]);
      for(ForwardRemoteWorker f : remotes) {
        f.close();
      }
      forward_remote_workers.clear();
    }
    server = null;
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "port=1080\n"
    + "#bind=192.168.100.2\n"
    + "#bindcmd=192.168.110.2\n"
    + "secure=false\n"
    + "#secure.verify=false\n"
    + "socks4=true\n"
    + "socks5=false\n"
    + "socks.bind=false\n"
    + "#socks.bind.timeout=3600000\n"
    + "#auth=user:pass\n"
    + "#src.ipnet=192.168.2.0/255.255.255.0\n"
    + "#src.ip=192.168.3.2\n"
    + "#dest.ipnet=192.168.0.0/255.255.255.0\n"
    + "#dest.ip=192.168.1.6\n"
    + "#forwardlocal=192.168.100.2:80,192.168.200.2:80\n"
    + "#forwardremote=[user:pass@]192.168.110.2:1080,0.0.0.0,80,192.168.200.2:80[,true]\n"
    + "#forwardremote.timeout=300000\n";

  private void loadConfig() {
    JFLog.log("loadConfig");
    user_pass_list = new ArrayList<String>();
    subnet_src_list = new ArrayList<Subnet4>();
    subnet_dest_list = new ArrayList<Subnet4>();
    forward_local_list = new ArrayList<ForwardLocal>();
    forward_remote_list = new ArrayList<ForwardRemote>();
    Section section = Section.None;
    bind.setIP("0.0.0.0");  //bind to all interfaces
    bind.port = 1080;
    try {
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      StringBuilder cfg = new StringBuilder();
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim();
        int cmt = ln.indexOf('#');
        if (cmt != -1) ln = ln.substring(0, cmt).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        int idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx);
        String value = ln.substring(idx + 1);
        switch (section) {
          case None:
          case Global:
            switch (key) {
              case "port":
                bind.port = Integer.valueOf(ln.substring(5));
                break;
              case "bind":
                if (!bind.setIP(value)) {
                  JFLog.log("SOCKS:bind:Invalid IP:" + value);
                  break;
                }
                break;
              case "bindcmd":
                if (!bind_cmd.setIP(value)) {
                  JFLog.log("SOCKS:bindcmd:Invalid IP:" + value);
                  break;
                }
                break;
              case "secure":
                secure = value.equals("true");
                break;
              case "secure.verify":
                secure_verify = value.equals("true");
                break;
              case "socks4":
                socks4 = value.equals("true");
                break;
              case "socks5":
                socks5 = value.equals("true");
                break;
              case "socks.bind":
                socks_bind = value.equals("true");
                break;
              case "socks.bind.timeout":
                socks_bind_timeout = Integer.valueOf(value);
                if (socks_bind_timeout < 60000) {
                  socks_bind_timeout = 60000;  //1 min
                }
                if (socks_bind_timeout > 86400000) {
                  socks_bind_timeout = 86400000;  //1 day
                }
                break;
              case "forward.remote.wait":  //old alias
              case "forwardremote.timeout":
                forward_remote_timeout = Integer.valueOf(value);
                if (forward_remote_timeout < 60000) {
                  forward_remote_timeout = 60000;  //1 min
                }
                if (forward_remote_timeout > 86400000) {
                  forward_remote_timeout = 86400000;  //1 day
                }
                break;
              case "auth":
                user_pass_list.add(value);
                break;
              case "ipnet":  //old alias
              case "dest.ipnet": {
                Subnet4 subnet = new Subnet4();
                idx = value.indexOf('/');
                if (idx == -1) {
                  JFLog.log("SOCKS:Invalid IP Subnet:" + value);
                  break;
                }
                String ip = value.substring(0, idx);
                String mask = value.substring(idx + 1);
                if (!subnet.setIP(ip)) {
                  JFLog.log("SOCKS:Invalid IP:" + ip);
                  break;
                }
                if (!subnet.setMask(mask)) {
                  JFLog.log("SOCKS:Invalid netmask:" + mask);
                  break;
                }
                JFLog.log("Dest Allow IP Network=" + subnet.toString());
                subnet_dest_list.add(subnet);
                break;
              }
              case "ip":  //old alias
              case "dest.ip": {
                Subnet4 subnet = new Subnet4();
                if (!subnet.setIP(value)) {
                  JFLog.log("SOCKS:Invalid IP:" + value);
                  break;
                }
                subnet.setMask("255.255.255.255");
                JFLog.log("Dest Allow IP Address=" + subnet.toString());
                subnet_dest_list.add(subnet);
                break;
              }
              case "src.ipnet": {
                Subnet4 subnet = new Subnet4();
                idx = value.indexOf('/');
                if (idx == -1) {
                  JFLog.log("SOCKS:Invalid IP Subnet:" + value);
                  break;
                }
                String ip = value.substring(0, idx);
                String mask = value.substring(idx + 1);
                if (!subnet.setIP(ip)) {
                  JFLog.log("SOCKS:Invalid IP:" + ip);
                  break;
                }
                if (!subnet.setMask(mask)) {
                  JFLog.log("SOCKS:Invalid netmask:" + mask);
                  break;
                }
                JFLog.log("Source Allow IP Network=" + subnet.toString());
                subnet_src_list.add(subnet);
                break;
              }
              case "src.ip": {
                Subnet4 subnet = new Subnet4();
                if (!subnet.setIP(value)) {
                  JFLog.log("SOCKS:Invalid IP:" + value);
                  break;
                }
                subnet.setMask("255.255.255.255");
                JFLog.log("Source Allow IP Address=" + subnet.toString());
                subnet_src_list.add(subnet);
                break;
              }
              case "forward":  //old alias
              case "forwardlocal": {
                //src_ip:src_port,dest_ip:dest_port
                String[] p = value.split(",");
                if (p.length != 2) {
                  JFLog.log(key + ":Invalid option:" + value);
                  break;
                }
                ForwardLocal forward = new ForwardLocal();
                if (!forward.set_from(p[0])) {
                  JFLog.log(key + ":Invalid from:" + p[0]);
                  break;
                }
                if (!forward.set_to(p[1])) {
                  JFLog.log(key + ":Invalid to:" + p[1]);
                  break;
                }
                forward_local_list.add(forward);
                break;
              }
              case "forwardremote": {
                //[user:pass@]192.168.150.2:1080,0.0.0.0,80,192.168.200.2:80[,true]\n";
                String[] p = value.split(",");
                if (p.length < 4) {
                  JFLog.log("bindforward:Invalid option:" + value);
                  break;
                }
                ForwardRemote forward = new ForwardRemote();
                String bind = p[0];
                int atidx = bind.indexOf('@');
                if (atidx != -1) {
                  String user_pass = bind.substring(0, atidx);
                  bind = bind.substring(atidx + 1);
                  int colonidx = user_pass.indexOf(':');
                  if (colonidx != -1) {
                    forward.user = user_pass.substring(0, colonidx);
                    forward.pass = user_pass.substring(colonidx + 1);
                  }
                }
                if (!forward.set_socks(bind)) {
                  JFLog.log("bindforward:Invalid bind address:" + p[0]);
                  break;
                }
                forward.set_from(p[1]);
                forward.set_port(Integer.valueOf(p[2]));
                if (!forward.set_to(p[3])) {
                  JFLog.log("bindforward:Invalid to:" + p[3]);
                  break;
                }
                if (p.length > 4) {
                  forward.secure = p[4].equals("true");
                }
                forward_remote_list.add(forward);
                break;
              }
              case "debug": {
                debug = value.equals("true");
                break;
              }
            }
            break;
        }
      }
      br.close();
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
      JFLog.log("config not found, creating defaults.");
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

  private boolean ip_src_allowed(String ip4) {
    if (subnet_src_list.size() == 0) return true;
    IP4 target = new IP4();
    if (!target.setIP(ip4)) return false;
    for(Subnet4 net : subnet_src_list) {
      if (net.isWithin(target)) {
        return true;
      }
    }
    return false;
  }

  private boolean ip_dest_allowed(String ip4) {
    if (subnet_dest_list.size() == 0) return true;
    IP4 target = new IP4();
    if (!target.setIP(ip4)) return false;
    for(Subnet4 net : subnet_dest_list) {
      if (net.isWithin(target)) {
        return true;
      }
    }
    return false;
  }

  public class SocksWorker extends Thread {
    private Socket c;
    private Socket o;
    private ProxyData pd1, pd2;
    private boolean connected = false;
    private InputStream cis = null;
    private OutputStream cos = null;
    private byte[] req = new byte[1500];
    private int reqSize = 0;
    public SocksWorker(Socket s) {
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
        JFLog.log("Session start");
        cis = c.getInputStream();
        cos = c.getOutputStream();
        //read request
        while (c.isConnected()) {
          int read = cis.read(req, reqSize, 1500 - reqSize);
          if (read < 0) break;
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
        if (log(e) && !connected) {
          byte[] reply = null;
          switch (req[0]) {
            case 0x04:
              reply = new byte[8];
              break;
            case 0x05:
              reply = new byte[10];
              reply[3] = 0x01;  //IP4
              break;
          }
          reply[0] = req[0];
          reply[1] = 0x5b;  //failed
          try {cos.write(reply);} catch (Exception e2) {}
        }
      }
      close();
      removeSession(this);
    }

    private void socks4() throws Exception {
      if (!socks4) throw new Exception("SOCKS4:not enabled");
      JFLog.log("socks4 connection started");
      switch (req[1]) {
        case 0x01: socks4_connect(); return;
        case 0x02: socks4_bind(); return;
      }
      throw new Exception("SOCKS4:bad request");
    }

    private void socks4_connect() throws Exception {
      int port = BE.getuint16(req, 2);
      String user_id;  //ignored
      String ip3 = String.format("%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff);
      String dest;
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
        dest = new String(req, 8, domain_null - 8);
        dest = InetAddress.getByName(dest).getHostAddress();
      } else {
        dest = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff);
      }
      if (!ip_dest_allowed(dest)) throw new Exception("SOCKS:Target IP outside of allowed IP Subnets:" + dest);
      o = new Socket(dest, port);
      connected = true;
      byte[] reply = new byte[8];
      reply[0] = 0x00;
      reply[1] = 0x5a;  //success
      cos.write(reply);
      //now just proxy data back and forth
      pd1 = new ProxyData(c,o,"s4-1");
      pd1.start();
      pd2 = new ProxyData(o,c,"s4-2");
      pd2.start();
      pd1.join();
      pd2.join();
      JFLog.log("SOCKS4:connect() Session end");
    }

    private void socks4_bind() throws Exception {
      if (!socks_bind) throw new Exception("SOCKS.bind disabled!");
      int port = BE.getuint16(req, 2);
      String src = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff);
      byte[] reply = new byte[8];
      reply[0] = 0x00;
      reply[1] = 0x5a;  //success
      cos.write(reply);
      ServerSocket ss = null;
      try {
        if (bind_cmd.isEmpty()) {
          ss = new ServerSocket(port);
        } else {
          synchronized (bind_cmd) {
            ss = new ServerSocket();
            bind_cmd.port = port;
            ss.bind(bind_cmd.toInetSocketAddress());
          }
        }
        ss.setSoTimeout(socks_bind_timeout);
        o = ss.accept();
        String src_addr = o.getInetAddress().getHostAddress();
        if (src_addr.equals("0:0:0:0:0:0:0:1")) {
          //IP6 localhost
          src_addr = "127.0.0.1";
        }
        int src_port = o.getPort();
        if (!src.equals("0.0.0.0")) {
          if (!src.equals(src_addr)) {
            throw new Exception("SOCKS4:bind:unexpected host connected:" + src_addr);
          }
        }
        connected = true;
        reply[0] = 0x00;
        reply[1] = 0x5a;  //success
        IP4 src_ip = new IP4();
        src_ip.setIP(src_addr);
        //return src ip/port
        reply[2] = (byte)(src_port & 0xff00 >> 8);
        reply[3] = (byte)(src_port & 0xff);
        for(int a=0;a<4;a++) {
          reply[4 + a] = (byte)src_ip.ip[0 + a];
        }
        cos.write(reply);
        //now just proxy data back and forth
        pd1 = new ProxyData(c,o,"s4-1");
        pd1.start();
        pd2 = new ProxyData(o,c,"s4-2");
        pd2.start();
        pd1.join();
        pd2.join();
      } catch (Exception e) {
        log(e);
      }
      if (ss != null) {
        try { ss.close(); } catch (Exception e2) {}
      }
      JFLog.log("SOCKS4:bind() Session end");
    }

    private void socks5() throws Exception {
      JFLog.log("socks5 connection started");
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
        if (read < 0) break;
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
      String pass = new String(req, 3 + user_len, pass_len);
      String user_pass = user + ":" + pass;
      if (!user_pass_list.contains(user_pass)) {
        throw new Exception("SOCKS5:user/pass not authorized");
      }
      reply = new byte[2];
      reply[0] = 0x01;  //version 1
      reply[1] = 0x00;  //authorized
      cos.write(reply);
      //read connect request
      reqSize = 0;
      int toRead = 10;
      while (c.isConnected()) {
        int read = cis.read(req, reqSize, toRead - reqSize);
        if (read < 0) break;
        reqSize += read;
        if (reqSize < 10) continue;
        int dest_type = req[3] & 0xff;
        if (dest_type == 0x01) {
          //ip4
          if (reqSize == toRead) break;
        } else if (dest_type == 0x03) {
          //domain name
          int domain_len = req[4] & 0xff;
          toRead = 5 + domain_len + 2;
          if (reqSize == toRead) break;
        } else if (dest_type == 0x04) {
          throw new Exception("SOCKS5:IP6 not supported");
        } else {
          throw new Exception("SOCKS5:dest_type not supported:" + dest_type);
        }
      }
      if (req[0] != 0x05) throw new Exception("SOCKS5:bad connection request:version != 0x05");
      switch (req[1]) {
        case 0x01: socks5_connect(); return;
        case 0x02: socks5_bind(); return;
      }
      throw new Exception("SOCKS5:cmd not supported:" + req[1]);
    }

    private void socks5_connect() throws Exception {
      byte[] reply;
      //req[2] = reserved
      String dest = null;
      int port = BE.getuint16(req, reqSize - 2);
      switch (req[3]) {
        case 0x01:
          dest = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff);
          break;
        case 0x03:
          dest = new String(req, 5, req[4] & 0xff);
          dest = InetAddress.getByName(dest).getHostAddress();
          break;
        default:
          throw new Exception("SOCKS5:bad connection request:addr type not supported:" + req[3]);
      }
      if (!ip_dest_allowed(dest)) throw new Exception("SOCKS:Target IP outside of allowed IP Subnets:" + dest);
      reply = new byte[reqSize];
      System.arraycopy(req, 0, reply, 0, reqSize);
      reply[1] = 0x00;  //success
      cos.write(reply);
      JFLog.log("SOCKS5:Connect:" + dest + ":" + port);
      o = new Socket(dest, port);
      connected = true;
      //now just proxy data back and forth
      pd1 = new ProxyData(c,o,"s5-1");
      pd1.start();
      pd2 = new ProxyData(o,c,"s5-2");
      pd2.start();
      pd1.join();
      pd2.join();
      JFLog.log("SOCKS5:connect() Session end");
    }
    private void socks5_bind() throws Exception {
      if (!socks_bind) throw new Exception("SOCKS.bind disabled!");
      byte[] reply;
      //req[2] = reserved
      String src = null;
      int port = BE.getuint16(req, reqSize - 2);
      switch (req[3]) {
        case 0x01:
          src = String.format("%d.%d.%d.%d", req[4] & 0xff, req[5] & 0xff, req[6] & 0xff, req[7] & 0xff);
          break;
        case 0x03:
          src = new String(req, 5, req[4] & 0xff);
          src = InetAddress.getByName(src).getHostAddress();
          break;
        default:
          throw new Exception("SOCKS5:bad connection request:addr type not supported:" + req[3]);
      }
      reply = new byte[reqSize];
      System.arraycopy(req, 0, reply, 0, reqSize);
      reply[1] = 0x00;  //success
      cos.write(reply);
      ServerSocket ss = null;
      try {
        if (bind_cmd.isEmpty()) {
          ss = new ServerSocket(port);
        } else {
          synchronized (bind_cmd) {
            ss = new ServerSocket();
            bind_cmd.port = port;
            ss.bind(bind_cmd.toInetSocketAddress());
          }
        }
        ss.setSoTimeout(socks_bind_timeout);
        o = ss.accept();
        String src_addr = o.getInetAddress().getHostAddress();
        if (src_addr.equals("0:0:0:0:0:0:0:1")) {
          //IP6 localhost
          src_addr = "127.0.0.1";
        }
        int src_port = o.getPort();
        if (!src.equals("0.0.0.0")) {
          if (!src.equals(src_addr)) {
            throw new Exception("SOCKS5:bind:unexpected host connected:" + src_addr);
          }
        }
        reply = new byte[10];
        reply[0] = 0x05;  //version
        reply[1] = 0x00;  //success
        reply[2] = 0x00;  //reserved
        reply[3] = 0x01;  //IP4
        IP4 src_ip = new IP4();
        src_ip.setIP(src_addr);
        //return src ip/port
        for(int a=0;a<4;a++) {
          reply[4 + a] = (byte)src_ip.ip[0 + a];
        }
        reply[8] = (byte)(src_port & 0xff00 >> 8);
        reply[9] = (byte)(src_port & 0xff);
        cos.write(reply);
        connected = true;
        //now just proxy data back and forth
        pd1 = new ProxyData(c,o,"s5-1");
        pd1.start();
        pd2 = new ProxyData(o,c,"s5-2");
        pd2.start();
        pd1.join();
        pd2.join();
      } catch (Exception e) {
        log(e);
      }
      if (ss != null) {
        try { ss.close(); } catch (Exception e2) {}
      }
      JFLog.log("SOCKS5:bind() Session end");
    }
  }

  public class ForwardLocalWorker extends Thread {
    private ForwardLocal forward;
    private ServerSocket ss;
    public ForwardLocalWorker(ForwardLocal forward) {
      this.forward = forward;
    }
    public void run() {
      try {
        ss = new ServerSocket();
        ss.bind(forward.from.toInetSocketAddress());
        while (server.active) {
          Socket from = ss.accept();
          try {
            Socket to = new Socket(forward.to.toInetAddress(), forward.to.port);
            ProxyData pd1 = new ProxyData(from, to, "f-1");
            pd1.start();
            ProxyData pd2 = new ProxyData(to, from, "f-2");
            pd2.start();
            //TODO : join ProxyData threads
          } catch (Exception e) {
            log(e);
          }
        }
      } catch (Exception e) {
        log(e);
        JF.sleep(1000);
      }
    }
    public void close() {
      try { ss.close(); } catch (Exception e) {}
    }
  }

  public class ForwardRemoteWorker extends Thread {
    private ForwardRemote forward;
    public ForwardRemoteWorker(ForwardRemote forward) {
      this.forward = forward;
    }
    public void run() {
      try {
        while (server.active) {
          boolean wait = true;
          try {
            Socket from = null;
            if (forward.secure) {
              from = JF.connectSSL(forward.socks.toIP4String(), forward.socks.port, keys);
            } else {
              from = new Socket(forward.socks.toInetAddress(), forward.socks.port);
            }
            if (forward.user != null && forward.pass != null) {
              if (!javaforce.SOCKS.bind(from, forward.from.toString(), forward.port, forward.user, forward.pass)) {
                throw new Exception("SOCKS5:bind:failed");
              }
            } else {
              if (!javaforce.SOCKS.bind(from, forward.from.toString(), forward.port)) {
                throw new Exception("SOCKS4:bind:failed");
              }
            }
            Socket to = new Socket(forward.to.toInetAddress(), forward.to.port);
            ProxyData pd1 = new ProxyData(from, to, "f-1");
            pd1.start();
            ProxyData pd2 = new ProxyData(to, from, "f-2");
            pd2.start();
            //TODO : join ProxyData threads
            wait = false;
          } catch (Exception e) {
            log(e);
            JF.sleep(1000);
          }
          if (wait) {
            //abnormal exception - wait to avoid hammering server
            int wait_time = 0;
            while (server.active && wait_time < forward_remote_timeout) {
              JF.sleep(1000);
              wait_time += 1000;
            }
          } else {
            int wait_time = 0;
            while (server.active && wait_time < 5000) {
              JF.sleep(1000);
              wait_time += 1000;
            }
          }
        }
      } catch (Exception e) {
        log(e);
      }
    }
    public void close() {
      try { ss.close(); } catch (Exception e) {}
    }
  }

  public class ProxyData extends Thread {
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
          if (read < 0) break;
          if (read > 0) {
            os.write(buf, 0, read);
          }
        }
      } catch (Exception e) {
        log(e);
      }
      try {sRead.close();} catch (Exception e) {}
      try {sWrite.close();} catch (Exception e) {}
    }
    public void close() {
      active = false;
    }
  }

  private static SOCKSServer socks;

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    socks = new SOCKSServer();
    socks.start();
  }

  public static void serviceStop() {
    JFLog.log("SOCKS : Stopping service");
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
    socks.stop();
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static boolean createKeys() {
    return KeyMgmt.keytool(new String[] {
      "-genkey", "-debug", "-alias", "jfsocks", "-keypass", "password", "-storepass", "password",
      "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfsocks.sourceforge.net, OU=user, O=server, C=CA",
      "-keyalg" , "RSA", "-keysize", "2048"
    });
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      socks.busClient.call(pack, "getConfig", socks.busClient.quote(socks.busClient.encodeString(socks.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      JFLog.log("setConfig");
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      JFLog.log("restart");
      socks.stop();
      socks = new SOCKSServer();
      socks.start();
    }

    public void genKeys(String pack) {
      if (createKeys()) {
        JFLog.log("Generated Keys");
        socks.busClient.call(pack, "genKeysStatus", socks.busClient.quote("OK"));
      } else {
        socks.busClient.call(pack, "genKeysStatus", socks.busClient.quote("ERROR"));
      }
    }

    public void getKeys(String pack) {
      byte[] data;
      try {
        FileInputStream fis = new FileInputStream(getKeyFile());
        data = fis.readAllBytes();
        fis.close();
        socks.busClient.call(pack, "giveKeys", socks.busClient.encodeByteArray(data));
      } catch (Exception e) {
        JFLog.log(e);
        socks.busClient.call(pack, "giveKeys", socks.busClient.quote(""));
      }
    }

    public void setKeys(String pack, String data) {
      byte[] keys = JBusClient.decodeByteArray(data);
      if (JF.writeFile(getKeyFile(), keys)) {
        socks.busClient.call(pack, "setKeysStatus", socks.busClient.quote("OK"));
      } else {
        socks.busClient.call(pack, "setKeysStatus", socks.busClient.quote("ERROR"));
      }
    }
  }
}
