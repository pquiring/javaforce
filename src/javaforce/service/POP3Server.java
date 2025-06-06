package javaforce.service;

/** POP3 Server.
 *
 * Retrieves messages from SMTP service.
 *
 * Supported ports : 110, 995(ssl)
 *
 * TODO : XTND, XLST (Thunderbird requests)
 * TODO : TOP, RSET
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.jbus.*;

public class POP3Server {
  public final static String busPack = "net.sf.jfpop3";

  public static boolean debug = false;

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfpop3.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfpop3.log";
  }

  public static String getMailboxFolder(String user) {
    StringBuilder path = new StringBuilder();
    if (JF.isWindows()) {
      path.append(System.getenv("ProgramData").replaceAll("\\\\", "/"));
      path.append("/jfsmtp/mail");
    } else {
      path.append("/var/jfsmtp/mail");
    }
    if (user != null) {
      path.append("/");
      path.append(user);
    }
    String mail = path.toString();
    new File(mail).mkdirs();
    return mail;
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33010;
    } else {
      return 777;
    }
  }

  private Server server;
  private ArrayList<ServerWorker> servers = new ArrayList<ServerWorker>();
  private ArrayList<ClientWorker> clients = new ArrayList<ClientWorker>();
  private String domain;
  private String ldap_domain;
  private String ldap_server;
  private ArrayList<EMail> users;
  private Object lock = new Object();
  private IP4Port bind = new IP4Port();
  private ArrayList<Subnet4> subnet_src_list;
  private ArrayList<Integer> ports = new ArrayList<>();
  private ArrayList<Integer> ssl_ports = new ArrayList<>();

  private static final int FLAG_ADMIN = 1;

  public POP3Server() {
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
    return JF.getConfigPath() + "/jfpop3.key";
  }

  private class Server extends Thread {
    private boolean active;
    public void run() {
      active = true;
      JFLog.append(JF.getLogPath() + "/jfpop3.log", true);
      JFLog.setRetention(30);
      JFLog.log("POP3 : Starting service");
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
        while (active) {
          JF.sleep(1000);
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
  }

  enum Section {None, Global};

  private final static String defaultConfig
    = "[global]\n"
    + "port=110\n"  //default port (supports SSL)
    + "#secure=995\n"  //implicit SSL port
    + "#bind=192.168.100.2\n"
    + "#domain=example.com\n"
    + "#ldap_domain=example.com\n"
    + "#ldap_server=192.168.200.2\n"
    + "#admin=user:pass\n"
    + "#account=user:pass\n"
    + "#src.ipnet=192.168.2.0/255.255.255.0\n"
    + "#src.ip=192.168.3.2\n"
    ;

  private void loadConfig() {
    JFLog.log("loadConfig");
    users = new ArrayList<EMail>();
    Section section = Section.None;
    bind.setIP("0.0.0.0");  //bind to all interfaces
    bind.port = 110;
    subnet_src_list = new ArrayList<Subnet4>();
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
                  JFLog.log("SMTP:bind:Invalid IP:" + value);
                  break;
                }
                break;
              case "admin":
              case "account":
                EMail user = new EMail();
                int cln = value.indexOf(':');
                if (cln == -1) {
                  JFLog.log("Invalid user:" + value);
                  continue;
                }
                user.user = value.substring(0, cln);
                user.pass = value.substring(cln + 1);
                user.flags = key.equals("admin") ? FLAG_ADMIN : 0;
                users.add(user);
                break;
              case "domain":
                domain = value;
                break;
              case "ldap_domain":
                ldap_domain = value;
                break;
              case "ldap_server":
                ldap_server = value;
                break;
              case "debug":
                debug = value.equals("true");
                break;
              case "src.ipnet": {
                Subnet4 subnet = new Subnet4();
                idx = value.indexOf('/');
                if (idx == -1) {
                  JFLog.log("POP3:Invalid IP Subnet:" + value);
                  break;
                }
                String ip = value.substring(0, idx);
                String mask = value.substring(idx + 1);
                if (!subnet.setIP(ip)) {
                  JFLog.log("POP3:Invalid IP:" + ip);
                  break;
                }
                if (!subnet.setMask(mask)) {
                  JFLog.log("POP3:Invalid netmask:" + mask);
                  break;
                }
                JFLog.log("Source Allow IP Network=" + subnet.toString());
                subnet_src_list.add(subnet);
                break;
              }
              case "src.ip": {
                Subnet4 subnet = new Subnet4();
                if (!subnet.setIP(value)) {
                  JFLog.log("POP3:Invalid IP:" + value);
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

  public class ServerWorker extends Thread {
    private ServerSocket ss;
    private int port;
    private boolean secure;
    private boolean worker_active;

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
          }
          ss = JF.createServerSocketSSL(keys);
        } else {
          ss = new ServerSocket();
        }
        synchronized(bind) {
          bind.port = port;
          ss.bind(bind.toInetSocketAddress());
        }
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
            JFLog.log("POP3:Source IP blocked:" + src_ip);
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

  public class ClientWorker extends Thread {
    private Socket c;
    private boolean secure;
    private InputStream cis = null;
    private OutputStream cos = null;

    private byte[] req = new byte[1500];
    private int reqSize = 0;

    private byte[] buffer = new byte[1500];
    private int bufferSize = 0;

    private String mailbox;
    private File[] files;

    private String user;
    private boolean admin;

    public ClientWorker(Socket s, boolean secure) {
      c = s;
      this.secure = secure;
    }
    public void close() {
      if (c != null) {
        try { c.close(); } catch (Exception e) {}
      }
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
        cos.write(("+OK jfPOP3 Server/" + JF.getVersion() + "\r\n").getBytes());
        cos.flush();
        while (c.isConnected()) {
          String cmd = readln();
          if (cmd == null) break;
          if (cmd.equalsIgnoreCase("QUIT")) {
            cos.write("+OK Goodbye\r\n".getBytes());
            break;
          }
          doCommand(cmd);
        }
      } catch (Exception e) {
        if (!(e instanceof SocketException)) JFLog.log(e);
      }
      close();
      removeSession(this);
    }

    public boolean login(String user, String pass, boolean _md5) {
      for(EMail acct : users) {
        if (acct.user.equals(user)) {
          admin = (acct.flags & FLAG_ADMIN) != 0;
          if (_md5) {
            MD5 md5 = new MD5();
            md5.add(acct.pass);
            return md5.toString().equals(pass);
          } else {
            return acct.pass.equals(pass);
          }
        }
      }
      if (ldap_server != null && ldap_domain != null) {
        LDAP ldap = new LDAP();
        return ldap.login(ldap_server, ldap_domain, user, pass);
      }
      return false;
    }

    private static final int bufsiz = 1500 - 20 - 20;

    private void doCommand(String cmd) throws Exception {
      if (debug) {
        JFLog.log("Request=" + cmd);
      }
      String[] p = cmd.split(" ", 2);
      switch (p[0].toUpperCase()) {
        case "APOP": {
          String[] c_u_p = cmd.split(" ", 3);
          user = c_u_p[1];
          String pass = c_u_p[2];  //MD5???
          if (login(user, pass, true)) {
            cos.write("+OK Login successful\r\n".getBytes());
          } else {
            JF.sleep(1000);
            cos.write("-ERR Login failed\r\n".getBytes());
            close();
            return;
          }
          setupMailbox();
          break;
        }
        case "USER": {
          user = p[1];
          cos.write("+OK User Accepted\r\n".getBytes());
          break;
        }
        case "PASS": {
          if (user == null) {
            cos.write("-ERR Login failed\r\n".getBytes());
            close();
            return;
          }
          String pass = p[1];
          if (login(user, pass, false)) {
            cos.write("+OK Login successful\r\n".getBytes());
          } else {
            JF.sleep(1000);
            cos.write("-ERR Login failed\r\n".getBytes());
            close();
            return;
          }
          setupMailbox();
          break;
        }
        case "STARTTLS": {
          if (secure) {
            cos.write("-ERR Already secure\r\n".getBytes());
            break;
          }
          cos.write("+OK Switching to TLS\r\n".getBytes());
          //upgrade connection to SSL
          c = JF.connectSSL(c, KeyMgmt.getDefaultClient());
          cis = c.getInputStream();
          cos = c.getOutputStream();
          secure = true;
          break;
        }
        case "STAT": {
          //display # messages and total size
          cos.write(stat().getBytes());
          break;
        }
        case "LIST": {
          //list messages
          cos.write(list().getBytes());
          break;
        }
        case "RETR": {
          //get message
          if (mailbox == null) {
            cos.write("-ERR mailbox not ready\r\n".getBytes());
            break;
          }
          int idx = getIndex(p[1]);
          idx--;
          if (idx < 0 || idx >= files.length || files[idx] == null) {
            cos.write("-ERR message not found\r\n".getBytes());
            break;
          }
          try {
            File file = files[idx];
            String realfile = getMailboxFolder(null) + "/" + file.getName();
            FileInputStream fis = new FileInputStream(realfile);
            long size = file.length();
            long sent = 0;
            cos.write(("+OK " + size + " octets\r\n").getBytes());
            byte[] buf = new byte[bufsiz];
            while (sent < size) {
              int read = fis.read(buf);
              if (read == -1) break;
              cos.write(buf, 0, read);
              sent += read;
            }
            fis.close();
            cos.write(".\r\n".getBytes());
          } catch (Exception e) {
            cos.write("-ERR error\r\n".getBytes());
            JFLog.log(e);
          }
          break;
        }
        case "DELE": {
          //delete message
          if (mailbox == null) {
            cos.write("-ERR mailbox not ready\r\n".getBytes());
            break;
          }
          int idx = getIndex(p[1]);
          idx--;
          if (idx < 0 || idx >= files.length || files[idx] == null) {
            cos.write("-ERR message not found\r\n".getBytes());
            break;
          }
          try {
            deleteMsg(files[idx]);
            files[idx] = null;
            cos.write(("+OK message " + (idx+1) + " deleted\r\n").getBytes());
          } catch (Exception e) {
            cos.write("-ERR error\r\n".getBytes());
            JFLog.log(e);
          }
          break;
        }
        case "UIDL": {
          //unique id listing
          String start = "1";
          if (p.length > 1) {
            start = p[1];
          }
          cos.write(uidl(start).getBytes());
          break;
        }
        case "NOOP": {
          cos.write("+OK no operation\r\n".getBytes());
          break;
        }
        default:
          cos.write("-ERR Unknown command\r\n".getBytes());
          break;
      }
    }
    private int getIndex(String arg) {
      int idx;
      if (arg.startsWith("T-")) {
        String find = arg.substring(2) + ".msg";
        idx = 0;
        int offset = 1;
        for(File file : files) {
          if (file.getName().equals(find)) {
            idx = offset;
            break;
          }
          offset++;
        }
      } else {
        idx = Integer.valueOf(arg);
      }
      return idx;
    }
    private void reset() {
      mailbox = null;
      files = null;
    }
    private void deleteMsg(File file) {
      //deletes user .msg file
      //deletes real .msg file if all other recipients have deleted msg
      synchronized(lock) {
        try {
          if (admin) {
            //TODO : check all users do not have .msg file linked (what about ldap users?)
            file.delete();
            return;
          }
          String filename = file.getName();
          FileInputStream fis = new FileInputStream(file);
          byte[] data = fis.readAllBytes();
          fis.close();
          file.delete();  //delete user .msg file
          String[] lns = new String(data).split("\r\n");
          for(String ln : lns) {
            if (!ln.startsWith("to:")) continue;
            String to_user = ln.substring(3);
            String usermailbox = getMailboxFolder(to_user);
            if (new File(usermailbox + "/" + filename).exists()) {
              //msg still linked to another recipient
              return;
            }
          }
          //real msg no longer needed
          String mailbox = getMailboxFolder(null);
          String msgfile = mailbox + "/" + filename;
          new File(msgfile).delete();  //delete real .msg file
        } catch (Exception e) {
          JFLog.log(e);
        }
      }
    }
    private void setupMailbox() {
      if (admin) {
        mailbox = getMailboxFolder(null);
      } else {
        mailbox = getMailboxFolder(user);
      }
      listFiles();
    }
    private void listFiles() {
      files = new File(mailbox).listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(".msg");
        }
      });
    }
    private String stat() {
      if (mailbox == null) {
        return "-ERR Mailbox not ready\r\n";
      }
      int cnt = 0;
      int size = 0;
      listFiles();
      for(File file : files) {
        cnt++;
        size += file.length();
      }
      StringBuilder reply = new StringBuilder();
      reply.append("+OK ");
      reply.append(cnt);
      reply.append(" ");
      reply.append(size);
      reply.append("\r\n");
      return reply.toString();
    }
    private String list() {
      if (mailbox == null) {
        return "-ERR Mailbox not ready\r\n";
      }
      int cnt = 0;
      int size = 0;
      StringBuilder list = new StringBuilder();
      int idx = 1;
      listFiles();
      for(File file : files) {
        cnt++;
        long msgsize = file.length();
        size += msgsize;
        list.append(idx++);
        list.append(" ");
        list.append(msgsize);
        list.append("\r\n");
      }
      StringBuilder reply = new StringBuilder();
      reply.append("+OK ");
      reply.append(cnt);
      reply.append(" ");
      reply.append(size);
      reply.append("\r\n");
      reply.append(list);
      reply.append(".\r\n");
      return reply.toString();
    }
    private String uidl(String start) {
      if (mailbox == null) {
        return "-ERR Mailbox not ready\r\n";
      }
      int sidx = 1;
      if (start.length() > 0) {
        sidx = JF.atoi(start);
      }
      StringBuilder list = new StringBuilder();
      int idx = 0;
      listFiles();
      for(File file : files) {
        String filename = file.getName();
        int ext = filename.indexOf('.');
        filename = filename.substring(0, ext);
        idx++;
        if (idx < sidx) continue;
        list.append(idx);  //index
        list.append(" T-");
        list.append(filename);  //unique ID (timestamp when file was created)
        list.append("\r\n");
      }
      StringBuilder reply = new StringBuilder();
      reply.append("+OK unique ID list follows...\r\n");
      reply.append(list);
      reply.append(".\r\n");
      return reply.toString();
    }
  }

  private static POP3Server pop3;

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    pop3 = new POP3Server();
    pop3.start();
  }

  public static void serviceStop() {
    JFLog.log("POP3 : Stopping service");
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
    pop3.stop();
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static boolean createKeys() {
    return KeyMgmt.keytool(new String[] {
      "-genkey", "-debug", "-alias", "jfpop3", "-keypass", "password", "-storepass", "password",
      "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfpop3.sourceforge.net, OU=user, O=server, C=CA",
      "-keyalg" , "RSA", "-keysize", "2048"
    });
  }

  public static class JBusMethods {
    public void getConfig(String pack) {
      pop3.busClient.call(pack, "getConfig", pop3.busClient.quote(pop3.busClient.encodeString(pop3.config)));
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
      pop3.stop();
      pop3 = new POP3Server();
      pop3.start();
    }

    public void genKeys(String pack) {
      if (createKeys()) {
        JFLog.log("Generated Keys");
        pop3.busClient.call(pack, "getKeys", pop3.busClient.quote("OK"));
      } else {
        pop3.busClient.call(pack, "getKeys", pop3.busClient.quote("ERROR"));
      }
    }
  }
}
