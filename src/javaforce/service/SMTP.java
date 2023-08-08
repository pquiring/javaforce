package javaforce.service;

/** SMTP Server.
 *
 * Simple non-relaying mail server.
 *
 * Supports 25, 465(ssl), 587(tls)
 *
 * See SMTPEvents
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.jbus.*;

public class SMTP extends Thread {
  public final static String busPack = "net.sf.jfsmtp";

  public static boolean debug = false;

  public interface Events {
    public void message(SMTP server, String msgfile);
  }

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfsmtp.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfsmtp.log";
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
      return 33009;
    } else {
      return 777;
    }
  }

  private static SMTP smtp;
  private static Events events;
  private static volatile boolean active;
  private static ArrayList<ServerWorker> servers = new ArrayList<ServerWorker>();
  private static ArrayList<ClientWorker> clients = new ArrayList<ClientWorker>();
  private static String domain;
  private static String ldap_domain;
  private static String ldap_server;
  private static ArrayList<EMail> users;
  private static Object lock = new Object();
  private static IP4Port bind = new IP4Port();
  private static ArrayList<Subnet4> subnet_src_list;
  private static ArrayList<Integer> ports = new ArrayList<>();
  private static ArrayList<Integer> ssl_ports = new ArrayList<>();
  private static boolean digest = false;  //any message is accepted ignoring all recipents (use POP3 admin account to retrieve)

  public SMTP() {
  }

  public void setEvents(Events events) {
    SMTP.events = events;
  }

  private static void addSession(ClientWorker sess) {
    synchronized(lock) {
      clients.add(sess);
    }
  }

  private static void removeSession(ClientWorker sess) {
    synchronized(lock) {
      clients.remove(sess);
    }
  }

  private static String getKeyFile() {
    return JF.getConfigPath() + "/jfsmtp.key";
  }

  public void run() {
    JFLog.append(JF.getLogPath() + "/jfsmtp.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfSMTP starting...");
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

  public void close() {
    active = false;
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
    + "port=25\n"  //default port (supports SSL)
    + "#port=587\n"  //explicit SSL port
    + "#secure=465\n"  //implicit SSL port
    + "#bind=192.168.100.2\n"
    + "#domain=example.com\n"
    + "#ldap_domain=example.org\n"
    + "#ldap_server=192.168.200.2\n"
    + "#account=user:pass\n"
    + "#digest=true\n"  //digest mode (see POP3/SMTPRelay services)
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
              case "domain":
                domain = value;
                break;
              case "ldap_domain":
                ldap_domain = value;
                break;
              case "ldap_server":
                ldap_server = value;
                break;
              case "digest":
                digest = value.equals("true");
                break;
              case "debug":
                debug = value.equals("true");
                break;
              case "src.ipnet": {
                Subnet4 subnet = new Subnet4();
                idx = value.indexOf('/');
                if (idx == -1) {
                  JFLog.log("SMTP:Invalid IP Subnet:" + value);
                  break;
                }
                String ip = value.substring(0, idx);
                String mask = value.substring(idx + 1);
                if (!subnet.setIP(ip)) {
                  JFLog.log("SMTP:Invalid IP:" + ip);
                  break;
                }
                if (!subnet.setMask(mask)) {
                  JFLog.log("SMTP:Invalid netmask:" + mask);
                  break;
                }
                JFLog.log("Source Allow IP Network=" + subnet.toString());
                subnet_src_list.add(subnet);
                break;
              }
              case "src.ip": {
                Subnet4 subnet = new Subnet4();
                if (!subnet.setIP(value)) {
                  JFLog.log("SMTP:Invalid IP:" + value);
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

  public static boolean login(String user, String pass) {
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

  public static class ServerWorker extends Thread {
    private ServerSocket ss;
    private int port;
    private boolean secure;

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
            keys.open(fis, "password".toCharArray());
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
        active = true;
        while (active) {
          Socket s = ss.accept();
          InetSocketAddress sa = (InetSocketAddress)s.getRemoteSocketAddress();
          String src_ip = sa.getAddress().getHostAddress();
          if (src_ip.equals("0:0:0:0:0:0:0:1")) {
            //IP6 localhost
            src_ip = "127.0.0.1";
          }
          if (!ip_src_allowed(src_ip)) {
            JFLog.log("SMTP:Source IP blocked:" + src_ip);
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

  private static boolean ip_src_allowed(String ip4) {
    if (subnet_src_list.size() == 0) return true;
    IP4 target = new IP4();
    if (!target.setIP(ip4)) return false;
    for(Subnet4 net : subnet_src_list) {
      if (net.matches(target)) {
        return true;
      }
    }
    return false;
  }

  private static boolean userExists(EMail email) {
    for(EMail acct : users) {
      if (acct.user.equals(email.user)) {
        return true;
      }
    }
    //TODO : ldap users? (don't have password)
    return false;
  }

  public static class ClientWorker extends Thread {
    private Socket c;
    private boolean secure;
    private InputStream cis = null;
    private OutputStream cos = null;

    private byte[] req = new byte[1500];
    private int reqSize = 0;

    private byte[] buffer = new byte[1500];
    private int bufferSize = 0;

    private EMail from;
    private ArrayList<EMail> to = new ArrayList<>();

    public ClientWorker(Socket s, boolean secure) {
      c = s;
      this.secure = secure;
    }
    public void close() {
      if (c != null) {
        try { c.close(); } catch (Exception e) {}
        c = null;
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
        cos.write(("220 jfSMTP Server/" + JF.getVersion() + "\r\n").getBytes());
        while (c != null && c.isConnected()) {
          String cmd = readln();
          if (cmd == null) break;
          if (cmd.equalsIgnoreCase("QUIT")) {
            cos.write("221 Goodbye\r\n".getBytes());
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

    private void doCommand(String cmd) throws Exception {
      if (debug) {
        JFLog.log("Request=" + cmd);
      }
      String[] p = cmd.split(" ", 2);
      switch (p[0].toUpperCase()) {
        case "HELO":
        case "EHLO":
          cos.write(("250 jfSMTP Server/" + JF.getVersion() + "\r\n").getBytes());
          break;
        case "AUTH":
          String type = p[1];
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
                cos.write("235 Login successful\r\n".getBytes());
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
          break;
        case "STARTTLS":
          if (secure) {
            cos.write("550 Already secure\r\n".getBytes());
            break;
          }
          cos.write("220 Ok\r\n".getBytes());
          //upgrade connection to SSL
          c = JF.connectSSL(c);
          cis = c.getInputStream();
          cos = c.getOutputStream();
          secure = true;
          break;
        case "DATA":
          if (from == null) {
            cos.write("550 No from address\r\n".getBytes());
            break;
          }
          if (to.size() == 0) {
            cos.write("550 No recipients\r\n".getBytes());
            break;
          }
          cos.write("354 Send Data\r\n".getBytes());
          String filename;
          synchronized(lock) {
            filename = Long.toString(System.currentTimeMillis());
            JF.sleep(10);
          }
          String basefile = getMailboxFolder(null) + "/" + filename;
          String quefile = basefile + ".que";
          String msgfile = basefile + ".msg";
          OutputStream questream = new FileOutputStream(quefile);
          while (c != null && c.isConnected()) {
            String ln = readln();
            if (ln == null) {close(); return;}
            if (ln.equals(".")) {
              break;
            }
            questream.write(ln.getBytes());
            questream.write("\r\n".getBytes());
          }
          questream.close();
          new File(quefile).renameTo(new File(msgfile));
          if (!digest) {
            //link message to each recipient
            StringBuilder to_list = new StringBuilder();
            for(EMail acct : to) {
              to_list.append("to:");
              to_list.append(acct.user);
              to_list.append("\r\n");
            }
            byte[] to_data = to_list.toString().getBytes();
            for(EMail acct : to) {
              String userbox = getMailboxFolder(acct.user);
              String userbasefile = userbox + "/" + filename;
              String userquefile = userbasefile + ".que";
              String usermsgfile = userbasefile + ".msg";
              FileOutputStream fos = new FileOutputStream(userquefile);
              fos.write(to_data);
              fos.close();
              new File(userquefile).renameTo(new File(usermsgfile));
            }
          }
          if (events != null) {
            events.message(smtp, msgfile);
          }
          cos.write("250 Ok\r\n".getBytes());
          reset();
          break;
        case "MAIL":
          if (from != null) {
            cos.write("550 Already have from email\r\n".getBytes());
            break;
          }
          from = new EMail();
          if (!from.set(cmd)) {  //MAIL FROM:<user@domain.com>
            from = null;
            cos.write("550 Invalid from address\r\n".getBytes());
            break;
          }
          cos.write("250 Ok\r\n".getBytes());
          break;
        case "RCPT":
          EMail email = new EMail();
          if (!email.set(cmd)) {
            cos.write("550 Invalid to address\r\n".getBytes());
            break;
          }
          if (!digest) {
            if (domain != null && !email.domain.equals(domain)) {
              cos.write("550 Server will not relay messages\r\n".getBytes());
              break;
            }
            if (!userExists(email)) {
              cos.write("550 User not found\r\n".getBytes());
              break;
            }
          }
          to.add(email);
          cos.write("250 Ok\r\n".getBytes());
          break;
        default:
          cos.write("500 Unknown command\r\n".getBytes());
          break;
      }
    }
    private void reset() {
      from = null;
      to.clear();
    }
  }

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    smtp = new SMTP();
    smtp.start();
  }

  public static void serviceStop() {
    smtp.close();
  }

  public static void main(String[] args) {
    serviceStart(args);
    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        serviceStop();
      }
    });
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      smtp.busClient.call(pack, "getConfig", smtp.busClient.quote(smtp.busClient.encodeString(smtp.config)));
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
      smtp.close();
      smtp = new SMTP();
      smtp.start();
    }

    public void genKeys(String pack) {
      if (KeyMgmt.keytool(new String[] {
        "-genkey", "-debug", "-alias", "jfsmtp", "-keypass", "password", "-storepass", "password",
        "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfsmtp.sourceforge.net, OU=user, O=server, C=CA",
        "-keyalg" , "RSA", "-keysize", "2048"
      })) {
        JFLog.log("Generated Keys");
        smtp.busClient.call(pack, "getKeys", smtp.busClient.quote("OK"));
      } else {
        smtp.busClient.call(pack, "getKeys", smtp.busClient.quote("ERROR"));
      }
    }
  }
}
