package javaforce.service;

/** ProxyTest
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;

public class ProxyTest {
  public static void load(String urlstr) {
    try {
      System.out.println("GET " + urlstr);
      URL url = new URI(urlstr).toURL();
      java.net.Proxy proxy = new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress("localhost", 3128));
      URLConnection c = url.openConnection(proxy);
      c.connect();
      InputStream is = c.getInputStream();
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      String[] keys = c.getHeaderFields().keySet().toArray(new String[0]);
      for(int a=0;a<keys.length;a++) {
        String value = c.getHeaderField(keys[a]);
        System.out.println(keys[a] + "=" + value);
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public static void main(String[] args) {
    JF.initHttps();
    HttpURLConnection.setFollowRedirects(false);
    load("https://example.com");
    load("https://arstechnica.com/");
  }
}
