package javaforce.utils;

/** Generates executable for platform.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class GenExecutable {
  private BuildTools tools;
  public static void chmod(String file) {
    new File(file).setExecutable(true);
  }
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
  private void doSubProjects() {
    for(int a=2;a<=5;a++) {
      String project = tools.getProperty("project" + a);
      if (project.length() == 0) continue;
      main(new String[] {project + ".xml"});
    }
  }
}
