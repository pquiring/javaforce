package javaforce.service.servlet;

/** WAR
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

  public static boolean debug = true;

  private WAR() {}

  public static WAR load(String folder) {
    if (JF.isWindows()) {
      folder = folder.replaceAll("\\\\", "/");
    }
/*
<web-app version="2.4">

  <servlet>
    <servlet-name>example</servlet-name>
    <servlet-class>code.example</servlet-class>
  </servlet>
  <servlet-mapping>
    <servlet-name>example</servlet-name>
    <url-pattern>/example</url-pattern>
  </servlet-mapping>

  <welcome-file-list>
    <welcome-file>
      index.html
    </welcome-file>
  </welcome-file-list>

</web-app>
*/
    WAR war = new WAR();
    int i1 = folder.lastIndexOf('/');
    int i2 = folder.indexOf('-');
    war.name = folder.substring(i1 + 1, i2);
    war.servlets = new ArrayList<>();
    if (debug) JFLog.log("loading class path...");
    war.loader = makeClassLoader(folder);
    war.folder = folder;

    HttpServlet servlet = null;
    String name = null;

    if (debug) JFLog.log("loading web.xml...");
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
                  if (debug) JFLog.log("WAR:add servlet:" + name);
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
                war.welcome = child.content;
                break;
            }
          }
          break;
      }
    }
    return war;
  }

  private static void printArray(File[] p) {
    System.out.print("p[] = ");
    for(int a=0;a<p.length;a++) {
      System.out.print("{" + p[a] + "}");
    }
    System.out.println("");
  }

/*
  private static void add_jars(ArrayList<File> files, File folder) {
    folder.listFiles((File file) -> {
      boolean is_jar = file.getName().endsWith(".jar");
      if (is_jar) files.add(file);
      return false;
    });
  }

  private static void add_classes(ArrayList<File> files, File folder) {
    folder.listFiles((File file) -> {
      boolean is_class = file.getName().endsWith(".class");
      if (is_class) files.add(file);
      if (file.isDirectory()) {
        add_classes(files, file);
      }
      return false;
    });
  }
*/

  private static JFClassLoader makeClassLoader(String folder) {
    File lib = new File(folder + "/WEB-INF/lib");
    File classes = new File(folder + "/WEB-INF/classes");
    return new JFClassLoader(new File[] {lib, classes});
  }

  public byte[] getStaticResource(String name) {
    if (name.startsWith("META-INF")) return null;
    if (name.startsWith("WEB-INF")) return null;
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
}
