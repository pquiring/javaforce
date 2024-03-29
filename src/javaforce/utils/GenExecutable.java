package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenExecutable {
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
    String home = tools.getProperty("home");
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
        }
        if (!JF.copyFile(home + "/native/win64" + type + ".exe", app + ".exe")) {
          throw new Exception("copy error:" + app + ".exe");
        }
        if (!new File(app + ".exe").exists()) {
          throw new Exception("copy error:" + app + ".exe");
        }
        WinPE.main(new String[] {app + ".exe", ico, cfg});
      } else if (JF.isMac()) {
        //mac
        JF.copyFile(home + "/native/mac64.bin", app);
      } else {
        //linux
        switch (apptype) {
          case "s":  //legacy
          case "service": type = "s"; break;
        }
        JF.copyFile(home + "/native/linux64" + type + ".bin", app + ".bin");
        ResourceManager.main(new String[] {app + ".bin", cfg});
      }
      System.out.println("Native Executable generated!");
    } catch (Exception e) {
      e.printStackTrace();
    }
    doSubProjects();
  }
  private void doSubProjects() {
    for(int a=2;a<=5;a++) {
      String project = tools.getProperty("project" + a);
      if (project.length() == 0) continue;
      main(new String[] {project + ".xml"});
    }
  }
}
