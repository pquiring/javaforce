package javaforce;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

/**
 * POP3 client class.
 */
public class POP3 {

  private Socket s;
  private InputStream is;
  private OutputStream os;
  private BufferedReader br;
  private String host;
  private boolean log = false;

  public boolean debug = false;
  /**
   * Holds the response strings from the last executed command
   */
  public String response;

  public static final int PORT = 25;  //default port
  public static final int TLSPORT = 587;  //default SSL port (explicit)
  public static final int SSLPORT = 465;  //default SSL port (implicit)

  /** Connects to an insecure POP3 server.
   * @param host = POP3 server
   * @param port = port to connect (default = 25 or 587)
   */
  public boolean connect(String host, int port) throws Exception {
    s = new Socket(host, port);
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    getResponse();
    if (response.startsWith("+OK")) {
      return true;
    }
    disconnect();  //not valid POP3 site
    return false;
  }

  /** Connects to a secure POP3 server.
   * @param host = POP3 server
   * @param port = port to connect (default = 465)
   */
  public boolean connectSSL(String host, int port) throws Exception {
    TrustManager[] trustAllCerts = new TrustManager[]{
      new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
        }
      }
    };
    // Let us create the factory where we can set some parameters for the connection
    SSLContext sc = SSLContext.getInstance("SSL");
    sc.init(null, trustAllCerts, new java.security.SecureRandom());
//      SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();  //this method will only work with trusted certs
    SSLSocketFactory sslsocketfactory = (SSLSocketFactory) sc.getSocketFactory();  //this method will work with untrusted certs
    SSLSocket ssl = (SSLSocket) sslsocketfactory.createSocket(host, port);
    s = (Socket) ssl;
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    getResponse();
    if (response.startsWith("+OK")) {
      return true;
    }
    disconnect();  //not valid POP3 site
    return false;
  }

  /** Disconnect from POP3 Server. */
  public void disconnect() throws Exception {
    if (s != null) {
      s.close();
    }
    s = null;
    is = null;
    os = null;
    br = null;
  }

  /** Set debug logging state. */
  public void setLogging(boolean state) {
    log = state;
  }

  /** Sends HELO command. */
  public boolean login() throws Exception {
    cmd("HELO " + host);
    getResponse();
    if (!response.startsWith("+OK")) {
      return false;
    }
    return true;
  }

  public static final String AUTH_LOGIN = "LOGIN";  //legacy
  public static final String AUTH_APOP = "APOP";

  /** Authenticates with POP3 Server using APOP.
   * @param user = username
   * @param pass = password
   * @param type = AUTH_LOGIN or AUTH_APOP
   */
  public boolean auth(String user, String pass, String type) throws Exception {
    switch (type) {
      case AUTH_LOGIN: return user_pass(user, pass);
      case AUTH_APOP: return apop(user, pass);
      default: JFLog.log("POP3:Unknown auth type:" + type);
    }
    return false;
  }

  private boolean user_pass(String user, String pass) throws Exception {
    cmd("USER " + user);
    getResponse();
    if (!response.startsWith("+OK")) {
      return false;
    }
    cmd("PASS " + pass);
    getResponse();
    if (!response.startsWith("+OK")) {
      return false;
    }
    return true;
  }

  private boolean apop(String user, String pass) throws Exception {
    MD5 md5 = new MD5();
    md5.add(pass);
    String pass_md5 = md5.toString();
    cmd("APOP " + user + " " + pass_md5);
    getResponse();
    if (!response.startsWith("+OK")) {
      return false;
    }
    return true;
  }

  /** Authenticates with POP3 Server using AUTH_LOGIN type.
   * @param user = username
   * @param pass = password
   */
  public boolean auth(String user, String pass) throws Exception {
    return auth(user, pass, AUTH_LOGIN);
  }

  /** Send QUIT command. */
  public void logout() throws Exception {
    cmd("quit");
    getResponse();  //should be "+OK" but ignored
  }

  /** Send any command to server. */
  public void cmd(String cmd) throws Exception {
    if ((s == null) || (s.isClosed())) {
      throw new Exception("not connected");
    }
    if (log || debug) {
      if (cmd.startsWith("pass ")) {
        JFLog.log("pass ****");
      } else {
        JFLog.log(cmd);
      }
    }
    cmd += "\r\n";
    os.write(cmd.getBytes());
  }

  /** Secures connection using STARTTLS command. */
  public void starttls() throws Exception {
    cmd("STARTTLS");
    getResponse();
    if (!response.startsWith("+OK")) {
      throw new Exception("STARTTLS failed!");
    }
    s = JF.connectSSL(s);
    is = s.getInputStream();
    os = s.getOutputStream();
  }

  public static class Message {
    public int idx;
    public long size;
  }

  private static final int bufsiz = 1500 - 20 - 20;

  /** List messages available. */
  public Message[] list() throws Exception {
    cmd("LIST");
    getResponse();
    if (!response.startsWith("+OK")) {
      throw new Exception("LIST failed!");
    }
    ArrayList<Message> list = new ArrayList<>();
    while (true) {
      String ln = br.readLine();  //# ###
      if (ln.equals(".")) break;
      String[] p = ln.split(" ", 2);
      int idx = Integer.valueOf(p[0]);
      int size = Integer.valueOf(p[1]);
      Message msg = new Message();
      msg.idx = idx;
      msg.size = size;
      list.add(msg);
    }
    return list.toArray(new Message[0]);
  }

  /** Get message */
  public byte[] get(int idx) throws Exception {
    cmd("RETR " + idx);
    getResponse();
    if (!response.startsWith("+OK")) {
      throw new Exception("RETR failed!");
    }
    String[] p = response.split(" ", 3);
    int length = Integer.valueOf(p[1]);
    StringBuilder sb = new StringBuilder();
    int total = 0;
    while (total < length) {
      String ln = br.readLine();
      sb.append(ln);
      total += ln.length();
      sb.append("\r\n");
      total += 2;
    }
    getResponse();  //"."
    return sb.toString().getBytes();
  }

  /** Get message */
  public void get(int idx, OutputStream os) throws Exception {
    cmd("RETR " + idx);
    getResponse();
    if (!response.startsWith("+OK")) {
      throw new Exception("RETR failed!");
    }
    String[] p = response.split(" ", 3);
    int length = Integer.valueOf(p[1]);
    int total = 0;
    while (total < length) {
      String ln = br.readLine();
      os.write(ln.getBytes());
      total += ln.length();
      os.write("\r\n".getBytes());
      total += 2;
    }
    getResponse();  //"."
  }

  /** Delete message on server. */
  public boolean delete(int idx) throws Exception {
    cmd("DELE " + idx);
    getResponse();
    if (!response.startsWith("+OK")) {
      throw new Exception("DELE failed!");
    }
    return true;
  }

  private void getResponse() throws Exception {
    if (debug) JFLog.log("POP3:reading response...");
    response = br.readLine();
    if (debug) JFLog.log("POP3:response=" + response);
  }
}
