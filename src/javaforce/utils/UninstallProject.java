package javaforce.utils;

/** Uninstall Project files.
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class UninstallProject implements ShellProcessListener {
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:UninstallProject build.xml");
      System.exit(1);
    }
    try {
      new UninstallProject().run(args[0]);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String buildfile) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML(buildfile)) throw new Exception("error loading " + buildfile);

    String app = tools.getProperty("app");
    String apptype = tools.getProperty("apptype");

    switch (apptype) {
      case "client": app = app + "-client"; break;
      case "server": app = app + "-server"; break;
    }

    //delete /usr/bin/${app}
    new File("/usr/bin/" + app).delete();

    //read files.lst and delete them all
    try {
      File files = new File("files.lst");
      if (files.exists()) {
        FileInputStream fis = new FileInputStream(files);
        byte[] lst = fis.readAllBytes();
        String[] lns = new String(lst).replaceAll("\r","").split("\n");
        for(String ln : lns) {
          if (ln.length() == 0) continue;
          File file = new File(ln);
          if (file.exists()) {
            file.delete();
          }
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }

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
