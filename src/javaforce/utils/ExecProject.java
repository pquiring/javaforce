package javaforce.utils;

import java.io.*;
import java.util.*;

import javaforce.*;

/** ExecProject
 *
 * @author pquiring
 */

public class ExecProject implements ShellProcessListener {
  private BuildTools tools;

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("ExecProject : Runs project using java");
      System.out.println("  Usage : ExecProject buildfile [aot-cache-folder]");
      System.exit(1);
    }
    try {
      new ExecProject().run(args);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String[] args) throws Exception {
    String buildfile = args[0];
    String aotcache = null;
    if (args.length > 1) {
      aotcache = args[1];
    }
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);

    String app = tools.getProperty("app");
    String cfg = tools.getProperty("cfg");
    if (cfg.length() == 0) {
      cfg = app + ".cfg";
    }
    String user_home = System.getProperty("user.home");
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

    if (service != null) {
      exec += "s";
    }

    if (JF.isWindows()) {
      exec += ".exe";
    }

    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add(user_home + File.separator + "bin" + File.separator + exec);
    if (aotcache != null) {
      String path = JF.getUserPath() + "/cache";
      new File(path).mkdir();
      String cache = path + "/" + app + ".aot";
      switch (aotcache) {
        case "aot-gen": cmd.add("-XX:AOTCacheOutput=" + cache); break;
        case "aot-run": cmd.add("-XX:AOTCache=" + cache); break;
      }
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
