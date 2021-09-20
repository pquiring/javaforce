package server;

/**
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;

import javaforce.*;
import javaforce.awt.*;
import javaforce.service.*;

public class Service implements WebHandler {
  private Web web;
  private String base64_password;
  private String rdpPass;
  private volatile long rdp;
  private String cs;
  private int maxUsers;
  private boolean viewOnly;

  private ServerSocket ss;

  public void start(String webPass, String rdpPass, int maxUsers, boolean viewOnly) {
    if (webPass == null || rdpPass.length() == 0) {
      return;
    }
    this.rdpPass = rdpPass;
    String u_p = ":" + webPass;
    base64_password = new String(Base64.encode(u_p.getBytes()));
    this.maxUsers = maxUsers;
    this.viewOnly = viewOnly;
    web = new Web();
    if (!web.start(this, 33001, true)) {
      JFAWT.showError("Error", "Failed to start Web Server");
      return;
    }
  }

  public void close() {
    System.out.println("RDP.close()");
    disconnect();
    if (web != null) {
      web.stop();
      web = null;
    }
    if (ss != null) {
      try {ss.close();} catch (Exception e) {}
    }
  }

  public void disconnect() {
    if (rdp != 0) {
      WDS.stopServer(rdp);
      rdp = 0;
    }
  }

  private void send401(WebResponse res) {
    res.addCookie("WWW-Authenticate", "Basic realm=\"jfRDP\"");
    res.setStatus(401, "401 Auth required.");
  }

  private void noCache(WebResponse res) {
    res.addHeader("Cache-Control: no-cache, no-store, must-revalidate");
    res.addHeader("Pragma: no-cache");
    res.addHeader("Expires: 0");
  }

  public void doGet(WebRequest req, WebResponse res) {
    String url = req.getURL();
    if (!url.equals("/invite")) {
      res.setStatus(404, "404 Object not found.");
      return;
    }
    String auth = req.getHeader("Authorization");
    if (auth == null) {
      send401(res);
      return;
    }
    //f = Basic "base64(username:password)"
    String f[] = auth.split(" ");
    if (f.length != 2 || !f[0].equals("Basic") || !f[1].equals(base64_password)) {
      send401(res);
      return;
    }
    if (rdpPass == null || rdpPass.length() == 0) {
      try {res.write("Server setup incomplete".getBytes());} catch (Exception e) {e.printStackTrace();}
      return;
    }
    if (maxUsers == 1 && rdp != 0) {
      WDS.stopServer(rdp);
      rdp = 0;
    }
    if (rdp == 0) {
      cs = null;
      new Thread() {
        public void run() {
          rdp = WDS.startServer("Viewer", "Group", rdpPass, maxUsers, viewOnly, 33002);
          cs = WDS.getConnectionString(rdp);
          WDS.runServer(rdp);
        }
      }.start();
      while (cs == null) {
        JF.sleep(10);
      }
      cs = fixIP(cs, req.getHost());
    }
    noCache(res);
    res.setContentType("text/plain");
    try {res.write(cs.getBytes());} catch (Exception e) {e.printStackTrace();}
    System.out.println("Connection string delivered.");
  }

  public void doPost(WebRequest req, WebResponse res) {
    res.setStatus(500, "500 POST not supported");
  }

  private String fixIP(String txt, String host) {
    XML xml = new XML();
    ByteArrayInputStream bais = new ByteArrayInputStream(txt.getBytes());
    xml.read(bais);
    //...
    XML.XMLTag E = xml.root;
    XML.XMLTag A = xml.getTag(new String[] {"E", "A"});
    XML.XMLTag C = xml.getTag(new String[] {"E", "C"});
    XML.XMLTag T = xml.getTag(new String[] {"E", "C", "T"});
    //enumerate T's L children
    int cnt = T.getChildCount();
    boolean gotIP4 = false;
    for(int i=0;i<cnt;) {
      XML.XMLTag l = T.getChildAt(i);
      String N = l.getArg("N");
      if (gotIP4 || N.indexOf(":") != -1) {
        //IP6 - ignore it
        T.remove(i);
        cnt--;
      } else {
        //IP4 - use it but change P and H
        l.setArg("N", host);
//        l.setArg("P", "33002");
        gotIP4 = true;
        i++;
      }
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    xml.write(baos);
    return new String(baos.toByteArray());
  }
}
