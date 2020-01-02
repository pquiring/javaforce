package javaforce;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;

/**
 * SMTP client class.
 */
public class SMTP {

  public SMTP() {
  }
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private BufferedReader br;
  private boolean passive = true;
  private String host;
  private ServerSocket active;  //active socket
  private boolean log = false;
  public boolean debug = false;
  /**
   * Holds the repsonse strings from the last executed command
   */
  public String response[];

  public static final int PORT = 25;  //default port
  public static final int SSLPORT = 465;  //default SSL port

  public boolean connect(String host, int port) throws Exception {
    s = new Socket(host, port);
    is = s.getInputStream();
    br = new BufferedReader(new InputStreamReader(is));
    os = s.getOutputStream();
    this.host = host;
    getResponse();
    if (response[response.length - 1].startsWith("220")) {
      return true;
    }
    disconnect();  //not valid SMTP site
    return false;
  }

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
    if (response[response.length - 1].startsWith("220")) {
      return true;
    }
    disconnect();  //not valid SMTP site
    return false;
  }

  public void disconnect() throws Exception {
    if (s != null) {
      s.close();
    }
    s = null;
    is = null;
    os = null;
  }

  public void setLogging(boolean state) {
    log = state;
  }

  public boolean login() throws Exception {
    cmd("HELO " + host);
    getResponse();
    if (!response[response.length - 1].startsWith("250")) {
      return false;
    }
    return true;
  }

  public boolean auth(String user, String pass) throws Exception {
    cmd("AUTH LOGIN");
    getResponse();
    cmd(encode(user));
    getResponse();
    cmd(encode(pass));
    getResponse();
    if (!response[response.length - 1].startsWith("235")) {
      return false;
    }
    return true;
  }

  public void logout() throws Exception {
    cmd("quit");
    getResponse();  //should be "221" but ignored
  }

  public void cmd(String cmd) throws Exception {
    if ((s == null) || (s.isClosed())) {
      throw new Exception("not connected");
    }
    if (log) {
      if (cmd.startsWith("pass ")) {
        JFLog.log("pass ****");
      } else {
        JFLog.log(cmd);
      }
    }
    cmd += "\r\n";
    os.write(cmd.getBytes());
  }

  public void from(String email) throws Exception {
    cmd("MAIL FROM:<" + email + ">");
    getResponse();
  }

  public void to(String email) throws Exception {
    cmd("RCPT TO:<" + email + ">");
    getResponse();
  }

  public boolean data(String msg) throws Exception {
    /*
     sample data:
     From: "First Last" <bob@example.com>
     To: "First Last" <to@example.com>
     Cc: "First Last" <cc@example.com>
     Date: Tue, 15 Jan 2008 16:02:43 -0500
     Subject: Subject line

     Hello Bob,
     blah blah blah
     make sure you don't have . (period) on a line by itself since that ends the message.
     */
    cmd("DATA");
    getResponse();
    if (!response[response.length - 1].startsWith("354")) {
      return false;
    }
    os.write(msg.getBytes());
    os.write("\r\n.\r\n".getBytes());
    getResponse();
    if (!response[response.length - 1].startsWith("250")) {
      return false;
    }
    return true;
  }

  private void getResponse() throws Exception {
    ArrayList<String> tmp = new ArrayList<String>();
    String str;
    while (!s.isClosed()) {
      str = br.readLine();
      tmp.add(str);
      if (str.charAt(3) == ' ') {
        break;
      }
    }
    int size = tmp.size();
    response = new String[size];
    for (int a = 0; a < size; a++) {
      response[a] = tmp.get(a);
      if (debug) {
        System.out.println(response[a]);
      }
    }
  }

  public String getLastResponse() {
    if (response == null) return null;
    return response[response.length - 1];
  }

  private static String extractEmail(String in) {
    int i1 = in.indexOf('<');
    int i2 = in.indexOf('>');
    return in.substring(i1+1, i2);
  }

  public static void main(String args[]) {
    if (args.length < 2) {
      System.out.println("Usage:SMTP server[:port] msg.txt [user pass]");
      System.out.println("msg.txt sample:\r\n");
      System.out.println("From: \"First Last\" <bob@example.com>");
      System.out.println("To: \"First Last\" <to@example.com>");
      System.out.println("Cc: \"First Last\" <cc@example.com>");
      System.out.println("Date: Tue, 15 Jan 2008 16:02:43 -0500");
      System.out.println("Subject: Subject line\r\n");
      System.out.println("Hello Bob, ...");
      return;
    }
    SMTP smtp = new SMTP();
    try {
      String txt = new String(JF.readAll(new FileInputStream(args[1])));
      txt = txt.replaceAll("\r", "");
      String lns[] = txt.split("\n");
      if (lns.length < 4) throw new Exception("file too short");
      smtp.setLogging(true);
      int port = 25;
      String host = args[0];
      int idx = host.indexOf(':');
      if (idx != -1) {
        port = JF.atoi(host.substring(idx+1));
        host = host.substring(0, idx);
      }
      if (port == 25 || port == 587) {
        smtp.connect(host, port);
      } else {
        //465
        smtp.connectSSL(host, port);
      }
      smtp.login();
      if (args.length == 4) {
        if (!smtp.auth(args[2], args[3])) {
          throw new Exception("Login failed!");
        }
      }
      smtp.from(extractEmail(lns[0]));
      for(int a=1;a<lns.length;a++) {
        String ln = lns[a].toLowerCase();
        if (ln.startsWith("to:") || ln.startsWith("cc:") || ln.startsWith("bcc:")) {
          smtp.to(extractEmail(lns[a]));
        } else {
          break;
        }
      }
      smtp.data(txt);
      System.out.println("Reply=" + smtp.getLastResponse());
      smtp.disconnect();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public static String encode(String in) {
    return new String(Base64.encode(in.getBytes()));
  }
}
