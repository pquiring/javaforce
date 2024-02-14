package javaforce.utils;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javaforce.*;

/** JNLP Very Basic Launcher
 *
 * Usage : JNLP file.jnlp [--nowait] [--debug]
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
 * On Windows the --nowait may be required.
 * On Linux the --nowait should not be used.
 *
 * Tested with:
 *  - Inductive Automation : Ignition v7.x
 *  - FCA BlueZone716
 *
 * @author pquiring
 */

public class JNLP {
  private static boolean debug = false;
  private static String version = "0.6";
  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      System.out.println("Desc: JavaForce JNLP Launcher/" + version);
      System.out.println("Usage: JNLP file.jnlp [--nowait]");
      System.out.println("Where: --nowait = Do not wait for JNLP app to exit");
      System.out.println("Author: Peter Quiring");
      System.out.println("WebSite: http://github.com/pquiring/javaforce");
      return;
    }
    boolean wait = true;
    for(int idx = 0;idx<args.length;idx++) {
      String arg = args[idx];
      switch (arg) {
        case "--nowait": {
          wait = false;
          break;
        }
        case "--debug": {
          debug = true;
          break;
        }
      }
    }
    if (debug) {
      System.out.println("JavaForce JNLP Launcher/" + version);
    }
    try {
      XML xml = new XML();
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
      int port = -1;
      String proto = "";
      if (codebase.startsWith("http://")) {
        proto = "http";
        codebase = codebase.substring(7);
        port = 80;
      } else if (codebase.startsWith("https://")) {
        proto = "https";
        codebase = codebase.substring(8);
        port = 443;
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
      int i1 = host.indexOf(':');
      if (i1 != -1) {
        if (debug) JFLog.log("port=" + host.substring(i1 + 1));
        port = Integer.valueOf(host.substring(i1 + 1));
        host = host.substring(0, i1);
      }
      ArrayList<String> res_jars = new ArrayList<String>();
      ArrayList<String> classpath_jars = new ArrayList<String>();
      ArrayList<String> nativelibs_jars = new ArrayList<String>();
      ArrayList<String> cmd = new ArrayList<String>();
      cmd.add(System.getProperty("java.home") + "/bin/java");
      int rescnt = res.getChildCount();
      for(int i2=0;i2<rescnt;i2++) {
        XML.XMLTag child = res.getChildAt(i2);
        switch (child.name) {
          case "nativelib":
          case "jar":
            String jar = child.getArg("href");
            if (debug) JFLog.log("jar=" + jar);
            res_jars.add(jar);
            int i3 = jar.lastIndexOf('/');
            if (i3 != -1) {
              jar = jar.substring(i3+1);
            }
            if (child.name.equals("jar"))
              classpath_jars.add(jar);
            else
              nativelibs_jars.add(jar);
            break;
          case "property":
            String name = child.getArg("name");
            String value = child.getArg("value");
            if (debug) JFLog.log("property:" + name + "=" + value);
            cmd.add("-D" + name + "=" + value);
            break;
          default:
            JFLog.log("Unknown resource:" + child.name);
            break;
        }
      }
      String main = app.getArg("main-class");
      String classpath = String.join(File.pathSeparator, classpath_jars);
      cmd.add("-cp");
      cmd.add(classpath);
      cmd.add(main);
      //download jar files
      for(String jar : res_jars) {
        HTTP http = null;
        switch (proto) {
          case "http": http = new HTTP(); break;
          case "https": http = new HTTPS(); break;
        }
        if (debug) {
          JFLog.log("Downloading:" + host + ":" + port + uri + jar);
          http.debug = true;
          JFLog.log("http=" + http);
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
      //extract nativelib jars
      for(String jar : nativelibs_jars) {
        unzip(jar, ".");
      }
      //add arguments
      int argcnt = app.getChildCount();
      for(int i2=0;i2<argcnt;i2++) {
        XML.XMLTag child = app.getChildAt(i2);
        switch (child.name) {
          case "argument":
            String arg = child.content;
            cmd.add(arg);
            break;
        }
      }
      //launch jnlp
      if (debug) {
        JFLog.log("executing jnlp:");
        FileOutputStream fos = new FileOutputStream("execute" + JF.getScriptExt());
        for(String c : cmd) {
          fos.write(c.getBytes());
          fos.write(" ".getBytes());
          JFLog.log(c);
        }
        fos.close();
      }
      Process p = Runtime.getRuntime().exec(cmd.toArray(new String[cmd.size()]));
      if (wait) p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public static void unzip(String zipFilePath, String destDirectory) throws IOException {
    File destDir = new File(destDirectory);
    if (!destDir.exists()) {
      destDir.mkdir();
    }
    ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
    ZipEntry entry = zipIn.getNextEntry();
    // iterates over entries in the zip file
    while (entry != null) {
      if (debug) JFLog.log("entry=" + entry.getName());
      String filePath = destDirectory + '/' + entry.getName().replaceAll("\\\\", "/");
      if (!entry.isDirectory()) {
        // if the entry is a file, extracts it
        extractFile(zipIn, filePath);
      } else {
        // if the entry is a directory, make the directory
        if (debug) JFLog.log("mkdir:" + filePath);
        File dir = new File(filePath);
        dir.mkdirs();
      }
      zipIn.closeEntry();
      entry = zipIn.getNextEntry();
    }
    zipIn.close();
  }
  private static final int BUFFER_SIZE = 4096;
  private static void extractFile(ZipInputStream zipIn, String path_file) throws IOException {
    if (debug) JFLog.log("extractFile:" + path_file);
    int idx = path_file.lastIndexOf('/');
    String path = path_file.substring(0, idx);
    if (debug) JFLog.log("mkdirs:" + path);
    File dir = new File(path);
    dir.mkdirs();
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(path_file));
    byte[] bytesIn = new byte[BUFFER_SIZE];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
  }
}

/*/

JNLP Layout:

<jnlp codebase="http://..." ...>
  <resources>
    <jar href="relpath/file.jar" .../>
    <nativelib href="relpath/file.jar" .../>
    <property name="..." value="..."/>
  </resources>
  <application-desc main-class="package.class" .../>
</jnlp>

/*/
