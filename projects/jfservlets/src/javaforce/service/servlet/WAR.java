package javaforce.service.servlet;

/** WAR (Web ARchive)
 *
 * @author peter.quiring
 */

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;

import javax.servlet.http.*;

import javaforce.*;

public class WAR {
  public String name;
  public XML web;
  public JFClassLoader loader;
  public ArrayList<HttpServlet> servlets;
  public Object lock = new Object();
  public boolean registered;
  public String welcome = "index.html";
  public String folder;
  public long install;
  public long delete;

  public static boolean debug = false;

  private WAR() {}

  public static WAR load(String folder) {
    if (JF.isWindows()) {
      folder = folder.replaceAll("\\\\", "/");
    }
    WAR war = new WAR();
    war.name = getName(folder);
    war.install = getInstallDate(folder);
    war.servlets = new ArrayList<>();
    war.loader = makeClassLoader(folder);
    war.folder = folder;

    HttpServlet servlet = null;
    String name = null;

    XML xml = new XML();
    xml.read(folder + "/WEB-INF/web.xml");
    XML.XMLTag root = xml.root;

    for(XML.XMLTag tag : root.children) {
      switch (tag.name) {
        case "servlet":
          servlet = null;
          name = null;
          for(XML.XMLTag child : tag.children) {
            switch (child.name) {
              case "servlet-name":
                name = child.content;
                break;
              case "servlet-class":
                try {
                  String cls_name = child.content;
                  Class<?> cls = war.loader.findClass(cls_name);
                  if (cls == null) throw new Exception("WAR:class not found:" + cls_name);
                  Constructor<?> ctor = cls.getConstructor();
                  if (ctor == null) throw new Exception("WAR:ctor not found:" + cls_name);
                  servlet = (HttpServlet)ctor.newInstance();
                  servlet.name = name;
                  war.servlets.add(servlet);
                } catch (Exception e) {
                  JFLog.log(e);
                }
                break;
            }
          }
          break;
        case "servlet-mapping":
          servlet = null;
          name = null;
          for(XML.XMLTag child : tag.children) {
            switch (child.name) {
              case "servlet-name":
                servlet = war.getServletByName(child.content);
                if (servlet == null) {
                  JFLog.log("Error:Servlet not found:" + child.content);
                }
                break;
              case "url-pattern":
                if (servlet != null) {
                  servlet.url = child.content;
                }
                break;
            }
          }
          break;
        case "welcome-file-list":
          for(XML.XMLTag child : tag.children) {
            switch (child.name) {
              case "welcome-file":
                war.welcome = child.content.trim();
                break;
            }
          }
          break;
      }
    }
    return war;
  }

  public static WAR delete(String name) {
    WAR war = new WAR();
    war.name = getName(name);
    war.install = getInstallDate(name);
    war.folder = Paths.workingPath + "/" + name;
    return war;
  }

  private static JFClassLoader makeClassLoader(String folder) {
    File lib = new File(folder + "/WEB-INF/lib");
    File classes = new File(folder + "/WEB-INF/classes");
    return new JFClassLoader(new File[] {lib, classes});
  }

  public byte[] getStaticResource(String name) {
    if (name.toUpperCase().startsWith("META-INF")) return null;
    if (name.toUpperCase().startsWith("WEB-INF")) return null;
    File file = new File(folder + "/" + name);
    if (!file.exists()) return null;
    try {
      InputStream is = new FileInputStream(file);
      byte[] data = is.readAllBytes();
      is.close();
      return data;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }

  public HttpServlet getServletByName(String name) {
    for(HttpServlet servlet : servlets) {
      if (servlet.name.equals(name)) return servlet;
    }
    return null;
  }

  public HttpServlet getServletByURL(String url) {
    for(HttpServlet servlet : servlets) {
      if (JF.isWildcard(servlet.url)) {
        if (JF.wildcardCompare(url, servlet.url, true)) {
          return servlet;
        }
      } else {
        if (servlet.url.equals(url)) return servlet;
      }
    }
    return null;
  }

  public static long getInstallDate(String folder) {
    int idx = folder.indexOf('-');
    if (idx == -1) return -1;
    return Long.valueOf(folder.substring(idx + 1));
  }

  public static String getName(String folder) {
    int i1 = folder.lastIndexOf('/');
    if (i1 == -1) i1 = 0; else i1++;
    int i2 = folder.indexOf('-');
    return folder.substring(i1, i2);
  }

  public String toString() {
    return name + "-" + install;
  }
}
