package javaforce.utils;

import java.util.*;

import javaforce.*;

/**
 * Just calls 'fuseiso'.
 *
 * @author pquiring
 *
 * Created : Feb 8, 2014
 */

public class jfuseiso {
  public boolean auth(String args[], String pass) {
    //iso never need auth
    return true;
  }
  public void start(String args[]) {
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("fuseiso");
    cmd.add("-n");  //do NOT maintain $HOME/.mtab.fuseiso
    for(int a=0;a<args.length;a++) {
      cmd.add(args[a]);
    }
    cmd.add("-f");  //foreground
    try {
      ProcessBuilder pb = new ProcessBuilder();
      pb.command(cmd);
      Process p = pb.start();
      p.waitFor();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public static void main(String args[]) {
    if (args.length == 0) {
      System.out.println("Usage : jfuse-iso iso_file mount");
      return;
    }
    new jfuseiso().main2(args);
  }
  public void main2(String args[]) {
    try {
      start(args);
    } catch (Exception e) {
      JFLog.log("Error:" + e);
    }
  }
}
