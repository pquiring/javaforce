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
  private static boolean use_javaforce_config = true;  //graal needs to be told about these
  private static boolean use_agent_config = false;  //these should be loaded into the jar file
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
    cmd.add("--no-fallback");

    //add JF config files

    if (use_javaforce_config) {
      JFLog.log("Using:META-INF/native-image/javaforce-jni-config.json");
      cmd.add("-H:JNIConfigurationFiles=" + json_path + "/javaforce-jni-config.json");
    }

    if (use_javaforce_config) {
      JFLog.log("Using:META-INF/native-image/javaforce-resource-config.json");
      cmd.add("-H:ResourceConfigurationFiles=" + json_path + "/javaforce-resource-config.json");
    }

    //add graal agent config files

    if (use_agent_config) {
      String json = json_path + "/jni-config.json";
      if (new File(json).exists()) {
        JFLog.log("Using:" + json);
        cmd.add("-H:JNIConfigurationFiles=" + json);
      }
    }

    if (use_agent_config) {
      String json = json_path + "/reflect-config.json";
      if (new File(json).exists()) {
        JFLog.log("Using:" + json);
        cmd.add("-H:ReflectionConfigurationFiles=" + json);
      }
    }

    if (use_agent_config) {
      String json = json_path + "/resource-config.json";
      if (new File(json).exists()) {
        JFLog.log("Using:" + json);
        cmd.add("-H:ResourceConfigurationFiles=" + json);
      }
    }

    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
