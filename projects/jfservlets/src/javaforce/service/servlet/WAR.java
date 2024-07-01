package javaforce.service.servlet;

/** WAR (Web ARchive)
 *
 * @author peter.quiring
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import javaforce.*;

public class WAR {
  public String name;
  public XML web;
  public JFClassLoader loader;
  public ArrayList<Servlet> servlets;
  public Object lock = new Object();
  public boolean registered;
  public String welcome = "index.html";
  public String folder;
  public long install;
  public long delete;

  public static boolean debug = false;

  private WAR() {}

  public static class Servlet {
    //must use reflection to get servlet from loader
    public Object servlet;
    public Method service;
    public Class<?> req;
    public Constructor<?> req_ctor;
    public Class<?> res;
    public Constructor<?> res_ctor;
    public String name;
    public String url;
  }

  public static WAR load(String folder) {
    JFLog.log("WAR:load:" + folder);
    if (JF.isWindows()) {
      folder = folder.replaceAll("\\\\", "/");
    }
    WAR war = new WAR();
    war.name = getName(folder);
    war.install = getInstallDate(folder);
    war.servlets = new ArrayList<>();
    war.loader = makeClassLoader(folder);
    war.folder = folder;

    Servlet servlet = null;
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
                  servlet = new Servlet();
                  servlet.name = name;
                  {
                    Class<?> cls = war.loader.findClass(cls_name);
                    if (cls == null) throw new Exception("WAR:class not found:" + cls_name);
                    Constructor<?> ctor = cls.getConstructor();
                    if (ctor == null) throw new Exception("WAR:ctor not found:" + cls_name);
                    servlet.servlet = ctor.newInstance();
                    for(Method method : cls.getMethods()) {
                      if (method.getName().equals("service")) {
                        servlet.service = method;
                      }
                    }
                    if (servlet.service == null) {
                      JFLog.log("Error:service not found:" + name);
                    }
                  }
                  {
                    Class<?> cls = war.loader.findClass("javax.servlet.http.HttpServletRequestImpl");
                    if (cls == null) throw new Exception("WAR:class not found:" + "HttpServletRequestImpl");
                    servlet.req = cls;
                    servlet.req_ctor = cls.getConstructors()[0];
                  }
                  {
                    Class<?> cls = war.loader.findClass("javax.servlet.http.HttpServletResponseImpl");
                    if (cls == null) throw new Exception("WAR:class not found:" + "HttpServletResponseImpl");
                    servlet.res = cls;
                    servlet.res_ctor = cls.getConstructors()[0];
                  }
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
    File servlets = null;
    if (JF.isWindows()) {
      servlets = new File(System.getProperty("java.app.home") + "/servlet-api.jar");
    } else {
      servlets = new File("/usr/share/java/servlet-api.jar");
    }
    return new JFClassLoader(new File[] {lib, classes, servlets});
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

  public Servlet getServletByName(String name) {
    for(Servlet servlet : servlets) {
      if (servlet.name.equals(name)) return servlet;
    }
    return null;
  }

  public Servlet getServletByURL(String url) {
    for(Servlet servlet : servlets) {
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
