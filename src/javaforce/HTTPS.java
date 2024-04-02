package javaforce;

/** HTTPS Client.
 *
 * @author pquiring
 */

public class HTTPS extends HTTP {
  static {
    JF.initHttps();
  }
  public boolean open(String host) {
    return open(host, 443);
  }
  public boolean open(String host, int port) {
    this.host = host;
    this.port = port;
    try {
      if (!super.open(host, port)) {
        throw new Exception("Connect failed");
      }
      s = JF.connectSSL(s);
      if (s == null) {
        throw new Exception("SSL Upgrade failed");
      }
      os = s.getOutputStream();
      is = s.getInputStream();
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
    return true;
  }
  /** Test HTTPS */
  public static void main(String[] args) {
    HTTPS http = new HTTPS();
    String html;
    boolean print = false;
    if (args.length > 0 && args[0].equals("print")) print = true;

    HTTP.debug = true;

    http.open("google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTPS.get() failed");
      return;
    }
    if (print) System.out.println(html);

    http.open("www.google.com");
    html = http.getString("/");
    http.close();
    if (html == null || html.length() == 0) {
      System.out.println("Error:HTTPS.get() failed");
      return;
    }
    if (print) System.out.println(html);
  }
  /** Removes user info from HTTP URL. */
  public static String cleanURL(String url) {
    //need to remove user:pass from url
    //https://user:pass@host:port/path?opt1=val1&opt2=val2
    int idx = url.indexOf('@');
    if (idx == -1) return url;
    return "https://" + url.substring(idx + 1);
  }
}
