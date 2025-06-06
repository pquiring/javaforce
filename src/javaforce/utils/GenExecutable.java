package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class GenExecutable implements ShellProcessListener {
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:GenExecutable build.xml");
      System.exit(1);
    }
    try {
      new GenExecutable().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public void run(String buildfile) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);
    String user_home = System.getProperty("user.home");
    String app = tools.getProperty("app");
    String apptype = tools.getProperty("apptype");
    String ico = tools.getProperty("ico");
    if (ico.length() == 0) {
      ico = app + ".ico";
    }
    String cfg = tools.getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }
    String type = "";
    try {
      if (JF.isWindows()) {
        //windows
        switch (apptype) {
          case "c":  //legacy
          case "console": type = "c"; break;
          case "s":  //legacy
          case "service": type = "s"; break;
          case "client": app = app + "-client"; break;
          case "server": app = app + "-server"; break;
        }
        if (!JF.copyFile(user_home + "/bin/win64" + type + ".exe", app + ".exe")) {
          throw new Exception("copy error:" + app + ".exe");
        }
        if (!new File(app + ".exe").exists()) {
          throw new Exception("copy error:" + app + ".exe");
        }
        WinPE.main(new String[] {app + ".exe", ico, cfg});
      } else if (JF.isMac()) {
        //mac
        switch (apptype) {
          case "client": app = app + "-client"; break;
          case "server": app = app + "-server"; break;
        }
        JF.copyFile(user_home + "/bin/mac64.bin", app);
      } else {
        //linux
        switch (apptype) {
          case "s":  //legacy
          case "service": type = "s"; break;
          case "client": app = app + "-client"; break;
          case "server": app = app + "-server"; break;
        }
        JF.copyFile(user_home + "/bin/linux64" + type + ".bin", app + ".bin");
        ResourceManager.main(new String[] {app + ".bin", cfg});
      }
      System.out.println("Native Executable generated!");
    } catch (Exception e) {
      e.printStackTrace();
    }
    doSubProjects();
  }
  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
  private void doSubProjects() {
    String[] subs = tools.getSubProjects();
    for(String project : subs) {
      ShellProcess sp = new ShellProcess();
      sp.addListener(this);
      String buildfile = project + ".xml";
      //ant -file buildfile install
      ArrayList<String> cmd = new ArrayList<String>();
      if (JF.isWindows()) {
        cmd.add("ant.bat");
      } else {
        cmd.add("ant");
      }
      cmd.add("-file");
      cmd.add(buildfile);
      cmd.add("executable");

      JFLog.log("Executing ant -file " + buildfile + " executable");
      sp.run(cmd.toArray(JF.StringArrayType), true);
    }
  }
}
