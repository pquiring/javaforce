package javaforce.utils;

/** ExecProject
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class ExecProject implements ShellProcessListener {
  private BuildTools tools;

  public static void main(String[] args) {
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
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);

    String app = tools.getProperty("app");
    String cfg = tools.getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }
    String home = tools.getProperty("home");
    String exec = "jfexecd";  //use debug version
    if (JF.isWindows()) {
      String apptype = tools.getProperty("apptype");
      if (apptype == null) {
        apptype = "window";
      }
      switch (apptype) {
//        case "window": exec += "w"; break;  //for debugging purposes do not use 'w' version
      }
    }

    Properties props = new Properties();
    FileInputStream fis = new FileInputStream(cfg);
    props.load(fis);
    fis.close();

    String classpath = props.getProperty("CLASSPATH");
    String mainclass = props.getProperty("MAINCLASS");
    String service = props.getProperty("SERVICE");

    if (service != null) {
      exec += "s";
    }

    if (JF.isWindows()) {
      exec += ".exe";
    }

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add(home + File.separator + "bin" + File.separator + exec);
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
}
