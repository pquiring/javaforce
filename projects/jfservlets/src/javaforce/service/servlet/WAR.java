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
    private Object servlet;
    private Method service;
    private Method init;
    private Method destroy;
    private Class<?> req;
    private Constructor<?> req_ctor;
    private Class<?> res;
    private Constructor<?> res_ctor;
    private String name;
    private String url;
    private boolean inited;
    private Object lock = new Object();

    private void init() {
      synchronized (lock) {
        if (inited) return;
        try {
          if (init != null) {
            init.invoke(servlet);
          }
        } catch (Throwable t) {
          JFLog.log(t);
        }
        inited = true;
      }
    }

    private void destroy() {
      synchronized (lock) {
        if (!inited) return;
        try {
          if (destroy != null) {
            destroy.invoke(servlet);
          }
        } catch (Throwable t) {
          JFLog.log(t);
        }
        inited = false;
      }
    }

    public Object createRequest(HashMap map) {
      try {
        return req_ctor.newInstance(map);
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }

    public Object createResponse(HashMap map) {
      try {
        return res_ctor.newInstance(map);
      } catch (Exception e) {
        JFLog.log(e);
        return null;
      }
    }

    public void invoke(Object http_req, Object http_res) {
      if (!inited) {
        init();
      }
      try {
        service.invoke(servlet, http_req, http_res);
      } catch (Throwable t) {
        JFLog.log(t);
      }
    }
  }

  public static WAR load(String folder) {
    JFLog.log("WAR:load:" + folder);
    if (JF.isWindows()) {
      folder = folder.replaceAll("\\\\", "/");
    }
    String web_xml = folder + "/WEB-INF/web.xml";
    if (!new File(web_xml).exists()) {
      JFLog.log("Error:WEB-INF/web.xml missing");
      return null;
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
    xml.read(web_xml);
    XML.XMLTag root = xml.root;

    for(XML.XMLTag tag : root.children) {
      switch (tag.name) {
        case "servlet":
          servlet = null;
          name = null;
          for(XML.XMLTag child : tag.children) {
            switch (child.name) {
              case "servlet-name":
                name = child.content.trim();
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
                    Method[] methods = cls.getMethods();
                    for(Method method : methods) {
                      String sign = method.toString();
                      if (debug) {
                        JFLog.log("method:name=" + method.getName() + ":signature=" + sign);
                      }
                      switch (sign) {
                        case "public void javax.servlet.http.HttpServlet.service(javax.servlet.ServletRequest,javax.servlet.ServletResponse)":
                          servlet.service = method;
                          break;
                        case "public void javax.servlet.http.GenericServlet.init()":
                          servlet.init = method;
                          break;
                        case "public void javax.servlet.http.GenericServlet.destroy()":
                          servlet.destroy = method;
                          break;
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
                  JFLog.log("WAR:servlet:" + servlet.name);
                  war.servlets.add(servlet);
                } catch (Throwable e) {
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
                name = child.content.trim();
                servlet = war.getServletByName(name);
                if (servlet == null) {
                  JFLog.log("Error:Servlet not found:" + name);
                }
                break;
              case "url-pattern":
                if (servlet != null) {
                  servlet.url = child.content.trim();
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

  //destroy all servlets (if inited)
  public void destroyAll() {
    //TODO : wait till all invoke()s have completed
    for(Servlet servlet : servlets) {
      if (servlet.inited) {
        servlet.destroy();
      }
    }
  }

  public String toString() {
    return name + "-" + install;
  }
}
