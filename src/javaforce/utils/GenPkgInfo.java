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
  private static void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  private static XML xml;
  private static String app, desc, arch, ver;
  private static long size;  //in bytes
  private static boolean graal;

  public static void main(String args[]) {
    if (args == null || args.length < 3) {
      System.out.println("genpkginfo : build linux package info files");
      System.out.println("  Usage : jpkginfo distro archtype files.list");
      System.out.println("    distro = debian fedora");
      System.exit(1);
    }
    String distro = args[0];
    arch = getArch();
    size = calcSize(args[2]);
    //load build.xml and extract app , desc , etc.
    xml = loadXML();
    app = getProperty("app");
    desc = getTag("description");
    ver = getProperty("version");
    graal = getProperty("graal").equals("true");
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
    return arch;
  }

  private static long calcSize(String files_list) {
    try {
      String files[] = new String(JF.readAll(new FileInputStream(files_list))).replaceAll("\r", "").split("\n");
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

  private static XML loadXML() {
    XML xml = new XML();
    try {
      xml.read(new FileInputStream("build.xml"));
      return xml;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  private static String getTag(String name) {
    XML.XMLTag tag = xml.getTag(new String[] {"project", name});
    if (tag == null) return "";
    return tag.content;
  }

  private static String getProperty(String name) {
    //<project> <property name="name" value="value">
    int cnt = xml.root.getChildCount();
    for(int a=0;a<cnt;a++) {
      XML.XMLTag tag = xml.root.getChildAt(a);
      if (!tag.name.equals("property")) continue;
      int attrs = tag.attrs.size();
      String attrName = null;
      String attrValue = null;
      for(int b=0;b<attrs;b++) {
        XML.XMLAttr attr = tag.attrs.get(b);
        if (attr.name.equals("name")) {
          attrName = attr.value;
        }
        if (attr.name.equals("value")) {
          attrValue = attr.value;
        }
      }
      if (attrName != null && attrName.equals(name)) {
        return attrValue;
      }
    }
    return "";
  }

  private static String[] getDepends(String tagName) {
    ArrayList<String> depends = new ArrayList<String>();
    String list[] = getProperty(tagName).split(",");
    if (!graal) {
      if (!app.equals("javaforce")) depends.add("javaforce");
    }
    for(int a=0;a<list.length;a++) {
      String depend = list[a].trim();
      if (depend.length() == 0) continue;
      depends.add(depend);
    }
    return depends.toArray(new String[0]);
  }

  private static void debian() {
    arch = convertArch(arch);
    try {
      StringBuffer sb = new StringBuffer();
      //mandatory
      sb.append("Package: " + app + "\n");
      sb.append("Version: " + ver + "\n");
      sb.append("Architecture: ");
      sb.append(arch);
      sb.append("\n");
      sb.append("Description: " + desc + "\n");
      sb.append("Maintainer: Peter Quiring <pquiring@gmail.com>\n");
      //optional
      sb.append("Installed-Size: " + Long.toString(size / 1024L) + "\n");
      sb.append("Depends: ");
      String depends[] = getDepends("debian.depends");
      for(int a=0;a<depends.length;a++) {
        if (a > 0) sb.append(",");
        sb.append(depends[a]);
      }
      sb.append("\n");
      new File("deb").mkdir();
      {
        FileOutputStream fos = new FileOutputStream("deb/control");
        fos.write(sb.toString().getBytes());
        fos.close();
      }
      //generate deb/postinst if not present
      if (!new File("deb/postinst").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postinst");
        if (graal) {
          fos.write("#!/bin/sh\n".getBytes());
        } else {
          fos.write("#!/bin/sh\nset -e\nupdate-desktop-database\n".getBytes());
        }
        fos.close();
      }
      //generate deb/postrm if not present
      if (!new File("deb/postrm").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postrm");
        if (graal) {
          fos.write("#!/bin/sh\n".getBytes());
        } else {
          fos.write("#!/bin/sh\nset -e\nupdate-desktop-database\n".getBytes());
        }
        fos.close();
      }
      System.out.println("Debian package info created");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void fedora() {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("Buildroot: /.\n");
      sb.append("Name: " + app + "\n");
      sb.append("Version: " + ver + "\n");
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
      sb.append("Requires: ");
      String[] depends = getDepends("fedora.depends");
      for(int a=0;a<depends.length;a++) {
        if (a > 0) sb.append(",");
        sb.append(depends[a]);
      }
      sb.append("\n");
      sb.append("%description\n " + desc + "\n");
      sb.append("%post\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("update-desktop-database\n");
      sb.append("%pre\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("update-desktop-database\n");
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

  private static void arch() {
    try {
      StringBuffer sb = new StringBuffer();
      sb.append("pkgname = " + app + "\n");
      sb.append("pkgver = " + ver + "-1\n");
      sb.append("pkgdesc = " + desc + "\n");
      sb.append("builddate = " + Long.toString(Calendar.getInstance().getTimeInMillis() / 1000L) + "\n");
      sb.append("packager = Peter Quiring <pquiring@gmail.com>\n");
      sb.append("size = " + size + "\n");
      sb.append("license = LGPL\n");
      sb.append("arch = ");
      sb.append(arch);
      sb.append("\n");
      String[] depends = getDepends("arch.depends");
      for(int a=0;a<depends.length;a++) {
        sb.append("depend = ");
        sb.append(depends[a]);
        sb.append("\n");
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
