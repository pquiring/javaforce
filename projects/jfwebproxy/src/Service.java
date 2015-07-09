import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.*;
import javaforce.JFLog;

public class Service extends Thread {

  public static final String version = "1.5.0";

  public Service(int port, String proxyHost, Logger logger, boolean alwaysSecure, boolean neverSecure, String ext) {
    this.proxyHost = proxyHost;
    this.logger = logger;
    this.alwaysSecure = alwaysSecure;
    this.neverSecure = neverSecure;
    this.ext = ext;
    if (port != -1) this.port = port;
    new File(getUserPath() + "/.jfwebproxy").mkdir();
    deleteKeys();
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream("localhost.key");
      FileOutputStream fos = new FileOutputStream(getUserPath() + "/.jfwebproxy/localhost.key");
      copyAll(is, fos);
      fos.close();
    } catch (Exception e) {
      logger.log(e);
    }
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
    }, getUserPath() + "/.jfwebproxy");
    initSSL();
  }

  //instance data
  private int port = 8080;

  //static shared data
  private static String proxyHost;
  private static boolean alwaysSecure = false;
  private static boolean neverSecure = false;
  private static Logger logger;
  private static int nextSSLport = 8081;
  private static int refID = 0;
  private static synchronized int getRefID() {
    return refID++;
  }
  private static String ext;

  public static String getUserPath() {
    return System.getProperty("user.home");
  }

  public static boolean copyAll(InputStream is, OutputStream os) {
    try {
      int len = is.available();
      byte buf[] = new byte[1024];
      int copied = 0;
      while (copied < len) {
        int read = is.read(buf);
        if (read == 0) {
          continue;
        }
        if (read == -1) {
          return false;
        }
        os.write(buf, 0, read);
        copied += read;
      }
      return true;
    } catch (Exception e) {
      logger.log(e);
      return false;
    }
  }

  private void log(String msg) {
    logger.log(msg);
  }
  private void log(Throwable t) {
    logger.log(t);
  }

  public void deleteKeys() {
    //delete all temp files in ~/.jfwebproxy
    File files[] = new File(getUserPath() + "/.jfwebproxy").listFiles();
    if (files != null) {
      for(int a=0;a<files.length;a++) {
        files[a].delete();
      }
    }
  }

  public void close() {
    try {
      logger.log("Closing port:" + port);
      ss.close();
      SecureSite ssList[] = secureSites.values().toArray(new SecureSite[0]);
      for(int a=0;a<ssList.length;a++) {
        ssList[a].close();
      }
      secureSites.clear();
      deleteKeys();
    } catch (Exception e) {}
    //close list
    Session sess;
    while (list.size() > 0) {
      sess = list.get(0);
      sess.close();
    }
  }

  /** Checks if system is Windows only. */
  public static boolean isWindows() {
    return (File.separatorChar == '\\');
  }

  /** Executes keytool directly */
  public static boolean keytool(String args[], String path) {
    ArrayList<String> cmd = new ArrayList<String>();
    try {
//      sun.security.tools.KeyTool.main(args);  //no longer available in Java 8
      if (isWindows()) {
        cmd.add(System.getProperty("java.home") + "\\bin\\keytool.exe");
      } else {
        cmd.add(System.getProperty("java.home") + "/bin/keytool");
      }
      for(int a=0;a<args.length;a++) {
        cmd.add(args[a]);
      }
      Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), null, new File(path));
      p.waitFor();
      return true;
    } catch (Exception e) {
      logger.log(e);
      return false;
    }
  }

  private static ServerSocket ss;
  private static Vector<Session> list = new Vector<Session>();
  private static HashMap<String, SecureSite> secureSites = new HashMap<String,SecureSite>();

  private static SSLContext gsc;
  private static SSLSocketFactory socketFactory;

  public static void initSSL() {
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
      gsc = SSLContext.getInstance("SSL");

      KeyStore ks = KeyStore.getInstance("JKS");
      ks.load(new FileInputStream(getUserPath() + "/.jfwebproxy/localhost.key"), "password".toCharArray());
      KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
      kmf.init(ks, "password".toCharArray());

      gsc.init(kmf.getKeyManagers(), trustAllCerts, new java.security.SecureRandom());
      socketFactory = (SSLSocketFactory) gsc.getSocketFactory();  //this method will work with untrusted certs
    } catch (Exception e) {
      e.printStackTrace();
    }
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
      }, getUserPath() + "/.jfwebproxy");
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
      }, getUserPath() + "/.jfwebproxy");
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
      }, getUserPath() + "/.jfwebproxy");
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
      }, getUserPath() + "/.jfwebproxy");
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
      }, getUserPath() + "/.jfwebproxy");
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
        ks.load(new FileInputStream(getUserPath() + "/.jfwebproxy/" + domain + ".key"), "password".toCharArray());
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
        logger.log(e);
      }
    }
    public void close() {
      try {
        logger.log("Closing port:" + port);
        ss.close();
      } catch (Exception e) {
        logger.log(e);
      }
    }
  }

  public void run() {
    Socket s;
    Session sess;
    try {
      ss = new ServerSocket(port);
      while (!ss.isClosed()) {
        s = ss.accept();
        sess = new Session(s, false);
        sess.start();
      }
    } catch (Exception e) {
      log(e);
    }
  }

  public static class Session extends Thread {
    private Socket p, i;  //proxy client, internet
    private InputStream pis, iis;
    private OutputStream pos, ios;
    private byte buf[] = new byte[4096];
    private final int bufsiz = 4096;
    private int refID = -1;
    private String httpver;  //"1.0" or "1.1"
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
    public void run() {
      refID = getRefID();
      StringBuilder req;
      int ch;
      list.add(this);
      httpver = "1.1";  //unless 1.0
      try {
        pis = p.getInputStream();
        pos = p.getOutputStream();
        do {
          log("reading request");
          req = new StringBuilder();
          do {
            ch = pis.read();
            if (ch == -1) {close(); return;}
            req.append((char)ch);
          } while (!req.toString().endsWith("\r\n\r\n") && p.isConnected());
          proxy(req.toString());
        } while (httpver.equals("1.1"));
        p.close();
      } catch (Exception e) {}
      close();
    }
    private void proxy(String req) throws Exception {
      String headers[] = req.split("\r\n");
      int hostidx = -1;
      if (headers[0].endsWith("1.0")) httpver = "1.0";
      for(int a=0;a<headers.length;a++) {
        if (headers[a].regionMatches(true, 0, "Host: ", 0, 6)) hostidx = a;
      }
      try {
        log("request=" + headers[0] + ":port=" + p.getPort());
        if (headers[0].regionMatches(true, 0, "POST ", 0, 5)) {
          if (hostidx == -1) {
            log("ERROR : No host specified : " + req);
            sendError(505, "No host specified");
            return;
          }
          sendRequest(req, headers, getPost(req));
          return;
        }
        if (headers[0].regionMatches(true, 0, "GET ", 0, 4)) {
          if (hostidx == -1) {
            log("ERROR : No host specified : " + req);
            sendError(505, "No host specified");
            return;
          }
          sendRequest(req, headers, null);
          return;
        }
        if (headers[0].regionMatches(true, 0, "CONNECT ", 0, 8)) {
          log("CONNECT");
          if (secure) {
            sendError(505, "Already secure?");
            return;
          }
          connectCommand(headers[0]);
          return;
        }
        sendError(505, "Unknown request");
      }
      catch (IOException ioe) {
        /*do nothing*/
        log(ioe);
      }
      catch (Exception e) {
        sendError(505, "Exception:" + e); e.printStackTrace(new PrintStream(pos));
        log(e);
      }
    }
    private void sendError(int code, String msg) throws Exception {
      String str = "HTTP/" + httpver + " " + code + " " + msg + "\r\n\r\n" + "<h1>Error : " + code + " : " + msg + "</h1>";
      pos.write(str.getBytes());
      pos.flush();
    }
    private String getPost(String req) throws Exception {
      String ln[] = req.split("\r\n");
      int length = -1;
      for(int a=0;a<ln.length;a++) {
        if (ln[a].regionMatches(true, 0, "Content-Length: ", 0, 16)) {
          length = Integer.valueOf(ln[a].substring(16));
          break;
        }
      }
      if (length == -1) {
        log("error:post length unknown");
        return null;
      }
      byte post[] = new byte[length];
      int pos = 0;
      while (pos != length) {
        int read = pis.read(post, pos, length - pos);
        if (read == -1) {
          log("error:post read data failed");
          return null;
        }
        pos += read;
      }
      return new String(post,0,length);
    }
    private void sendRequest(String proxyreq, String headers[], String proxypost) throws Exception {
      StringBuilder post = new StringBuilder();
      post.append("headers=" + URLEncoder.encode(proxyreq, "UTF-8"));
      post.append("&secure=" + secure);
      if (proxypost != null) {
        post.append("&post=" + URLEncoder.encode(proxypost, "UTF-8"));
      } else {
        post.append("&post=");  //avoid errors on server-side
      }
      if (alwaysSecure || (secure && !neverSecure)) {
        i = socketFactory.createSocket(proxyHost, 443);
      } else {
        i = new Socket(proxyHost, 80);
      }
      ios = i.getOutputStream();
      StringBuilder request = new StringBuilder();
      request.append("POST /redir2." + ext + " HTTP/1.0\r\n");
      request.append("Host: " + proxyHost + "\r\n");
      request.append("Content-Length:" + post.length() + "\r\n");
      request.append("Content-Type: application/x-www-form-urlencoded\r\n");
      request.append("\r\n");
      ios.write(request.toString().getBytes());
      ios.write(post.toString().getBytes());
      iis = i.getInputStream();
      //relayReply
      int read;
//      FileOutputStream fos = new FileOutputStream("test.dat");
      int total = 0;
      byte pheaders[] = new byte[0];  //proxy headers (ignored)
      boolean header = false;
      do {
        read = iis.read(buf, 0, bufsiz);
        if (read == -1) {
          break;
        }
        if (read > 0) {
          if (!header) {
            int oldLength = pheaders.length;
            pheaders = Arrays.copyOf(pheaders, pheaders.length + read);
            System.arraycopy(buf, 0, pheaders, oldLength, read);
            if (pheaders.length > 4) {
              int len = pheaders.length-4;
              for(int a=0;a<=len;a++) {
                if (pheaders[a] == '\r' && pheaders[a+1] == '\n' && pheaders[a+2] == '\r' && pheaders[a+3] == '\n') {
                  header = true;
                  a+=4;
                  if (a < pheaders.length) {
                    pos.write(pheaders, a, pheaders.length-a);
//                    fos.write(pheaders, a, pheaders.length-a);
                  }
                  pheaders = null;
                  break;
                }
              }
            }
          } else {
            pos.write(buf, 0, read);
//          fos.write(buf, 0, read);
          }
          total += read;
        }
      } while (true);
      log("size=" + total);
//      fos.close();
      pos.flush();
    }
    private void connectCommand(String reqln0) throws Exception {
      String ln[] = reqln0.split(" ");
      if (ln.length != 3) {
        sendError(505, "Bad CONNECT syntax");
        return;
      }
      String host = ln[1];
      int portidx = ln[1].indexOf(':');
      if (portidx != -1) {
        int port = Integer.valueOf(ln[1].substring(portidx+1));
        if (port != 443) {
          sendError(505, "CONNECT is for port 443 only");
          return;
        }
        host = ln[1].substring(0, portidx);
      }
      int sslport = getSSLPort(host);
      i = new Socket("localhost", sslport);  //connecting to SECURE localport
      iis = i.getInputStream();
      ios = i.getOutputStream();
      pos.write(("HTTP/" + httpver + " 200 Connection established\r\n\r\n").getBytes());
      pos.flush();
      ConnectRelay i2p = new ConnectRelay(iis, pos, "i2p", p);
      ConnectRelay p2i = new ConnectRelay(pis, ios, "p2i", i);
      i2p.start();
      p2i.start();
      i2p.join();
      p2i.join();
      httpver = "1.0";  //discard socket
    }
    private void log(String msg) {
      logger.log("[" + refID + (secure ? ":S" : "") + "]" + msg);
    }
    private void log(Throwable t) {
      logger.log("[" + refID + (secure ? ":S" : "") + "] Exception...");
      logger.log(t);
    }
    private class ConnectRelay extends Thread {
      private InputStream is;
      private OutputStream os;
      private byte buf[] = new byte[4096];
      private final int buflen = 4096;
      private String name;
      private Socket other;
      public ConnectRelay(InputStream is, OutputStream os, String name, Socket other) {
        this.is = is;
        this.os = os;
        this.name = name;
        this.other = other;
      }
      public void run() {
        int read;
        try {
//          log("ConnectRelay start:" + name);
          while (true) {
            read = is.read(buf, 0, buflen);
//            log("read:" + name + "=" + read);
            if (read == -1) break;
            if (read > 0) {os.write(buf, 0, read); os.flush();}
//            log("write:" + name + "=" + read);
          }
//          log("ConnectRelay stop:" + name);
        } catch (Exception e) {
//          log("ConnectRelay exception:" + name);
//          log(e);
        }
        try { other.close(); } catch (Exception e) {}
      }
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
      logger.log("Creating new SSL site:" + host);
      secureSite = new SecureSite(host, nextSSLport++);
      secureSites.put(host, secureSite);
    }
    synchronized(secureSite.lock) {
      secureSite.start();
      try {secureSite.lock.wait();} catch (Exception e) {
        logger.log(e);
      }
    }
    return secureSite.port;
  }

  public static void initHttps() {
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
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
      HttpsURLConnection.setDefaultSSLSocketFactory(sslsocketfactory);
    } catch (Exception e) {
      JFLog.log(e);
    }
    //trust any hostname
    HostnameVerifier hv = new HostnameVerifier() {
      public boolean verify(String urlHostName, SSLSession session) {
        if (!urlHostName.equalsIgnoreCase(session.getPeerHost())) {
          System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
        }
        return true;
      }
    };
    HttpsURLConnection.setDefaultHostnameVerifier(hv);
  }
}
