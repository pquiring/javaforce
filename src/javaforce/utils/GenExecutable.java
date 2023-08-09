package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenExecutable {
  private XML xml;
  public static void chmod(String file) {
    new File(file).setExecutable(true);
  }
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenExecutable build.xml");
      System.exit(1);
    }
    new GenExecutable().run(args[0]);
  }
  public void run(String buildfile) {
    xml = loadXML(buildfile);
    String home = getProperty("home");
    String app = getProperty("app");
    String apptype = getProperty("apptype");
    String ico = getProperty("ico");
    if (ico.length() == 0) {
      ico = app + ".ico";
    }
    String cfg = getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }
    try {
      if (JF.isWindows()) {
        //windows
        String ext = "";
        switch (apptype) {
          case "c":  //legacy
          case "console": ext = "c"; break;
          case "s":  //legacy
          case "service": ext = "s"; break;
        }
        if (!JF.copyFile(home + "/stubs/win64" + ext + ".exe", app + ".exe")) {
          throw new Exception("copy error");
        }
        WinPE.main(new String[] {app + ".exe", ico, cfg});
      } else if (JF.isMac()) {
        //mac
        JF.copyFile(home + "/stubs/mac64.bin", app);
        chmod(app);
      } else {
        //linux
        JF.copyFile(home + "/stubs/linux64.bin", app + ".bin");
        ResourceManager.main(new String[] {app + ".bin", cfg});
        chmod("/usr/bin/" + app);
      }
      System.out.println("Native Executable generated!");
    } catch (Exception e) {
      e.printStackTrace();
    }
    doSubProjects();
  }
  private static XML loadXML(String buildfile) {
    XML xml = new XML();
    try {
      xml.read(new FileInputStream(buildfile));
      return xml;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  private String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  private String getProperty(String name) {
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
        return attrValue;
      }
    }
    return "";
  }
  private void doSubProjects() {
    for(int a=2;a<=5;a++) {
      String project = getProperty("project" + a);
      if (project.length() == 0) continue;
      main(new String[] {project + ".xml"});
    }
  }
}
