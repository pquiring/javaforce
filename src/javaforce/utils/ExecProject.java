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
    String exec = "jfexec";
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
    String ffmpeg = props.getProperty("FFMPEG");
    String vm = props.getProperty("VM");

    if (service != null) {
      exec += "s";
    }

    exec += "d";  //use debug version

    if (JF.isWindows()) {
      exec += ".exe";
    }

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add(home + File.separator + "bin" + File.separator + exec);
    if (ffmpeg != null) {
      cmd.add("-ffmpeg");
    }
    if (vm != null) {
      cmd.add("-vm");
    }
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(classpath);
    } else {
      cmd.add(classpath.replaceAll("[;]", ":"));
    }
    cmd.add(mainclass);
    sp.run(cmd.toArray(JF.StringArrayType), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
