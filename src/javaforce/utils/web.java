package javaforce.utils;

/** Simple web server that serves files from current folder.
 *
 * @author pquiring
 *
 * Created : Mar 4, 2014
 */

import java.io.*;

import javaforce.*;
import javaforce.service.*;

public class web implements WebHandler {
  private static boolean debug;
  private static boolean nosecure;
  public static void main(String[] args) {
    WebServer server = new WebServer();
    for(String arg : args) {
      switch (arg) {
        case "debug":
          debug = true;
          server.debug = true;
          break;
        case "nosecure":
          nosecure = true;
          break;
      }
    }
    if (nosecure) {
      System.out.println("Starting web server on port 80...");
      server.start(new web(), 80);
    } else {
      System.out.println("Starting web server on port 443/80...");
      server.start(new web(), 443, KeyMgmt.getDefaultClient());
      WebServerRedir redir = new WebServerRedir();
      redir.start(80, 443);
    }
  }

  public void doPost(WebRequest req, WebResponse res) {
    doGet(req, res);
  }

  public void doGet(WebRequest req, WebResponse res) {
    String url = req.getURL();
    if (debug) {
      System.out.println(req.getMethod() + ":" + req.getURL());
    }
    if (url.equals("")) url = "/";
    if (url.endsWith("/")) {
      //generate folder listing
      String path = url;
      if (path.startsWith("/")) path = url.substring(1);
      if (path.equals("")) path = ".";
      File folder = new File(path);
      File[] files = folder.listFiles();
      if (files == null) files = new File[0];
      StringBuilder sb = new StringBuilder();
      try {
        sb.append("Index of " + url);
        sb.append("<br><br>");
        for(int a=0;a<files.length;a++) {
          String name = files[a].getName();
          if (files[a].isDirectory()) {
            sb.append("<a href='" + name + "/'>");
            sb.append(name);
            sb.append("</a><br>");
          } else {
            sb.append("<a href='" + name + "'>");
            sb.append(name);
            sb.append("</a><br>");
          }
        }
        OutputStream os = res.getOutputStream();
        os.write(sb.toString().getBytes());
      } catch (Exception e) {
        JFLog.log(e);
      }
    } else {
      //send file
      if (url.startsWith("/")) url = url.substring(1);
      FileInputStream fis = null;
      try {
        File file = new File(url);
        if (!file.exists()) {
          res.setStatus(404, "Not found");
          return;
        }
        fis = new FileInputStream(url);
        JF.copyAll(fis, res.getOutputStream());
        fis.close();
      } catch (Exception e) {
        if (fis != null) try{fis.close();} catch (Exception e2) {}
      }
    }
  }
}
