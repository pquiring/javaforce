package javaforce.utils;

/** ExecGraalAgent
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class ExecGraalAgent implements ShellProcessListener {
  private BuildTools tools;

  public static void main(String args[]) {
    if (args.length != 1) {
      System.out.println("ExecGraalAgent : Runs project using Graal agentlib");
      System.out.println("  Usage : ExecGraalAgent buildfile");
      System.exit(1);
    }
    try {
      new ExecGraalAgent().run(args[0]);
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

    new File("META-INF/native-image").mkdirs();
    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-agentlib:native-image-agent=config-output-dir=META-INF/native-image");
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
