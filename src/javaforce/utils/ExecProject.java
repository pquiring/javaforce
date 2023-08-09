package javaforce.utils;

/** ExecProject
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class ExecProject implements ShellProcessListener {
  private XML xml;
  private void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("ExecProject : Runs project using java");
      System.out.println("  Usage : ExecProject buildfile");
      System.exit(1);
    }
    try {
      new ExecProject().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String buildfile) throws Exception {
    xml = loadXML(buildfile);

    String app = getProperty("app");
    String cfg = getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }

    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(cfg);
    props.load(fis);
    fis.close();

    String classpath = props.getProperty("CLASSPATH");
    String mainclass = props.getProperty("MAINCLASS");

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(classpath);
    } else {
      cmd.add(classpath.replaceAll("[;]", ":"));
    }
    cmd.add(mainclass);
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
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
}
