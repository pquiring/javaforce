package javaforce.utils;

/** Build Tools
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class BuildTools {
  private XML xml;
  private BuildTools versions;
  private String file;
  private boolean debug = false;

  public boolean loadXML(String xmlfile) {
    this.file = xmlfile;
    xml = new XML();
    try {
      xml.read(new FileInputStream(xmlfile));
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return false;
  }

  public String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  public String getProperty(String name) {
    //<project> <property name="name" value="value">
    int cnt = xml.root.getChildCount();
    for(int a=0;a<cnt;a++) {
      XML.XMLTag tag = xml.root.getChildAt(a);
      if (!tag.name.equals("property")) continue;
      int attrs = tag.attrs.size();
      String attrName = null;
      String attrValue = null;
      for(int b=0;b<attrs;b++) {
        XML.XMLAttr attr = tag.attrs.get(b);
        if (attr.name.equals("name")) {
          attrName = attr.value;
        }
        if (attr.name.equals("value")) {
          attrValue = attr.value;
        }
        if (attr.name.equals("location")) {
          attrValue = attr.value;
        }
      }
      if (attrName != null && attrName.equals(name)) {
        //TODO : expand ${} tags
        if (attrValue.equals("${javaforce-version}")) {
          attrValue = getVersion("javaforce-version");
        }
        return attrValue;
      }
    }
    if (debug) JFLog.log("error:property not found:" + name + ":file=" + file);
    return "";
  }
  public String getVersion(String name) {
    String home = getProperty("home");
    if (versions == null) {
      versions = new BuildTools();
      versions.loadXML(home + "/versions.xml");
    }
    return versions.getProperty(name);
  }
  public static void chmod_x(String file) {
    new File(file).setExecutable(true);
  }
  public static boolean checkFiles(String files_lst) {
    try {
      File files = new File(files_lst);
      if (!files.exists()) {
        JFLog.log("Error:" + files_lst + " not found");
        return false;
      }
      FileInputStream fis = new FileInputStream(files);
      byte[] lst = fis.readAllBytes();
      String[] lns = new String(lst).replaceAll("\r","").split("\n");
      for(String ln : lns) {
        if (ln.length() == 0) continue;
        File file = new File(ln);
        if (!file.exists()) {
          JFLog.log("Error:File not found:" + ln);
          return false;
        }
      }
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  public String[] getSubProjects() {
    if (!file.endsWith(File.separator + "build.xml")) {
      return new String[0];
    }
    if (!new File("projects.lst").exists()) {
      return new String[0];
    }
    try {
      byte[] data = JF.readFile("projects.lst");
      String[] subs = new String(data).replaceAll("\r", "").split("\n");
      //remove empty lines
      ArrayList<String> list = new ArrayList<>();
      for(String sub : subs) {
        sub = sub.trim();
        if (sub.length() == 0) continue;
        if (sub.equals("build")) continue;
        list.add(sub);
      }
      return list.toArray(JF.StringArrayType);
    } catch (Exception e) {
      return new String[0];
    }
  }
}
