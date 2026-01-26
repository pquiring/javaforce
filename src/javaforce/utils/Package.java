package javaforce.utils;

/** Package everything for current OS.
 *
 * THIS IS A WIP!!!  See TODO's
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.linux.*;

public class Package {
  public static void main(String[] args) {
    if (JF.isWindows()) {
      doWindows();
    } else {
      doLinux();
    }
  }
  private static void doWindows() {
    type = "msi";
    String[] projects = new File("projects").list();
    for(String project : projects) {
      if (!new File("projects/" + project + "/wix64.xml").exists()) continue;
      if (!execute("projects/" + project)) return;
    }
  }
  private static void doLinux() {
    Linux.detectDistro();
    String distro = null;
    String version = null;
    switch (Linux.distro) {
      case Debian: type = "deb"; distro = "debian"; version = Linux.getVersionCodeName(); break;
      case Fedora: type = "rpm"; distro = "fedora"; version = Linux.getVersion(); break;
      case Arch: type = "pac"; distro = "arch"; version = "latest"; break;
    }
    //clean repo (clean.sh)
    if (!clean_repo(distro, version)) {
      JFLog.log("clean repo failed");
      return;
    }
    //clean .class files (force rebuild)
    JF.deletePathEx(".", ".*\\.class$");
    //package javaforce
    if (!execute(".")) return;
    //package utils
    if (!execute("utils")) return;
    //package projects
    String[] projects = new File("projects").list();
    for(String project : projects) {
      if (!execute("projects/" + project)) return;
    }
    package_libs();
  }
  private static boolean clean_repo(String distro, String version) {
    //detect cpu
    String cpu = System.getenv("HOSTTYPE");
    if (cpu == null) return false;
    if (distro.equals("debian")) {
      //debian uses alt names for cpu
      switch (cpu) {
        case "x86_64": cpu = "amd64"; break;
        case "aarch64": cpu = "arm64"; break;
      }
    }
    ProcessBuilder pb = new ProcessBuilder();
    pb = pb.directory(new File("repo/" + distro + "/" + version + "/" + cpu));
    pb = pb.redirectOutput(new File("output.log"));
    ArrayList<String> cmd = new ArrayList<String>();
    cmd.add("/usr/bin/bash");
    cmd.add("clean.sh");
    pb = pb.command(cmd);
    try {
      Process p = pb.start();
      return p.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }
  private static String type;
  private static boolean execute(String folder) {
    ProcessBuilder pb = new ProcessBuilder();
    pb = pb.directory(new File(folder));
    pb = pb.redirectOutput(new File("output.log"));
    ArrayList<String> cmd = new ArrayList<String>();
    if (JF.isWindows()) {
      cmd.add("ant.bat");
    } else {
      cmd.add("ant");
    }
    cmd.add(type);
    pb = pb.command(cmd);
    try {
      Process p = pb.start();
      return p.waitFor() == 0;
    } catch (Exception e) {
      return false;
    }
  }
  private static void package_libs() {
    //TODO
  }
}
