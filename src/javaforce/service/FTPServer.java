package javaforce.service;

/** FTP Server.
 *
 * Known issue:
 *  windows command line ftp app requires passive mode.  port mode fails although FileZilla works fine.
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.jbus.*;

public class FTPServer {
  public final static String busPack = "net.sf.jfftp";

  public static boolean debug = false;

  private static final int bufsiz = (1500 - 20 - 20);  //ethernet=1500 ip=20 tcp=20

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfftp.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfftp.log";
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33013;
    } else {
      return 777;
    }
  }

  private Server server;
  private ArrayList<ServerWorker> servers = new ArrayList<ServerWorker>();
  private ArrayList<ClientWorker> clients = new ArrayList<ClientWorker>();
  private String ldap_domain;
  private String ldap_server;
  private String root;
  private ArrayList<EMail> users;
  private Object lock = new Object();
  private IP4Port bind = new IP4Port();
  private ArrayList<Subnet4> subnet_src_list;
  private ArrayList<Integer> ports = new ArrayList<>();
  private ArrayList<Integer> ssl_ports = new ArrayList<>();

  public FTPServer() {
  }

  private void addSession(ClientWorker sess) {
    synchronized(lock) {
      clients.add(sess);
    }
  }

  private void removeSession(ClientWorker sess) {
    synchronized(lock) {
      clients.remove(sess);
    }
  }

  private static String getKeyFile() {
    return JF.getConfigPath() + "/jfftp.key";
  }

  private class Server extends Thread {
    public boolean active;
    public void run() {
      active = true;
      JFLog.append(JF.getLogPath() + "/jfftp.log", true);
      JFLog.setRetention(30);
      JFLog.log("FTP : Starting service");
      try {
        loadConfig();
        busClient = new JBusClient(busPack, new JBusMethods());
        busClient.setPort(getBusPort());
        busClient.start();
        for(int p : ports) {
          ServerWorker worker = new ServerWorker(p, false);
          worker.start();
          servers.add(worker);
        }
        for(int p : ssl_ports) {
          ServerWorker worker = new ServerWorker(p, true);
          worker.start();
          servers.add(worker);
        }
        while(active) {
          synchronized(this) {
            wait();
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
    synchronized(lock) {
      ServerWorker[] sa = servers.toArray(new ServerWorker[0]);
      for(ServerWorker s : sa) {
        s.close();
      }
      servers.clear();

      ClientWorker[] ca = clients.toArray(new ClientWorker[0]);
      for(ClientWorker s : ca) {
        s.close();
      }
      clients.clear();
    }
    synchronized(server) {
      server.notify();
    }
    server = null;
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "port=21\n"  //default port (supports SSL)
    + "#secure=990\n"  //implicit SSL port
    + "#bind=192.168.100.2\n"
    + "#ldap_domain=example.org\n"
    + "#ldap_server=192.168.200.2\n"
    + "#account=user:pass\n"
    + "#root=/\n"  //root folder (linux)
    + "#root=c:/\n"  //root folder (windows)
    + "#root=/user/${user}/\n"  //root folder (linux custom)
    + "#root=c:/users/${user}/\n"  //root folder (windows custom)
    + "#digest=true\n"  //digest mode (see POP3/FTPRelay services)
    + "#src.ipnet=192.168.2.0/255.255.255.0\n"
    + "#src.ip=192.168.3.2\n"
    ;

  private void loadConfig() {
    JFLog.log("loadConfig");
    users = new ArrayList<EMail>();
    Section section = Section.None;
    bind.setIP("0.0.0.0");  //bind to all interfaces
    bind.port = 25;
    subnet_src_list = new ArrayList<Subnet4>();
    if (JF.isWindows()) {
      setRoot("c:/");
    } else {
      setRoot("/");
    }
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
                ports.add(Integer.valueOf(ln.substring(5)));
                break;
              case "secure":
                ssl_ports.add(Integer.valueOf(ln.substring(7)));
                break;
              case "bind":
                if (!bind.setIP(value)) {
                  JFLog.log("FTP:bind:Invalid IP:" + value);
                  break;
                }
                break;
              case "account": {
                EMail user = new EMail();
                int cln = value.indexOf(':');
                if (cln == -1) {
                  JFLog.log("Invalid user:" + value);
                  continue;
                }
                user.user = value.substring(0, cln);
                user.pass = value.substring(cln + 1);
                users.add(user);
                break;
              }
              case "ldap_domain":
                ldap_domain = value;
                break;
              case "ldap_server":
                ldap_server = value;
                break;
              case "root":
                setRoot(value.replaceAll("\\\\", "/"));
                break;
              case "debug":
                debug = value.equals("true");
                break;
              case "src.ipnet": {
                Subnet4 subnet = new Subnet4();
                idx = value.indexOf('/');
                if (idx == -1) {
                  JFLog.log("FTP:Invalid IP Subnet:" + value);
                  break;
                }
                String ip = value.substring(0, idx);
                String mask = value.substring(idx + 1);
                if (!subnet.setIP(ip)) {
                  JFLog.log("FTP:Invalid IP:" + ip);
                  break;
                }
                if (!subnet.setMask(mask)) {
                  JFLog.log("FTP:Invalid netmask:" + mask);
                  break;
                }
                JFLog.log("Source Allow IP Network=" + subnet.toString());
                subnet_src_list.add(subnet);
                break;
              }
              case "src.ip": {
                Subnet4 subnet = new Subnet4();
                if (!subnet.setIP(value)) {
                  JFLog.log("FTP:Invalid IP:" + value);
                  break;
                }
                subnet.setMask("255.255.255.255");
                JFLog.log("Source Allow IP Address=" + subnet.toString());
                subnet_src_list.add(subnet);
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

  private void setRoot(String path) {
    if (!path.endsWith("/")) {
      path += "/";
    }
    try {
      root = new File(path).getCanonicalPath().replaceAll("\\\\", "/");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public boolean login(String user, String pass) {
    for(EMail acct : users) {
      if (acct.user.equals(user)) {
        return acct.pass.equals(pass);
      }
    }
    if (ldap_server != null && ldap_domain != null) {
      LDAP ldap = new LDAP();
      return ldap.login(ldap_server, ldap_domain, user, pass);
    }
    return false;
  }

  public class ServerWorker extends Thread {
    private ServerSocket ss;
    private int port;
    private boolean secure;
    public boolean worker_active;

    public ServerWorker(int port, boolean secure) {
      this.port = port;
      this.secure = secure;
    }

    public void run() {
      try {
        if (secure) {
          JFLog.log("CreateServerSocketSSL");
          KeyMgmt keys = new KeyMgmt();
          if (new File(getKeyFile()).exists()) {
            FileInputStream fis = new FileInputStream(getKeyFile());
            keys.open(fis, "password");
            fis.close();
          } else {
            JFLog.log("Warning:Server SSL Keys not generated!");
            return;
          }
          ss = JF.createServerSocketSSL(keys);
        } else {
          ss = new ServerSocket();
        }
        synchronized(bind) {
          bind.port = port;
          ss.bind(bind.toInetSocketAddress());
        }
        JFLog.log("FTP Server started on port " + ss.getLocalPort());
        worker_active = true;
        while (worker_active) {
          Socket s = ss.accept();
          InetSocketAddress sa = (InetSocketAddress)s.getRemoteSocketAddress();
          String src_ip = sa.getAddress().getHostAddress();
          if (src_ip.equals("0:0:0:0:0:0:0:1")) {
            //IP6 localhost
            src_ip = "127.0.0.1";
          }
          if (!ip_src_allowed(src_ip)) {
            JFLog.log("FTP:Source IP blocked:" + src_ip);
            s.close();
            continue;
          }
          ClientWorker sess = new ClientWorker(s, secure);
          addSession(sess);
          sess.start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }

    public void close() {
      worker_active = false;
      try { ss.close(); } catch (Exception e) {}
      ss = null;
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

  private boolean userExists(EMail email) {
    for(EMail acct : users) {
      if (acct.user.equals(email.user)) {
        return true;
      }
    }
    //TODO : ldap users? (don't have password)
    return false;
  }

  private enum Mode {none, passive, port};

  public class ClientWorker extends Thread {
    private Socket c;
    private boolean secure;
    private InputStream cis = null;
    private OutputStream cos = null;

    //data socket
    private Socket d;
    private InputStream dis = null;
    private OutputStream dos = null;

    private String user;

    private String croot;
    private String curdir;  //current directory (relative to root)
    private String olddir;  //renameFrom

    private byte[] req = new byte[1500];
    private int reqSize = 0;

    private byte[] buffer = new byte[1500];
    private int bufferSize = 0;

    private Mode mode = Mode.none;

    //passive setup
    private ServerSocket passive_ss;

    //port setup
    private String port_ip;
    private int port_port;

    public ClientWorker(Socket s, boolean secure) {
      c = s;
      this.secure = secure;
    }
    public void close() {
      if (cos != null) {
        try {cos.flush();} catch (Exception e) {}
      }
      if (c != null) {
        try { c.close(); } catch (Exception e) {}
        c = null;
        cis = null;
        cos = null;
      }
      closeData();
    }
    private boolean openData() throws Exception {
      switch (mode) {
        case none: {
          cos.write("500 No data port established\r\n".getBytes());
          return false;
        }
        case passive:
          mode = Mode.none;
          d = passive_ss.accept();
          dis = d.getInputStream();
          dos = d.getOutputStream();
          passive_ss.close();
          passive_ss = null;
          cos.write("150 Using existing data connection\r\n".getBytes());
          return true;
        case port:
          //TODO : bind local socket to port 20 (random port for now)
          mode = Mode.none;
          d = new Socket(port_ip, port_port);
          dis = d.getInputStream();
          dos = d.getOutputStream();
          return true;
      }
      return false;
    }
    private void closeData() {
      if (dos != null) {
        try {dos.flush();} catch (Exception e) {}
      }
      if (d != null) {
        try {d.close();} catch (Exception e) {}
        d = null;
        dis = null;
        dos = null;
      }
      if (passive_ss != null) {
        try {passive_ss.close();} catch (Exception e) {}
        passive_ss = null;
      }
      port_ip = null;
      port_port = 0;
      mode = Mode.none;
    }
    public String readln() {
      if (bufferSize > 0) {
        System.arraycopy(buffer, 0, req, 0, bufferSize);
        reqSize = bufferSize;
        bufferSize = 0;
      } else {
        reqSize = 0;
      }
      try {
        while (c != null && c.isConnected()) {
          //check for EOL
          if (reqSize >= 2) {
            for(int idx=2;idx<=reqSize;idx++) {
              if (req[idx - 2] == '\r' && req[idx - 1] == '\n') {
                int left = reqSize - idx;
                if (left > 0) {
                  //save extra data for next time
                  System.arraycopy(req, idx, buffer, 0, left);
                  bufferSize = left;
                }
                return new String(req, 0, idx - 2).trim();
              }
            }
          }
          //expand buffer if needed
          if (reqSize == req.length) {
            //req buffer full - expand it
            if (req.length >= 8 * 1500) {
              throw new Exception("data too large");
            }
            int newSize = req.length << 1;
            byte[] new_req = new byte[newSize];
            buffer = new byte[newSize];
            System.arraycopy(req, 0, new_req, 0, req.length);
            req = new_req;
          }
          //read more data
          int read = cis.read(req, reqSize, req.length - reqSize);
          if (read < 0) break;
          reqSize += read;
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
      return null;
    }
    public void run() {
      try {
        JFLog.log("Session start");
        cis = c.getInputStream();
        cos = c.getOutputStream();
        cos.write(("220 jfFTP Server/" + JF.getVersion() + "\r\n").getBytes());
        curdir = "";
        while (c != null && c.isConnected()) {
          String ln = readln();
          if (ln == null) break;
          String[] args = ln.split(" ");
          String cmd = args[0].toUpperCase();
          if (cmd.equals("QUIT")) {
            cos.write("221 Goodbye\r\n".getBytes());
            break;
          }
          doCommand(cmd, args);
        }
      } catch (Exception e) {
        if (!(e instanceof SocketException)) JFLog.log(e);
      }
      close();
      removeSession(this);
    }

    private void doCommand(String cmd, String[] args) throws Exception {
      if (debug) {
        JFLog.log("Request=" + cmd);
      }
      switch (cmd) {
        case "AUTH":
          String type = args[1];
          switch (type.toUpperCase()) {
            case "LOGIN": {  //plain text
              cos.write("334 Send username\r\n".getBytes());
              String user_base64 = readln();
              if (user_base64 == null) {close(); return;}
              String user = new String(javaforce.Base64.decode(user_base64.getBytes()));
              cos.write("334 Send password\r\n".getBytes());
              String pass_base64 = readln();
              if (pass_base64 == null) {close(); return;}
              String pass = new String(javaforce.Base64.decode(pass_base64.getBytes()));
              if (login(user, pass)) {
                cos.write("230 Login successful\r\n".getBytes());
                setClientRoot();
              } else {
                JF.sleep(1000);
                cos.write("501 Login failed\r\n".getBytes());
                close();
                return;
              }
              break;
            }
            default: {
              cos.write("504 Unknown AUTH type\r\n".getBytes());
              break;
            }
          }
          return;
        case "USER":
          user = args[1];
          cos.write("331 Send password\r\n".getBytes());
          return;
        case "PASS":
          if (user == null) {
            cos.write("501 Login failed\r\n".getBytes());
            close();
            return;
          }
          if (login(user, args[1])) {
            cos.write("230 Login successful\r\n".getBytes());
            setClientRoot();
          } else {
            JF.sleep(1000);
            cos.write("501 Login failed\r\n".getBytes());
            close();
            return;
          }
          return;
        case "STARTTLS":
          if (secure) {
            cos.write("550 Already secure\r\n".getBytes());
            break;
          }
          cos.write("220 Ok\r\n".getBytes());
          //upgrade connection to SSL
          c = JF.connectSSL(c, KeyMgmt.getDefaultClient());
          cis = c.getInputStream();
          cos = c.getOutputStream();
          secure = true;
          return;
      }
      if (croot == null) {
        cos.write("401 Unauthorized\r\n".getBytes());
        throw new Exception("unauthorized");
      }
      switch (cmd) {
        case "RETR":
          get(JF.join(" ", args, 1));
          break;
        case "STOR":
          put(JF.join(" ", args, 1));
          break;
        case "CDUP":
          changeDir("..");
          break;
        case "CWD":
          changeDir(JF.join(" ", args, 1));
          break;
        case "SITE":
          //site chmod 777 file
          String cmd2 = args[1];
          switch (cmd2) {
            case "chmod":
              chmod(args[2], JF.join(" ", args, 3));
              break;
            default:
              break;
          }
          break;
        case "MKD":
          makeDir(JF.join(" ", args, 1));
          break;
        case "RNFR":
          renameFrom(JF.join(" ", args, 1));
          break;
        case "RNTO":
          renameTo(JF.join(" ", args, 1));
          break;
        case "DELE":
          delete(JF.join(" ", args, 1));
          break;
        case "RMD":
          removeDir(JF.join(" ", args, 1));
          break;
        case "LS":
        case "LIST":
          String path = ".";
          if (args.length > 1) {
            path = JF.join(" ", args, 1);
          }
          list(path);
          break;
        case "PWD":
          printCurDir();
          break;
        case "PASV":
          passive();
          break;
        case "PORT":
          port(args[1]);
          break;
        case "TYPE":
          cos.write("200 Ok\r\n".getBytes());
          break;
        case "SYST":
          cos.write("215 UNIX Type:L8\r\n".getBytes());
          break;
        default:
          cos.write("500 Unknown command\r\n".getBytes());
          break;
      }
    }
    private void get(String filename) throws Exception {
      String full = checkPath(filename);
      if (full == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      File file = new File(full);
      if (!file.exists()) {
        cos.write("500 File not found\r\n".getBytes());
        return;
      }
      if (file.isDirectory()) {
        cos.write("500 File is directory\r\n".getBytes());
        return;
      }
      if (!openData()) return;
      long length = file.length();
      long sent = 0;
      long left = length;
      byte[] buf = new byte[bufsiz];
      FileInputStream fis = new FileInputStream(file);
      while (sent < length) {
        int toread = bufsiz;
        if (toread > left) {
          toread = (int)left;
        }
        int read = fis.read(buf);
        if (read > 0) {
          dos.write(buf, 0, read);
          sent += read;
          left -= read;
        }
      }
      fis.close();
      closeData();
      cos.write("226 Transfer complete\r\n".getBytes());
    }
    private void put(String filename) throws Exception {
      String full = checkPath(filename);
      if (full == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      File file = new File(full);
      if (file.isDirectory()) {
        cos.write("500 File is directory\r\n".getBytes());
        return;
      }
      if (!openData()) return;
      byte[] buf = new byte[bufsiz];
      FileOutputStream fos = new FileOutputStream(file);
      while (d.isConnected() || dis.available() > 0) {
        int read = dis.read(buf);
        if (read == -1) break;
        if (read > 0) {
          fos.write(buf, 0, read);
        }
      }
      fos.close();
      closeData();
      cos.write("226 Transfer complete\r\n".getBytes());
    }
    private void changeDir(String dir) throws Exception {
      olddir = null;
      String newdir = checkPath(dir);
      if (newdir == null) {
        cos.write("500 Invalid path\r\n".getBytes());
      } else {
        curdir = stripPath(newdir);
        cos.write("250 Ok\r\n".getBytes());
      }
    }
    private void chmod(String mode, String filename) throws Exception {
      olddir = null;
      String full = checkPath(filename);
      if (full == null) {
        cos.write("500 Invalid path\r\n".getBytes());
      } else {
        File file = new File(full);
        cos.write("500 Not implemented yet!\r\n".getBytes());
      }
    }
    private void makeDir(String dir) throws Exception {
      olddir = null;
      String newdir = checkPath(dir);
      if (newdir == null) {
        cos.write("500 Invalid path\r\n".getBytes());
      } else {
        if (!new File(newdir).mkdir()) {
          cos.write("500 Unable to create directory\r\n".getBytes());
        } else {
          cos.write("257 Ok\r\n".getBytes());
        }
      }
    }
    private void renameFrom(String oldname) throws Exception {
      olddir = checkPath(oldname);
      if (olddir == null) {
        cos.write("500 Unable to create directory\r\n".getBytes());
      } else {
        cos.write("350 Ok\r\n".getBytes());
      }
    }
    private void renameTo(String newname) throws Exception {
      if (olddir == null) {
        cos.write("500 do renameFrom first\r\n".getBytes());
        return;
      }
      String newdir = checkPath(newname);
      if (newdir == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      if (!new File(olddir).renameTo(new File(newdir))) {
        cos.write("500 rename failed\r\n".getBytes());
      } else {
        cos.write("250 Ok\r\n".getBytes());
      }
    }
    private void delete(String filename) throws Exception {
      olddir = null;
      String full = checkPath(filename);
      if (full == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      File file = new File(full);
      if (!file.exists()) {
        cos.write("500 File not found\r\n".getBytes());
        return;
      }
      if (!file.delete()) {
        cos.write("500 Failed to delete!\r\n".getBytes());
      } else {
        cos.write("250 Ok\r\n".getBytes());
      }
    }
    private void removeDir(String dirname) throws Exception {
      olddir = null;
      String full = checkPath(dirname);
      if (full == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      File file = new File(full);
      if (!file.exists()) {
        cos.write("500 File not found\r\n".getBytes());
        return;
      }
      if (!file.delete()) {
        cos.write("500 Failed to delete!\r\n".getBytes());
      } else {
        cos.write("250 Ok\r\n".getBytes());
      }
    }
    private void list(String dir) throws Exception {
      String path = checkPath(dir);
      if (path == null) {
        cos.write("500 Invalid path\r\n".getBytes());
        return;
      }
      if (!openData()) return;
      File[] files = new File(path).listFiles();
      if (files == null) {
        files = new File[0];
        JFLog.log("Warning:no files for path:" + dir);
      }
      StringBuilder sb = new StringBuilder();
      for(File file : files) {
        String name = file.getName();
        if (file.isDirectory()) {
          sb.append("drwxrwxrwx");  //attributes
          sb.append("   1");  //???
          sb.append(" 0");  //user
          sb.append("      0");  //group
          sb.append(String.format("%16s", 0));  //size
          sb.append(" Jan 01  2001 ");  //date
          sb.append(name);
          sb.append("\r\n");
        } else {
          long length = file.length();
          sb.append("-rwxrwxrwx");  //attributes
          sb.append("   1");  //???
          sb.append(" 0");  //user
          sb.append("      0");  //group
          sb.append(String.format("%16s", length));  //size
          sb.append(" Jan 01  2001 ");  //date
          sb.append(name);
          sb.append("\r\n");
        }
      }
      dos.write(sb.toString().getBytes());
      cos.write("226 Transfer complete\r\n".getBytes());
      JF.sleep(250);
      closeData();
    }
    private void printCurDir() throws Exception {
      cos.write(("257 \"/" + curdir + "\" is current directory\r\n").getBytes());
    }
    private void passive() throws Exception {
      //server creates ServerSocket on random port
      passive_ss = new ServerSocket(0);
      InetAddress sa = c.getLocalAddress();
      byte[] ip8 = sa.getAddress();
      int[] ip32 = new int[4];
      for(int a=0;a<4;a++) {
        ip32[a] = ip8[a] & 0xff;
      }
      int port = passive_ss.getLocalPort();
      String msg = String.format("227 Entering Passive Mode (%d,%d,%d,%d,%d,%d)\r\n"
        , ip32[0], ip32[1], ip32[2], ip32[3]
        , (port & 0xff00) >> 8, port & 0xff
      );
      cos.write(msg.getBytes());
      mode = Mode.passive;
    }
    private void port(String ip_port) throws Exception {
      //server connects from port 20 to client
      String[] p = ip_port.split("[,]");  //ip,ip,ip,ip,port_hi,port_lo
      if (p.length != 6) {
        cos.write("500 Invalid port command\r\n".getBytes());
        return;
      }
      int[] v = new int[6];
      for(int a=0;a<6;a++) {
        v[a] = Integer.valueOf(p[a]);
        if (v[a] < 0 || v[a] > 255) {
          cos.write("500 Invalid port command\r\n".getBytes());
          return;
        }
      }
      port_ip = String.format("%d.%d.%d.%d", v[0], v[1], v[2], v[3]);
      port_port = (v[4] << 8) | v[5];
      cos.write("200 Ok\r\n".getBytes());
      mode = Mode.port;
    }
    private String checkPath(String name) throws Exception {
      String newdir;
      if (name.startsWith("/")) {
        //abs path
        File newfile = new File(croot + name);
        newdir = newfile.getCanonicalPath().replaceAll("\\\\", "/");
      } else {
        //rel path
        File newfile = new File(croot + curdir + "/" + name);
        newdir = newfile.getCanonicalPath().replaceAll("\\\\", "/");
      }
      if (newdir.length() < croot.length()) {
        JFLog.log("checkPath:length<root:" + newdir);
        return null;
      }
      String rootdir = newdir.substring(0, croot.length());
      if (!rootdir.equals(croot)) {
        JFLog.log("checkPath:not under root:" + newdir);
        return null;
      }
      return newdir;
    }

    private String stripPath(String path) {
      int len = croot.length();
      return path.substring(len);
    }

    private void setClientRoot() {
      croot = root.replaceAll("\\$\\{user\\}", user);
    }
  }

  private static FTPServer ftp;

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    ftp = new FTPServer();
    ftp.start();
  }

  public static void serviceStop() {
    JFLog.log("FTP : Stopping service");
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
    ftp.stop();
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      ftp.busClient.call(pack, "getConfig", ftp.busClient.quote(ftp.busClient.encodeString(ftp.config)));
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
      ftp.stop();
      ftp = new FTPServer();
      ftp.start();
    }

    public void genKeys(String pack) {
      if (KeyMgmt.keytool(new String[] {
        "-genkey", "-debug", "-alias", "jfftp", "-keypass", "password", "-storepass", "password",
        "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfftp.sourceforge.net, OU=user, O=server, C=CA",
        "-keyalg" , "RSA", "-keysize", "2048"
      })) {
        JFLog.log("Generated Keys");
        ftp.busClient.call(pack, "getKeys", ftp.busClient.quote("OK"));
      } else {
        ftp.busClient.call(pack, "getKeys", ftp.busClient.quote("ERROR"));
      }
    }
  }
}
