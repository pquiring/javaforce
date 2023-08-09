package javaforce.utils;

/** Install Project files.
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

public class InstallProject implements ShellProcessListener {
  private XML xml;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:ExecGraal build.xml");
      System.exit(1);
    }
    try {
      new InstallProject().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String buildfile) throws Exception {
    xml = loadXML(buildfile);

    String app = getProperty("app");

    //cp ${app}.bin /usr/bin/${app}
    JFLog.log("Installing executable:" + app + ".bin to /usr/bin");
    JF.copyFile(app + ".bin", "/usr/bin/" + app);

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);

    //ant -file buildfile install
    ArrayList<String> cmd = new ArrayList<String>();
    if (JF.isWindows()) {
      cmd.add("ant.bat");
    } else {
      cmd.add("ant");
    }
    cmd.add("-file");
    cmd.add(buildfile);
    cmd.add("install");

    JFLog.log("Executing ant -file " + buildfile + " install");
    sp.run(cmd.toArray(new String[0]), true);

    doSubProjects();
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
  private void doSubProjects() {
    for(int a=2;a<=5;a++) {
      String project = getProperty("project" + a);
      if (project.length() == 0) continue;
      main(new String[] {project + ".xml"});
    }
  }
}
