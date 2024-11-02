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

  public static void main(String[] args) {
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
    String service = props.getProperty("SERVICE");
    String graalargs = props.getProperty("GRAALARGS");
    if (graalargs == null) {
      graalargs = "";
    }

    String folder = "META-INF/native-image";
    new File(folder).mkdirs();
    //delete existing config files (except javaforce)
    if (!JF.getCurrentPath().endsWith("javaforce")) {
      File[] files = new File(folder).listFiles();
      for(File file : files) {
        if (file.isFile()) {
          file.delete();
        }
      }
    }
    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    String home = tools.getProperty("home");
    String exec = "jfexec";
    if (service != null) {
      exec += "s";
    }
    exec += "d";  //use debug version
    if (JF.isWindows()) {
      exec += ".exe";
    }

    cmd.add(exec);
    cmd.add("-agentlib:native-image-agent=config-output-dir=META-INF/native-image");
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(classpath);
    } else {
      cmd.add(classpath.replaceAll("[;]", ":"));
    }
    cmd.add(mainclass);

    String[] args = graalargs.split(" ");
    for(String arg : args) {
      cmd.add(arg);
    }

    String[] cmdArray = cmd.toArray(JF.StringArrayType);

    System.out.println("cmd=" + JF.join(" ", cmdArray));

    sp.run(cmdArray, true);

    generate_jni_config(mainclass);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }

  private void generate_jni_config(String mainclass) {
    String filename = "META-INF/native-image/jni-config.json";
    if (new File(filename).exists()) return;
    try {
      StringBuilder jni = new StringBuilder();
      jni.append("[");
      jni.append("{ \"name\" : \"" + mainclass + "\", \"methods\": [ {\"name\" : \"main\"} ]}");
      jni.append("]");
      FileOutputStream fos = new FileOutputStream(filename);
      fos.write(jni.toString().getBytes());
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
}
