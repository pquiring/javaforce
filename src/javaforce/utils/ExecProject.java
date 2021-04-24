package javaforce.utils;

/** ExecProject
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.*;

public class ExecProject implements ShellProcessListener {
  private void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  public static void main(String args[]) {
    if (args == null || args.length < 1) {
      System.out.println("ExecProject : Runs project using java");
      System.out.println("  Usage : ExecProject CLASSPATH MAINCLASS");
      System.exit(1);
    }
    new ExecProject().run(args);
  }

  public void run(String[] args) {
    ShellProcess sp = new ShellProcess();
    sp.addListener(this);
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("java");
    cmd.add("-cp");
    if (JF.isWindows()) {
      cmd.add(args[0]);
    } else {
      cmd.add(args[0].replaceAll("[;]", ":"));
    }
    cmd.add(args[1]);  //MAINCLASS
    sp.run(cmd.toArray(new String[0]), true);
  }

  public void shellProcessOutput(String str) {
    System.out.print(str);
  }
}
