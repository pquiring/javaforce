package javaforce.utils;

/** Install Project files.
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class InstallProject implements ShellProcessListener {
  private BuildTools tools;
  public static void main(String[] args) {
    if (args.length != 1) {
      System.out.println("Usage:InstallProject build.xml");
      System.exit(1);
    }
    try {
      new InstallProject().run(args[0]);
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

    //cp ${app}.bin /usr/bin/${app}
    JFLog.log("Installing executable:" + app + ".bin to /usr/bin");
    JF.copyFile(app + ".bin", "/usr/bin/" + app);
    BuildTools.chmod_x("/usr/bin/" + app);

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
    sp.run(cmd.toArray(JF.StringArrayType), true);

    doSubProjects();
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
  private void doSubProjects() {
    String[] subs = tools.getSubProjects();
    for(String project : subs) {
      main(new String[] {project + ".xml"});
    }
  }
}
