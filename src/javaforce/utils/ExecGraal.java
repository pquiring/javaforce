package javaforce.utils;

/** Executes Graal 'native-image' command.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class ExecGraal implements ShellProcessListener {
  public static void main(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage:ExecGraal CLASSPATH MAINCLASS");
      System.exit(1);
    }
    new ExecGraal().run(args);
  }

  public void run(String[] args) {
    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    if (JF.isWindows()) {
      cmd.add("native-image.cmd");
    } else {
      cmd.add("native-image");
    }
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(args[0]);  //CLASSPATH
    } else {
      cmd.add(args[0].replaceAll("[;]", ":"));  //CLASSPATH
    }
    cmd.add(args[1].replaceAll("/", "."));  //MAINCLASS
    cmd.add("--shared");
    cmd.add("--enable-all-security-services");
    JFLog.log("Using:javaforce-jni-config.json");
    cmd.add("-H:JNIConfigurationFiles=javaforce-jni-config.json");
    String agent_jni = "jni-config.json";
    if (new File(agent_jni).exists()) {
      JFLog.log("Using:" + agent_jni);
      cmd.add("-H:JNIConfigurationFiles=" + agent_jni);
    }
    JFLog.log("Using:javaforce-resource-config.json");
    cmd.add("-H:ResourceConfigurationFiles=javaforce-resource-config.json");
    String agent_res = "resource-config.json";
    if (new File(agent_res).exists()) {
      JFLog.log("Using:" + agent_res);
      cmd.add("-H:ResourceConfigurationFiles=" + agent_res);
    }
    String agent_reflect = "reflect-config.json";
    if (new File(agent_reflect).exists()) {
      JFLog.log("Using:" + agent_reflect);
      cmd.add("-H:ReflectionConfigurationFiles=" + agent_reflect);
    }
    String app_res = "app-resource-config.json";
    if (new File(app_res).exists()) {
      JFLog.log("Using:" + app_res);
      cmd.add("-H:ResourceConfigurationFiles=" + app_res);
    }
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
