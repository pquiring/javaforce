package javaforce.service;

/**
 * Web Proxy Server
 *
 * @author pquiring
 *
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;

import javaforce.*;
import javaforce.jbus.*;

public class ProxyServer extends Thread {

  public final static String busPack = "net.sf.jfproxy";

  public static String getConfigFile() {
    return JF.getConfigPath() + "/jfproxy.cfg";
  }

  public static String getLogFile() {
    return JF.getLogPath() + "/jfproxy.log";
  }

  private static class URLChange {
    public String url, newurl;
  }

  private ServerSocket ss;
  private static Vector<Session> list = new Vector<Session>();
  private static ArrayList<String> blockedDomain = new ArrayList<String>();
  private static ArrayList<String> blockedURL = new ArrayList<String>();
  private static ArrayList<URLChange> urlChanges = new ArrayList<URLChange>();
  private static ArrayList<Integer> allow_net = new ArrayList<Integer>();
  private static ArrayList<Integer> allow_mask = new ArrayList<Integer>();
  private static int port = 3128;
  private static int nextSSLport = 8081;
  private static boolean filtersecure = false;
  private static HashMap<String, SecureSite> secureSites = new HashMap<String,SecureSite>();
  private static String keyPath;
  private static SSLSocketFactory sslSocketFactory;

  public void close() {
    JFLog.logTrace("proxy.close()");
    try {
      ss.close();
    } catch (Exception e) {}
    busClient.close();
    //close list
    Session sess;
    while (list.size() > 0) {
      sess = list.get(0);
      sess.close();
    }
    if (filtersecure) {
      SecureSite[] ssList = secureSites.values().toArray(new SecureSite[0]);
      for(int a=0;a<ssList.length;a++) {
        ssList[a].close();
      }
      secureSites.clear();
      deleteKeys();
    }
  }

  public void deleteKeys() {
    //delete all temp files in ~/.jfproxy.keys
    File[] files = new File(keyPath).listFiles();
    if (files != null) {
      for(int a=0;a<files.length;a++) {
        if (files[a].getName().startsWith("localhost.")) continue;
        files[a].delete();
      }
    }
  }

  public void run() {
    JFLog.append(getLogFile(), true);
    JFLog.setRetention(30);
    JFLog.log("Proxy : Starting service");
    Socket s;
    Session sess;
    loadConfig();
    busClient = new JBusClient(busPack, new JBusMethods());
    busClient.setPort(getBusPort());
    busClient.start();
    if (filtersecure) {
      JFLog.log("Setting up secure server...");
      keyPath = JF.getConfigPath() + "/jfproxy";
      new File(keyPath).mkdir();
      deleteKeys();
      if (!new File(keyPath + "/localhost.key").exists()) {
        keytool(new String[] {
          "-genkeypair",
          "-alias",
          "localhost",
          "-keystore",
          "localhost.key",
          "-storepass",
          "password",
          "-keypass",
          "password",
          "-keyalg",
          "RSA",
          "-dname",
          "CN=localhost, OU=JavaForce, O=JavaForce, C=CA",
          "-validity",
          "3650",
          "-ext",
          "KeyUsage=digitalSignature,keyEncipherment,keyCertSign,codeSigning",
          "-ext",
          "ExtendedKeyUsage=serverAuth,clientAuth",
          "-ext",
          "BasicConstraints=ca:true,pathlen:3",
        });
        keytool(new String[] {
          "-exportcert",
          "-alias",
          "localhost",
          "-keystore",
          "localhost.key",
          "-storepass",
          "password",
          "-file",
          "localhost.crt"
        });
      }
      initSSL();
    }
    //try to bind to port 5 times (in case restart() takes a while)
    for(int a=0;a<5;a++) {
      try {
        ss = new ServerSocket(port);
      } catch (Exception e) {
        if (a == 4) return;
        JF.sleep(1000);
        continue;
      }
      break;
    }
    try {
      JFLog.log("Starting proxy on port " + port);
      while (!ss.isClosed()) {
        s = ss.accept();
        sess = new Session(s, false);
        sess.start();
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Executes keytool directly */
  public static boolean keytool(String[] args) {
    ArrayList<String> cmd = new ArrayList<String>();
    try {
      if (JF.isWindows()) {
        cmd.add(System.getProperty("java.home") + "\\bin\\keytool.exe");
      } else {
        cmd.add(System.getProperty("java.home") + "/bin/keytool");
      }
      for(int a=0;a<args.length;a++) {
        cmd.add(args[a]);
      }
      Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), null, new File(keyPath));
      p.waitFor();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private static enum Section {None, Global, BlockDomain, URLChange, BlockURL};

  private final static String defaultConfig
    = "[global]\n"
    + "port=3128\n"
    + "allow=0.0.0.0/0 #allow all\n"
    + "#allow=192.168.0.0/24 #allow subnet\n"
    + "#allow=10.1.2.3/32 #allow single ip\n"
    + "\n"
    + "[blockdomain]\n"
    + ".*youtube[.]com\n"
    + "\n"
    + "[urlchange]\n"
    + "#www.example.com/test = www.google.com\n";

  private void loadConfig() {
    filtersecure = false;
    Section section = Section.None;
    try {
      StringBuilder cfg = new StringBuilder();
      BufferedReader br = new BufferedReader(new FileReader(getConfigFile()));
      while (true) {
        String ln = br.readLine();
        if (ln == null) break;
        cfg.append(ln);
        cfg.append("\n");
        ln = ln.trim().toLowerCase();
        int cmt = ln.indexOf('#');
        if (cmt != -1) ln = ln.substring(0, cmt).trim();
        if (ln.length() == 0) continue;
        if (ln.equals("[global]")) {
          section = Section.Global;
          continue;
        }
        if (ln.equals("[blockdomain]")) {
          section = Section.BlockDomain;
          continue;
        }
        if (ln.equals("[urlchange]")) {
          section = Section.URLChange;
          continue;
        }
        if (ln.equals("[blockurl]")) {
          section = Section.BlockURL;
          continue;
        }
        int idx = ln.indexOf("=");
        if (idx == -1) continue;
        String key = ln.substring(0, idx).toLowerCase().trim();
        String value = ln.substring(idx+1).trim();
        switch (section) {
          case Global:
            switch (key) {
              case "port":
                port = JF.atoi(value);
                break;
              case "filtersecure":
                filtersecure = value.equals("true");
                break;
              case "allow":
                String net_mask = value;
                idx = net_mask.indexOf("/");
                String net = net_mask.substring(0, idx);
                int addr = getIP(net);
                allow_net.add(addr);
                String mask = net_mask.substring(idx+1);
                int maskBits = getMask(mask);
                allow_mask.add(maskBits);
                break;
            }
            break;
          case BlockDomain:
            blockedDomain.add(ln);
            break;
          case URLChange:
            int eq = ln.indexOf(" = ");
            if (eq == -1) {
              JFLog.log("Bad URLChange:" + ln);
              break;
            }
            URLChange uc = new URLChange();
            uc.url = ln.substring(0, eq);
            uc.newurl = ln.substring(eq+3);
            urlChanges.add(uc);
            break;
          case BlockURL:
            blockedURL.add(ln);
            break;
        }
      }
      config = cfg.toString();
    } catch (FileNotFoundException e) {
      //create default config
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

  private static int ba2int(byte ba[]) {
    int ret = 0;
    for(int a=0;a<4;a++) {
      ret <<= 8;
      ret += ((int)ba[a]) & 0xff;
    }
    return ret;
  }

  private static int getIP(String ip) {
    String p[] = ip.split("[.]");
    byte o[] = new byte[4];
    for(int a=0;a<4;a++) {
      o[a] = (byte)JF.atoi(p[a]);
    }
    return ba2int(o);
  }

  private int getMask(String mask) {
    int bits = JF.atoi(mask);
    if (bits == 0) return 0;
    int ret = 0x80000000;
    bits--;
    while (bits > 0) {
      ret >>= 1;  //signed shift will repeat the sign bit (>>>=1 would not)
      bits--;
    }
    return ret;
  }

  private static void initSSL() {
    try {
      TrustManager[] trustAllCerts = new TrustManager[] {
        new X509TrustManager() {
          public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
          }
          public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
          public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        }
      };
      // Let us create the factory where we can set some parameters for the connection
      SSLContext gsc = SSLContext.getInstance("SSL");

      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream(keyPath + "/localhost.key"), "password".toCharArray());
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, "password".toCharArray());

      gsc.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
      sslSocketFactory = (SSLSocketFactory) gsc.getSocketFactory();  //this method will work with untrusted certs
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static int getSSLPort(String host) {
    if (host.equals("localhost")) return -1;  //should not happen
    if (host.equals("127.0.0.1")) return -1;  //should not happen
    SecureSite secureSite;
    synchronized(secureSites) {
      secureSite = secureSites.get(host);
      if (secureSite != null) {
        return secureSite.port;
      }
      JFLog.log("Creating new SSL site:" + host);
      secureSite = new SecureSite(host, nextSSLport++);
      secureSites.put(host, secureSite);
    }
    synchronized(secureSite.lock) {
      secureSite.start();
      try {secureSite.lock.wait();} catch (Exception e) {
        JFLog.log(e);
      }
    }
    return secureSite.port;
  }

  public static class SecureSite extends Thread {
    public String domain;
    public SSLContext sc;
    public SSLServerSocketFactory serverSocketFactory;
    public SSLServerSocket ss;
    public int port;
    public Object lock = new Object();
    public SecureSite(String domain, int port) {
      this.domain = domain;
      this.port = port;
    }
    public void run() {
      //create a cert for domain signed by localhost
      keytool(new String[] {
        "-genkeypair",
        "-alias",
        domain,
        "-keystore",
        domain + ".key",
        "-storepass",
        "password",
        "-keypass",
        "password",
        "-keyalg",
        "RSA",
        "-dname",
        "CN=" + domain + ", OU=JavaForce, O=JavaForce, C=CA",
        "-validity",
        "3650"
      });
      //create csr
      keytool(new String[] {
        "-certreq",
        "-alias",
        domain,
        "-keystore",
        domain + ".key",
        "-file",
        domain + ".csr",
        "-storepass",
        "password"
      });
      //sign key
      keytool(new String[] {
        "-gencert",
        "-alias",
        "localhost",
        "-keystore",
        "localhost.key",
        "-storepass",
        "password",
        "-keyalg",
        "RSA",
        "-infile",
        domain + ".csr",
        "-outfile",
        domain + ".crt",
        "-validity",
        "3650"
      });
      //import cert auth (optional ???)
      keytool(new String[] {
        "-import",
        "-alias",
        "root",
        "-file",
        "localhost.crt",
        "-keystore",
        domain + ".key",
        "-storepass",
        "password",
        "-noprompt"
      });
      //import signed cert
      keytool(new String[] {
        "-import",
        "-alias",
        domain,
        "-file",
        domain + ".crt",
        "-keystore",
        domain + ".key",
        "-storepass",
        "password"
      });
      try {
        TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return null;
            }
            public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
            public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
          }
        };
        // Let us create the factory where we can set some parameters for the connection
        sc = SSLContext.getInstance("SSL");

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keyPath + "/" + domain + ".key"), "password".toCharArray());
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, "password".toCharArray());

        sc.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
        serverSocketFactory = (SSLServerSocketFactory) sc.getServerSocketFactory();  //this method will work with untrusted certs

        SSLSocket s;
        Session sess;
        ss = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        synchronized(lock) {
          lock.notify();
        }
        while (!ss.isClosed()) {
          s = (SSLSocket)ss.accept();
          sess = new Session(s, true);
          sess.start();
        }
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void close() {
      try {
        JFLog.log("Closing port:" + port);
        ss.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }

  private static class Session extends Thread {
    private Socket p, i;  //proxy, internet
    private InputStream pis, iis;
    private OutputStream pos, ios;
    private boolean disconn = false;
    private int client_port;
    private String client_ip;
    private boolean secure;
    public synchronized void close() {
      try {
        if ((p!=null) && (p.isConnected())) p.close();
        p = null;
      } catch (Exception e1) {}
      try {
        if ((i!=null) && (i.isConnected())) i.close();
        i = null;
      } catch (Exception e2) {}
      list.remove(this);
    }
    public Session(Socket s, boolean secure) {
      p = s;
      this.secure = secure;
    }
    public String toString(int ip) {
      long ip64 = ((long)ip) & 0xffffffffL;
      return Long.toString(ip64, 16);
    }
    private void log(String s) {
      JFLog.log(client_ip + ":" + client_port + ":" + s);
    }
    private void log(Exception e) {
      String s = e.toString();
      StackTraceElement stack[] = e.getStackTrace();
      for(int a=0;a<stack.length;a++) {
        s += "\r\n" + stack[a].toString();
      }
      log(s);
    }
    public void run() {
      String req = "";
      int ch;
      list.add(this);
      client_port = p.getPort();
      client_ip = p.getInetAddress().getHostAddress();
      log("Session Start");
      try {
        boolean allowed = false;
        for(int a=0;a<allow_net.size();a++) {
          int net = allow_net.get(a);
          int mask = allow_mask.get(a);
          int host = getIP(p);
          if ((net & mask) == (host & mask)) {
            allowed = true;
            break;
          }
        }
        if (!allowed) throw new Exception("client not allowed");
        pis = p.getInputStream();
        pos = p.getOutputStream();
        while (true) {
          req = "";
          log("reading request");
          do {
            ch = pis.read();
            if (ch == -1) throw new Exception("read error");
            req += (char)ch;
          } while (!req.endsWith("\r\n\r\n"));
          proxy(req);
          if (disconn) {
            log("disconn");
            break;
          }
        }
        p.close();
      } catch (Exception e) {
        if (req.length() > 0) log(e);
      }
      close();
      log("Session Stop");
    }
    private int getIP(Socket s) {
      if (s.getInetAddress().isLoopbackAddress()) return 0x7f000001;  //loopback may return IP6 address
      byte o[] = s.getInetAddress().getAddress();
      return ba2int(o);
    }
    private void proxy(String req) throws Exception {
      String ln[] = req.split("\r\n");
      log("Proxy:" + ln[0]);
      int hostidx = -1;
      if (ln[0].endsWith("1.0")) disconn = true;  //HTTP/1.0
      for(int a=0;a<ln.length;a++) {
        if (ln[a].regionMatches(true, 0, "Host: ", 0, 6)) hostidx = a;
      }
      if (hostidx == -1) {
        log("ERROR : No host specified : " + req);
        replyError(505, "No host specified");
        return;
      }
      String hostln = ln[hostidx].substring(6);  //"Host: "
      String host;
      try {
        String method = null, proto = null, url = null, http = null;
        int port;
        String f[] = ln[0].split(" ");
        method = f[0];
        url = f[1];
        http = f[2];
        if (url.startsWith("http://")) {
          proto = "http://";
          url = url.substring(7);
          port = 80;
        } else if (url.startsWith("ftp://")) {
          proto = "ftp://";
          url = url.substring(6);
          port = 21;
        } else {
          proto = "http://";  //assume http
          port = secure ? 443 : 80;
        }
        int portidx = hostln.indexOf(':');
        if (portidx != -1) {
          host = hostln.substring(0, portidx);
          port = Integer.valueOf(hostln.substring(portidx+1));
        } else {
          host = hostln;
        }
        //check if host is blocked
        host = host.trim().toLowerCase();
        for(int a=0;a<blockedDomain.size();a++) {
          if (host.matches(blockedDomain.get(a))) {
            replyError(505, "Access Denied");
            return;
          }
        }
        //check if URL is blocked
        for(int a=0;a<blockedURL.size();a++) {
          if ((host + "/" + url).matches(blockedURL.get(a))) {
            replyError(505, "Access Denied");
            return;
          }
        }
        if (method.equals("CONNECT")) {
          connectCommand(host, ln[0]);
          return;
        }
        //check if url is changed
        for(int a=0;a<urlChanges.size();a++) {
          URLChange uc = urlChanges.get(a);
          if (url.matches(uc.url)) {
            url = uc.newurl;
            ln[0] = method + " " + proto + url + " " + http;
            int iport = url.indexOf(":");
            int iurl = url.indexOf("/");
            if (iurl == -1) iurl = url.length();
            if (iport == -1) {
              port = 80;
              host = url.substring(0, iurl);
              ln[hostidx] = "Host: " + host;
            } else {
              port = JF.atoi(url.substring(iport+1, iurl));
              host = url.substring(0, iport);
              ln[hostidx] = "Host: " + host + ":" + port;
            }
            break;
          }
        }
        if (proto.equals("http://")) {
          connect(host, port);
          sendRequest(ln);
          if (method.equals("POST")) sendPost(ln);
          relayReply(proto + url);
        } else {
          ftp(host, port, url);
        }
        return;
      } catch (UnknownHostException uhe) {
        replyError(404, "Domain not found");
        log(uhe);
      } catch (IOException ioe) {
        /*do nothing*/
        log(ioe);
      } catch (Exception e) {
        replyError(505, "Exception:" + e);
        log(e);
      }
    }
    private void connect(String host, int port) throws UnknownHostException, IOException {
      log("connect:" + host + ":" + port);
      if (!secure) {
        i = new Socket(host, port);
      } else {
        i = sslSocketFactory.createSocket(host, port);
      }
      iis = i.getInputStream();
      ios = i.getOutputStream();
    }
    private void ftp(String host, int port, String url) throws Exception {
      int idx = url.indexOf('/');
      url = url.substring(idx);  //remove host
      FTP ftp = new FTP();
      log("ftp:" + url);
      try {
        ftp.connect(host, port);
        ftp.login("anonymous", "nobody@jfproxy.sf.net");
        if (url.endsWith("/")) {
          //directory listing
          String ls = ftp.ls(url);
          String lns[] = ls.replaceAll("\r", "").split("\n");
          StringBuffer content = new StringBuffer();
          content.append("<html><head>");
          //TODO : fix this for Firefox - only works with Chrome
          content.append("<style type=\"text/css\">\r\n");
          content.append(".file\r\n {\r\nbackground :\r\n url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAABnRSTlMAAAAAAABupgeRAAABHUlEQVR42o2RMW7DIBiF3498iHRJD5JKHurL+CRVBp+i2T16tTynF2gO0KSb5ZrBBl4HHDBuK/WXACH4eO9/CAAAbdvijzLGNE1TVZXfZuHg6XCAQESAZXbOKaXO57eiKG6ft9PrKQIkCQqFoIiQFBGlFIB5nvM8t9aOX2Nd18oDzjnPgCDpn/BH4zh2XZdlWVmWiUK4IgCBoFMUz9eP6zRN75cLgEQhcmTQIbl72O0f9865qLAAsURAAgKBJKEtgLXWvyjLuFsThCSstb8rBCaAQhDYWgIZ7myM+TUBjDHrHlZcbMYYk34cN0YSLcgS+wL0fe9TXDMbY33fR2AYBvyQ8L0Gk8MwREBrTfKe4TpTzwhArXWi8HI84h/1DfwI5mhxJamFAAAAAElFTkSuQmCC') left top no-repeat;\r\npadding-left : 24px;\r\n}\r\n");
          content.append(".folder\r\n {\r\nbackground :\r\n url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAd5JREFUeNqMU79rFUEQ/vbuodFEEkzAImBpkUabFP4ldpaJhZXYm/RiZWsv/hkWFglBUyTIgyAIIfgIRjHv3r39MePM7N3LcbxAFvZ2b2bn22/mm3XMjF+HL3YW7q28YSIw8mBKoBihhhgCsoORot9d3/ywg3YowMXwNde/PzGnk2vn6PitrT+/PGeNaecg4+qNY3D43vy16A5wDDd4Aqg/ngmrjl/GoN0U5V1QquHQG3q+TPDVhVwyBffcmQGJmSVfyZk7R3SngI4JKfwDJ2+05zIg8gbiereTZRHhJ5KCMOwDFLjhoBTn2g0ghagfKeIYJDPFyibJVBtTREwq60SpYvh5++PpwatHsxSm9QRLSQpEVSd7/TYJUb49TX7gztpjjEffnoVw66+Ytovs14Yp7HaKmUXeX9rKUoMoLNW3srqI5fWn8JejrVkK0QcrkFLOgS39yoKUQe292WJ1guUHG8K2o8K00oO1BTvXoW4yasclUTgZYJY9aFNfAThX5CZRmczAV52oAPoupHhWRIUUAOoyUIlYVaAa/VbLbyiZUiyFbjQFNwiZQSGl4IDy9sO5Wrty0QLKhdZPxmgGcDo8ejn+c/6eiK9poz15Kw7Dr/vN/z6W7q++091/AQYA5mZ8GYJ9K0AAAAAASUVORK5CYII=') left top no-repeat;\r\npadding-left : 24px;\r\n}\r\n");
          content.append(".up\r\n {\r\nbackground :\r\n url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAmlJREFUeNpsU0toU0EUPfPysx/tTxuDH9SCWhUDooIbd7oRUUTMouqi2iIoCO6lceHWhegy4EJFinWjrlQUpVm0IIoFpVDEIthm0dpikpf3ZuZ6Z94nrXhhMjM3c8895977BBHB2PznK8WPtDgyWH5q77cPH8PpdXuhpQT4ifR9u5sfJb1bmw6VivahATDrxcRZ2njfoaMv+2j7mLDn93MPiNRMvGbL18L9IpF8h9/TN+EYkMffSiOXJ5+hkD+PdqcLpICWHOHc2CC+LEyA/K+cKQMnlQHJX8wqYG3MAJy88Wa4OLDvEqAEOpJd0LxHIMdHBziowSwVlF8D6QaicK01krw/JynwcKoEwZczewroTvZirlKJs5CqQ5CG8pb57FnJUA0LYCXMX5fibd+p8LWDDemcPZbzQyjvH+Ki1TlIciElA7ghwLKV4kRZstt2sANWRjYTAGzuP2hXZFpJ/GsxgGJ0ox1aoFWsDXyyxqCs26+ydmagFN/rRjymJ1898bzGzmQE0HCZpmk5A0RFIv8Pn0WYPsiu6t/Rsj6PauVTwffTSzGAGZhUG2F06hEc9ibS7OPMNp6ErYFlKavo7MkhmTqCxZ/jwzGA9Hx82H2BZSw1NTN9Gx8ycHkajU/7M+jInsDC7DiaEmo1bNl1AMr9ASFgqVu9MCTIzoGUimXVAnnaN0PdBBDCCYbEtMk6wkpQwIG0sn0PQIUF4GsTwLSIFKNqF6DVrQq+IWVrQDxAYQC/1SsYOI4pOxKZrfifiUSbDUisif7XlpGIPufXd/uvdvZm760M0no1FZcnrzUdjw7au3vu/BVgAFLXeuTxhTXVAAAAAElFTkSuQmCC') left top no-repeat;\r\npadding-left : 24px;\r\n}\r\n");
          content.append("</style>\r\n");
          content.append("</head><body>");
          if (!url.equals("/")) {
            //add up link
            int upidx = url.substring(0, url.length() - 1).lastIndexOf("/");
            String up = url.substring(0, upidx+1);
            content.append("<a class='up' href='" + up + "'>[parent folder]</a><br>\r\n");
          }
          for(int a=0;a<lns.length;a++) {
            String f[] = lns[a].split(" ", -1);  //drwxrwxrwx ? uid gid size month day time_or_year filename
            int last = f.length - 1;
            if (f[0].charAt(0) == 'd') {
              //folder
              content.append("<a class='folder' href='" + f[last] + "/'>[" + f[last] + "]</a><br>\r\n");
            } else {
              //file
              content.append("<a class='file' href='" + f[last] + "'>" + f[last] + "</a><br>\r\n");
            }
          }
          content.append("</body></html>");
          int code = 200;
          String msg = "OK";
          String headers = "HTTP/1.1 " + code + " " + msg + "\r\nContent-Type: text/html\r\nContent-Length: " + content.length() + "\r\n\r\n";
          pos.write(headers.getBytes());
          pos.write(content.toString().getBytes());
          pos.flush();
        } else {
          //download file
          int code = 200;
          String msg = "OK";
          String headers = "HTTP/1.0 " + code + " " + msg + "\r\n\r\n";
          disconn = true;
          pos.write(headers.getBytes());
          ftp.get(url, pos);
          pos.flush();
        }
      } catch (Exception e) {
        replyError(505, "Exception:" + e);
        log(e);
      }
    }
    private void replyError(int code, String msg) throws Exception {
      log("Error:" + code);
      String content = "<h1>Error : " + code + " : " + msg + "</h1>";
      String headers = "HTTP/1.1 " + code + " " + msg + "\r\nContent-Length: " + content.length() + "\r\n\r\n";
      pos.write(headers.getBytes());
      pos.write(content.getBytes());
      pos.flush();
    }
    private void sendRequest(String ln[]) throws Exception {
      String req = "";
      for(int a=0;a<ln.length;a++) {
        if (a == 0) ln[a] = removeHost(ln[a]);
        req += ln[a];
        req += "\r\n";
      }
      req += "\r\n";
      ios.write(req.getBytes());
      ios.flush();
    }
    private void sendPost(String ln[]) throws Exception {
      int length = -1;
      for(int a=0;a<ln.length;a++) {
        if (ln[a].regionMatches(true, 0, "Content-Length: ", 0, 16)) {
          length = Integer.valueOf(ln[a].substring(16, ln[a].length()));
        }
      }
      if (length == -1) throw new Exception("unknown post size");
      log("sendPost data len=" + length);
      byte post[] = JF.readAll(pis, length);
      ios.write(post);
      ios.flush();
    }
    private void relayReply(String fn) throws Exception {
      log("relayReply:" + fn);
      String tmp[];
      String line = "";
      String headers = "";
      int length = -1;
      int contentLength = -1;
      int ch;
      boolean first = true;
      int code;
      String encoding = "";
      do {
        ch = iis.read();
        if (ch == -1) throw new Exception("read error");
        line += (char)ch;
        if (!line.endsWith("\r\n")) continue;
        if (line.regionMatches(true, 0, "Content-Length: ", 0, 16)) {
          length = Integer.valueOf(line.substring(16, line.length() - 2));
          contentLength = length;
        }
        if (line.regionMatches(true, 0, "Connection: Close", 0, 17)) {
          disconn = true;
        }
        if (line.regionMatches(true, 0, "Transfer-Encoding:", 0, 18)) {
          encoding = line.substring(18).trim().toLowerCase();
        }
        if (first == true) {
          //HTTP/1.1 CODE MSG
          if (line.startsWith("HTTP/1.0")) disconn = true;
          tmp = line.split(" ");
          code = Integer.valueOf(tmp[1]);
          log("reply=" + code + ":" + line);
          first = false;
        }
        headers += line;
        if (line.length() == 2) break;  //blank line (double enter)
        line = "";
      } while (true);
      pos.write(headers.getBytes());
      pos.flush();
      if (length == 0) {
        log("reply:done:content.length=0:headers.length=" + headers.length());
        return;
      }
      if (length == -1) {
        if (encoding.equals("chunked")) {
          //read chunked format
          contentLength = 0;
          while (true) {
            //read chunk size followed by \r\n
            String chunkSize = "";
            while (true) {
              ch = iis.read();
              if (ch == -1) throw new Exception("read error");
              chunkSize += (char)ch;
              if (chunkSize.endsWith("\r\n")) break;
            }
            contentLength += chunkSize.length();
            int idx = chunkSize.indexOf(";");  //ignore extensions
            if (idx == -1) idx = chunkSize.length() - 2;
            int chunkLength = Integer.valueOf(chunkSize.substring(0, idx), 16);
            pos.write(chunkSize.getBytes());
            boolean zero = chunkLength == 0;
            //read chunk
            chunkLength += 2;  // \r\n
            contentLength += chunkLength;
            int read;
            int bufsiz = chunkLength;
            if (bufsiz > 4096) bufsiz = 4096;
            byte buf[] = new byte[bufsiz];
            while (chunkLength != 0) {
              read = iis.read(buf, 0, chunkLength <= 4096 ? chunkLength : bufsiz);
              if (read == -1) throw new Exception("read error");
              if (read > 0) {
                chunkLength -= read;
                pos.write(buf, 0, read);
              }
            }
            pos.flush();
            if (zero) break;
          }
        } else {
          contentLength = 0;
          //read until disconnected (HTTP/1.0)
          int read;
          byte buf[] = new byte[64 * 1024];
          while (true) {
            read = iis.read(buf, 0, 64 * 1024);
            if (read == -1) break;
            if (read > 0) {
              contentLength += read;
              pos.write(buf, 0, read);
              pos.flush();
            }
          }
        }
      } else {
        //read content (length known)
        int read, off = 0;
        byte buf[] = new byte[length];
        while (length != 0) {
          read = iis.read(buf, off, length);
          if (read == -1) break;
          if (read > 0) {
            length -= read;
            off += read;
          }
        }
        pos.write(buf);
        pos.flush();
      }
      log("reply:done:content.length=" + contentLength + ":headers.length=" + headers.length());
    }
    private void connectCommand(String host, String req) throws Exception {
      String ln[] = req.split(" ");
      if (ln.length != 3) {
        replyError(505, "Bad CONNECT syntax");
        return;
      }
      int portidx = ln[1].indexOf(':');
      if (portidx != -1) {
        int port = Integer.valueOf(ln[1].substring(portidx+1));
        if (port != 443) {
          replyError(505, "CONNECT is for port 443 only");
          return;
        }
      }
      if (filtersecure) {
        connect("localhost", getSSLPort(host));
      } else {
        connect(host, 443);
      }
      pos.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      pos.flush();
      ConnectRelay i2p = new ConnectRelay(iis, pos);
      ConnectRelay p2i = new ConnectRelay(pis, ios);
      i2p.start();
      p2i.start();
      i2p.join();
      p2i.join();
      disconn = true;  //not HTTP/1.1 compatible?
    }
    private String removeHost(String req) throws Exception {
      //GET URL HTTP/1.1
      //remove host from URL if present
      String p[] = req.split(" ");
      if (p.length != 3) return req;
      String urlstr = p[1];
      if ((!urlstr.startsWith("http:")) && (!urlstr.startsWith("https:"))) return req;
      URL url = new URI(urlstr).toURL();
      return p[0] + " " + url.getFile() + " " + p[2];
    }
    private class ConnectRelay extends Thread {
      private InputStream is;
      private OutputStream os;
      private byte buf[] = new byte[4096];
      private final int buflen = 4096;
      public ConnectRelay(InputStream is, OutputStream os) {
        this.is = is;
        this.os = os;
      }
      public void run() {
        int read;
        try {
          while (true) {
            read = is.read(buf, 0, buflen);
            if (read == -1) break;
            if (read > 0) {os.write(buf, 0, read); os.flush();}
          }
        } catch (Exception e) {}
      }
    }
  }

  private static JBusServer busServer;
  private JBusClient busClient;
  private String config;

  public static class JBusMethods {
    public void getConfig(String pack) {
      proxy.busClient.call(pack, "getConfig", proxy.busClient.quote(proxy.busClient.encodeString(proxy.config)));
    }
    public void setConfig(String cfg) {
      //write new file
      try {
        FileOutputStream fos = new FileOutputStream(getConfigFile());
        fos.write(JBusClient.decodeString(cfg).getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    public void restart() {
      proxy.close();
      proxy = new ProxyServer();
      proxy.start();
    }
  }

  public static int getBusPort() {
    if (JF.isWindows()) {
      return 33003;
    } else {
      return 777;
    }
  }

  public static void main(String args[]) {
  }

  //Win32 Service

  private static ProxyServer proxy;

  public static void serviceStart(String args[]) {
    if (JF.isWindows()) {
      busServer = new JBusServer(getBusPort());
      busServer.start();
      while (!busServer.ready) {
        JF.sleep(10);
      }
    }
    proxy = new ProxyServer();
    proxy.start();
  }

  public static void serviceStop() {
    JFLog.log("Proxy : Stopping service");
    if (busServer != null) {
      busServer.close();
      busServer = null;
    }
    proxy.close();
  }
}
