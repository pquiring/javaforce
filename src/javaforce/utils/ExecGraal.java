package javaforce.utils;

/** Executes Graal 'native-image' command.
 *
 * @author pquiring
 */

import java.util.*;
import java.io.*;

import javaforce.*;

public class ExecGraal implements ShellProcessListener {
  private BuildTools tools;

  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:ExecGraal build.xml");
      System.exit(1);
    }
    try {
      new ExecGraal().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String buildfile) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);

    String app = tools.getProperty("app");
    String cfg = tools.getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }

    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(cfg);
    props.load(fis);
    fis.close();

    String classpath = props.getProperty("CLASSPATH");
    String mainclass = props.getProperty("MAINCLASS");

    classpath += ";meta.jar";

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    if (JF.isWindows()) {
      cmd.add("native-image.cmd");
    } else {
      cmd.add("native-image");
    }
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(classpath);
    } else {
      cmd.add(classpath.replaceAll("[;]", ":"));
    }
    cmd.add(mainclass.replaceAll("/", "."));
    cmd.add("--shared");
    cmd.add("--no-fallback");

    String[] cmdArray = cmd.toArray(JF.StringArrayType);

    System.out.println("cmd=" + JF.join(" ", cmdArray));

    sp.run(cmdArray, true);

    doSubProjects();
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
  private void doSubProjects() {
    for(int a=2;a<=5;a++) {
      String project = tools.getProperty("project" + a);
      if (project.length() == 0) continue;
      main(new String[] {project + ".xml"});
    }
  }
}
