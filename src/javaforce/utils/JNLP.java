package javaforce.utils;

import java.io.*;
import java.util.*;

import javaforce.*;

/** JNLP Very Basic Launcher
 *
 * Usage : JNLP file.jnlp
 *
 * Supports:
 *  - jnlp,codebase
 *  - resources,href
 *  - properties,name,value
 *  - application-desc,main-class
 *
 * Not supported:
 *  - Applets
 *  - parameters
 *
 * Tested with Inductive Automation : Ignition v7.x
 *
 * @author pquiring
 */

public class JNLP {
  private static boolean debug = false;
  private static String version = "0.1";
  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      System.out.println("Desc: JavaForce JNLP Launcher/" + version);
      System.out.println("Usage: JNLP file.jnlp");
      System.out.println("Author: Peter Quiring");
      System.out.println("WebSite: http://github.com/pquiring/javaforce");
      return;
    }
    try {
      XML xml = new XML();
      xml.setUseUniqueNames(false);
      xml.read(args[0]);
      XML.XMLTag jnlp = xml.getTag(new String[] {"jnlp"});
      if (jnlp == null) throw new Exception("no jnlp tag");
      XML.XMLTag res = xml.getTag(new String[] {"jnlp", "resources"});
      if (res == null) throw new Exception("no resources tag");
      XML.XMLTag app = xml.getTag(new String[] {"jnlp", "application-desc"});
      if (app == null) throw new Exception("no application-desc tag");
      String codebase = jnlp.getArg("codebase");
      if (codebase == null) throw new Exception("no codebase attr");
      //split codebase into host/uri
      String proto = "";
      if (codebase.startsWith("http://")) {
        proto = "http";
        codebase = codebase.substring(7);
      } else if (codebase.startsWith("https://")) {
        proto = "https";
        codebase = codebase.substring(8);
      } else {
        throw new Exception("Unknown protocol");
      }
      String host = "";
      String uri = "";
      int i0 = codebase.indexOf('/');
      if (i0 == -1) {
        host = codebase;
        uri = "/";
      } else {
        host = codebase.substring(0, i0);
        uri = codebase.substring(i0);
        if (!uri.endsWith("/")) {
          uri += '/';
        }
      }
      if (debug) JFLog.log("host=" + host);
      int port = 80;
      int i1 = host.indexOf(':');
      if (i1 != -1) {
        if (debug) JFLog.log("port=" + host.substring(i1 + 1));
        port = Integer.valueOf(host.substring(i1 + 1));
        host = host.substring(0, i1);
      }
      ArrayList<String> jars = new ArrayList<String>();
      ArrayList<String> files = new ArrayList<String>();
      ArrayList<String> cmd = new ArrayList<String>();
      cmd.add(System.getProperty("java.home") + "/bin/java");
      int rescnt = res.getChildCount();
      for(int i2=0;i2<rescnt;i2++) {
        XML.XMLTag child = res.getChildAt(i2);
        switch (child.name) {
          case "jar":
            String jar = child.getArg("href");
            jars.add(jar);
            int i3 = jar.lastIndexOf('/');
            if (i3 != -1) {
              files.add(jar.substring(i3+1));
            } else {
              files.add(jar);
            }
            break;
          case "property":
            String name = child.getArg("name");
            String value = child.getArg("value");
            cmd.add("-D" + name + "=" + value);
            break;
          default:
            JFLog.log("Unknown resource:" + child.name);
            break;
        }
      }
      String main = app.getArg("main-class");
      String classpath = String.join(File.pathSeparator, files);
      cmd.add("-cp");
      cmd.add(classpath);
      cmd.add(main);
      //download jar files
      for(String jar : jars) {
        HTTP http = null;
        switch (proto) {
          case "http": http = new HTTP(); break;
          case "https": http = new HTTPS(); break;
        }
        http.open(host, port);
        byte[] data = http.get(uri + jar);
        if (data == null || data.length == 0) {
          JFLog.log("Download Error:" + uri + jar);
        }
        http.close();
        int i4 = jar.lastIndexOf('/');
        if (i4 != -1) {
          jar = jar.substring(i4+1);
        }
        FileOutputStream fos = new FileOutputStream(jar);
        fos.write(data);
        fos.close();
      }
      //launch jnlp
      if (debug) {
        JFLog.log("executing jnlp:");
        for(String c : cmd) {
          JFLog.log(c);
        }
      }
      Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}

/*/

JNLP Layout:

<jnlp codebase="http://..." ...>
  <resources>
    <jar href="relpath/file.jar" .../>
    <property name="..." value="..."/>
  </resources>
  <application-desc main-class="package.class" .../>
</jnlp>

/*/
