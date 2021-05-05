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
    cmd.add("-H:JNIConfigurationFiles=graal_jni.json");
    String app_jni = args[1] + "_jni.json";
    if (new File(app_jni).exists()) {
      JFLog.log("Using:" + app_jni);
      cmd.add("-H:JNIConfigurationFiles=" + app_jni);
    }
    cmd.add("-H:ResourceConfigurationFiles=graal_res.json");
    String app_res = args[1] + "_res.json";
    if (new File(app_res).exists()) {
      JFLog.log("Using:" + app_res);
      cmd.add("-H:ResourceConfigurationFiles=" + app_res);
    }
//    cmd.add("-H:Log=registerResource");
//    cmd.add("-H:+TraceNativeToolUsage");
    cmd.add("--shared");
    cmd.add("--enable-all-security-services");
    cmd.add("--allow-incomplete-classpath");
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
