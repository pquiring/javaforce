package javaforce.utils;

/** genpkginfo
 *
 * Generates Linux package info files.
 *
 * debian : deb/control
 * fedora : rpm.spec
 * arch : .PKGINFO
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class GenPkgInfo {
  private void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  private BuildTools tools;
  private String app, apptype, desc, arch, ver, deps;
  private long size;  //in bytes

  public static void main(String[] args) {
    if (args == null || args.length < 4) {
      System.out.println("GenPkgInfo : build linux package info files");
      System.out.println("  Usage : GenPkgInfo distro archtype files.list depends");
      System.out.println("    distro = debian fedora arch");
      System.exit(1);
    }
    try {
      new GenPkgInfo().run(args);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void run(String[] args) throws Exception {
    tools = new BuildTools();
    if (!tools.loadXML("build.xml")) throw new Exception("error loading build.xml");
    String distro = args[0];
    arch = getArch();
    size = calcSize(args[2]);
    deps = args[3];
    //load build.xml and extract app , desc , etc.
    app = tools.getProperty("app");
    apptype = tools.getProperty("apptype");
    switch (apptype) {
      case "client":
      case "server":
        apptype = "-" + apptype;
        break;
      default:
        apptype = "";
        break;
    }
    desc = tools.getTag("description");
    ver = tools.getProperty("version");
    switch (distro) {
      case "debian": debian(); break;
      case "fedora": fedora(); break;
      case "arch": arch(); break;
      default: error("Unknown distro:" + distro);
    }
  }

  public static String convertArch(String arch) {
    switch (arch) {
      case "x86_64": return "amd64";
      case "aarch64": return "arm64";
    }
    return arch;
  }

  public static String getArch() {
    String arch = System.getenv("HOSTTYPE");
    if (arch == null) {
      arch = System.getProperty("os.arch");
      if (arch == null) {
        JFLog.log("Error:Unable to detect CPU from env:HOSTTYPE or property:os.arch");
      }
    }
    //use GNU names
    switch (arch) {
      case "amd64": return "x86_64";
      case "arm64": return "aarch64";
    }
    return arch;
  }

  private long calcSize(String files_list) {
    try {
      String[] files = new String(JF.readAll(new FileInputStream(files_list))).replaceAll("\r", "").split("\n");
      long size = 0;
      for(int a=0;a<files.length;a++) {
        size += new File(files[a]).length();
      }
      return size;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return -1;
    }
  }

  private String[] getDepends() {
    ArrayList<String> depends = new ArrayList<String>();
    String[] list = deps.split(",");
    for(int a=0;a<list.length;a++) {
      String depend = list[a].trim();
      if (depend.length() == 0) continue;
      if (depend.equals("null")) continue;
      depends.add(depend);
    }
    if (!app.equals("javaforce") && !depends.contains("javaforce")) {
      depends.add("javaforce");
    }
    return depends.toArray(JF.StringArrayType);
  }

  private void debian() {
    arch = convertArch(arch);
    try {
      StringBuffer sb = new StringBuffer();
      //mandatory
      sb.append("Package: " + app + apptype + "\n");
      sb.append("Version: " + ver + "\n");
      sb.append("Architecture: ");
      sb.append(arch);
      sb.append("\n");
      sb.append("Description: " + desc + "\n");
      sb.append("Maintainer: Peter Quiring <pquiring@gmail.com>\n");
      //optional
      sb.append("Installed-Size: " + Long.toString(size / 1024L) + "\n");
      if (!deps.equals("null")) {
        sb.append("Depends: ");
        String[] depends = getDepends();
        for(int a=0;a<depends.length;a++) {
          if (a > 0) sb.append(",");
          sb.append(depends[a]);
        }
        sb.append("\n");
      }
      new File("deb").mkdir();
      {
        FileOutputStream fos = new FileOutputStream("deb/control");
        fos.write(sb.toString().getBytes());
        fos.close();
      }

      //generate deb/preinst if not present
      if (!new File("deb/preinst").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/preinst");
        fos.write("#!/bin/sh\n".getBytes());
        if (new File("folders.lst").exists()) {
          FileInputStream fis = new FileInputStream("folders.lst");
          String[] lns = new String(fis.readAllBytes()).replaceAll("\\r", "").split("\n");
          fis.close();
          for(String ln : lns) {
            ln = ln.trim();
            if (ln.length() == 0) continue;
            fos.write(("mkdir -p " + ln + "\n").getBytes());
          }
        }
        fos.close();
      }
//install package here
      //generate deb/postinst if not present
      if (!new File("deb/postinst").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postinst");
        fos.write("#!/bin/sh\n".getBytes());
        fos.close();
      }

      //generate deb/prerm if not present
      if (!new File("deb/prerm").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postinst");
        fos.write("#!/bin/sh\n".getBytes());
        fos.close();
      }
//remove package here
      //generate deb/postrm if not present
      if (!new File("deb/postrm").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postrm");
        fos.write("#!/bin/sh\n".getBytes());
        fos.close();
      }

      System.out.println("Debian package info created");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void fedora() {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("Buildroot: /.\n");
      sb.append("Name: " + app + apptype + "\n");
      sb.append("Version: " + ver.replaceAll("-", "_") + "\n");
      sb.append("Release: 1\n");
      sb.append("License: LGPL\n");
/*
      sb.append("Architecture: ");
      switch (archtype) {
        case "any": sb.append("noarch"); break;
        case "x32": sb.append("i686"); break;
        case "x64": sb.append("x86_64"); break;
        case "a32": sb.append("armv7hl"); break;
        case "a64": sb.append("aarch64"); break;
      }
      sb.append("\n");
*/
      sb.append("Summary: " + desc + "\n");
      if (!deps.equals("null")) {
        sb.append("Requires: ");
        String[] depends = getDepends();
        for(int a=0;a<depends.length;a++) {
          if (a > 0) sb.append(",");
          sb.append(depends[a]);
        }
        sb.append("\n");
      }
      sb.append("%description\n " + desc + "\n");
      sb.append("%post\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("%pre\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("%files\n");
      //files.lst is added by jfrpm
      FileOutputStream fos = new FileOutputStream("rpm.spec");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("Fedora package info created");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private void arch() {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("pkgname = " + app + apptype + "\n");
      sb.append("pkgver = " + ver + "-1\n");
      sb.append("pkgdesc = " + desc + "\n");
      sb.append("builddate = " + Long.toString(Calendar.getInstance().getTimeInMillis() / 1000L) + "\n");
      sb.append("packager = Peter Quiring <pquiring@gmail.com>\n");
      sb.append("size = " + size + "\n");
      sb.append("license = LGPL\n");
      sb.append("arch = ");
      sb.append(arch);
      sb.append("\n");
      if (!deps.equals("null")) {
        String[] depends = getDepends();
        for(int a=0;a<depends.length;a++) {
          sb.append("depend = ");
          sb.append(depends[a]);
          sb.append("\n");
        }
      }
      FileOutputStream fos = new FileOutputStream(".PKGINFO");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("Arch package info created");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
