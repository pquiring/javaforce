package javaforce.utils;

/** Executes Graal 'native-image' command.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class ExecGraal implements ShellProcessListener {
  private static String json_path = "META-INF/native-image";
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
//    cmd.add("--allow-incomplete-classpath");  //deprecated (default)
    cmd.add("--no-fallback");
//    cmd.add("--enable-all-security-services");  //deprecated (for removal)

    JFLog.log("Using:META-INF/native-image/javaforce-jni-config.json");
    cmd.add("-H:JNIConfigurationFiles=" + json_path + "/javaforce-jni-config.json");

    String agent_jni = json_path + "/jni-config.json";
    if (new File(agent_jni).exists()) {
      JFLog.log("Using:" + agent_jni);
      cmd.add("-H:JNIConfigurationFiles=" + agent_jni);
    }

    JFLog.log("Using:META-INF/native-image/javaforce-resource-config.json");
    cmd.add("-H:ResourceConfigurationFiles=" + json_path + "/javaforce-resource-config.json");

    String agent_res = json_path + "/resource-config.json";
    if (new File(agent_res).exists()) {
      JFLog.log("Using:" + agent_res);
      cmd.add("-H:ResourceConfigurationFiles=" + agent_res);
    }

    String agent_reflect = json_path + "/reflect-config.json";
    if (new File(agent_reflect).exists()) {
      JFLog.log("Using:" + agent_reflect);
      cmd.add("-H:ReflectionConfigurationFiles=" + agent_reflect);
    }

    String app_res = json_path + "/app-resource-config.json";
    if (new File(app_res).exists()) {
      JFLog.log("Using:" + app_res);
      cmd.add("-H:ResourceConfigurationFiles=" + app_res);
    }

//    cmd.add("-H:TempDirectory=temp");
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
