package javaforce.service;

/** POP3 Server.
 *
 * Retrieves messages from SMTP service.
 *
 * Supported ports : 110, 995(ssl)
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

import javaforce.*;
import javaforce.net.*;
import javaforce.jbus.*;

public class POP3 extends Thread {
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
    if (!digest) {
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

  private static POP3 pop3;
  private static volatile boolean active;
  private static ArrayList<ServerWorker> servers = new ArrayList<ServerWorker>();
  private static ArrayList<ClientWorker> clients = new ArrayList<ClientWorker>();
  private static String domain;
  private static String ldap_server = null;
  private static ArrayList<String> user_pass_list;
  private static Object lock = new Object();
  private static IP4Port bind = new IP4Port();
  private static ArrayList<Integer> ports = new ArrayList<>();
  private static ArrayList<Integer> ssl_ports = new ArrayList<>();
  private static boolean digest = false;  //messages are stored in global mailbox for retrieval by SMTPRelay agent

  public POP3() {
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
    return JF.getConfigPath() + "/jfpop3.key";
  }

  public void run() {
    JFLog.append(JF.getLogPath() + "/jfpop3.log", true);
    JFLog.setRetention(30);
    JFLog.log("jfPOP3 starting...");
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
    + "port=110\n"  //default port (supports SSL)
    + "#secure=995\n"  //implicit SSL port
    + "#bind=192.168.100.2\n"
    + "#domain=example.com\n"
    + "#ldap_server=192.168.200.2\n"
    + "#account=user:pass\n"
    + "#digest=true\n";  //digest mode (global mailbox)

  private void loadConfig() {
    JFLog.log("loadConfig");
    user_pass_list = new ArrayList<String>();
    Section section = Section.None;
    bind.setIP("0.0.0.0");  //bind to all interfaces
    bind.port = 110;
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
              case "account":
                user_pass_list.add(value);
                break;
              case "domain":
                domain = value;
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

  public static boolean login(String user, String pass, boolean _md5) {
    for(String u_p : user_pass_list) {
      int idx = u_p.indexOf(':');
      if (idx == -1) continue;
      String u = u_p.substring(0, idx);
      String p = u_p.substring(idx + 1);
      if (u.equals(user)) {
        if (_md5) {
          MD5 md5 = new MD5();
          md5.add(p);
          return md5.toString().equals(pass);
        } else {
          return p.equals(pass);
        }
      }
    }
    if (ldap_server != null && domain != null) {
      LDAP ldap = new LDAP();
      return ldap.login(ldap_server, domain, user, pass);
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

  public static class ClientWorker extends Thread {
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
    private String from;
    private ArrayList<String> to = new ArrayList<>();

    private String user;

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
        System.arraycopy(buffer, 0, req, 0, buffer.length);
        reqSize = buffer.length;
        bufferSize = 0;
      } else {
        reqSize = 0;
      }
      try {
        while (c.isConnected()) {
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
          //read more data
          int read = cis.read(req, reqSize, req.length - reqSize);
          if (read < 0) break;
          reqSize += read;
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
          c = JF.connectSSL(c);
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
          int idx = Integer.valueOf(p[1]);
          idx--;
          if (idx < 0 || idx >= files.length || files[idx] == null) {
            cos.write("-ERR message not found\r\n".getBytes());
            break;
          }
          try {
            File file = files[idx];
            FileInputStream fis = new FileInputStream(file);
            long size = file.length();
            long sent = 0;
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
          int idx = Integer.valueOf(p[1]);
          idx--;
          if (idx < 0 || idx >= files.length || files[idx] == null) {
            cos.write("-ERR message not found\r\n".getBytes());
            break;
          }
          try {
            files[idx].delete();
            files[idx] = null;
            cos.write(("+OK message " + idx + " deleted\r\n").getBytes());
          } catch (Exception e) {
            cos.write("-ERR error\r\n".getBytes());
            JFLog.log(e);
          }
          break;
        }
        default:
          cos.write("-ERR Unknown command\r\n".getBytes());
          break;
      }
    }
    private String getHeaders() {
      StringBuilder sb = new StringBuilder();
      sb.append("MAIL FROM:<" + from + ">\r\n");
      for(String t : to) {
        sb.append("RCPT TO:<" + t + ">\r\n");
      }
      return sb.toString();
    }
    private void reset() {
      from = null;
      mailbox = null;
      files = null;
      to.clear();
    }
    private void setupMailbox() {
      mailbox = getMailboxFolder(user);
      listFiles();
    }
    private void listFiles() {
      files = new File(mailbox).listFiles();
    }
    private String stat() {
      if (mailbox == null) {
        return "-ERR Mailbox not ready";
      }
      int cnt = 0;
      int size = 0;
      listFiles();
      for(File file : files) {
        if (!file.getName().endsWith(".msg")) continue;
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
        return "-ERR Mailbox not ready";
      }
      int cnt = 0;
      int size = 0;
      StringBuilder list = new StringBuilder();
      int idx = 1;
      listFiles();
      for(File file : files) {
        if (!file.getName().endsWith(".msg")) continue;
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
  }

  public static String strip_email(String email) {
    int i1 = email.indexOf('<');
    if (i1 == -1) return null;
    int i2 = email.indexOf('>');
    if (i2 == -1) return null;
    return email.substring(i1 + 1, i2);
  }

  public static void serviceStart(String[] args) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    pop3 = new POP3();
    pop3.start();
  }

  public static void serviceStop() {
    pop3.close();
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
      pop3.close();
      pop3 = new POP3();
      pop3.start();
    }

    public void genKeys(String pack) {
      if (KeyMgmt.keytool(new String[] {
        "-genkey", "-debug", "-alias", "jfpop3", "-keypass", "password", "-storepass", "password",
        "-keystore", getKeyFile(), "-validity", "3650", "-dname", "CN=jfpop3.sourceforge.net, OU=user, O=server, C=CA",
        "-keyalg" , "RSA", "-keysize", "2048"
      })) {
        JFLog.log("Generated Keys");
        pop3.busClient.call(pack, "getKeys", pop3.busClient.quote("OK"));
      } else {
        pop3.busClient.call(pack, "getKeys", pop3.busClient.quote("ERROR"));
      }
    }
  }
}
