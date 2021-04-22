package javaforce.utils;

/** ExecGraalAgent
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class ExecGraalAgent implements ShellProcessListener {
  private void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  public static void main(String args[]) {
    if (args == null || args.length < 1) {
      System.out.println("ExecGraalAgent : Runs project using Graal agentlib");
      System.out.println("  Usage : ExecGraalAgent CLASSPATH MAINCLASS");
      System.exit(1);
    }
    new ExecGraalAgent().run(args);
  }

  public void run(String[] args) {
    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-agentlib:native-image-agent=config-output-dir=META-INF/native-image");
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(args[0]);  //CLASSPATH
    } else {
      cmd.add(args[0].replaceAll("[;]", ":"));  //CLASSPATH
    }
    cmd.add(args[1]);  //MAINCLASS
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
