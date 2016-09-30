package javaforce.utils;

/** jpkginfo
 *
 * Generates Linux package info files.
 *
 * ubuntu : deb/control
 * fedora : rpm.spec
 * arch : .PKGINFO
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class jpkginfo {
  private static void error(String msg) {
    System.out.print(msg);
    System.exit(1);
  }

  private static XML xml;
  private static String app, desc, archtype, ver, size;

  public static void main(String args[]) {
    if (args == null || args.length < 3) {
      System.out.println("jpkginfo : build linux package info files");
      System.out.println("  Usage : jpkginfo distro archtype files.list");
      System.out.println("    distro = ubuntu fedora arch");
      System.out.println("    archtype = x32 x64 a32 a64");
      System.exit(1);
    }
    String distro = args[0];
    archtype = args[1];
    size = calcSize(args[2]);
    //load build.xml and extract app , desc , etc.
    xml = loadXML();
    app = getProperty("app");
    desc = getTag("description");
    app = getProperty("version");
    switch (distro) {
      case "ubuntu": ubuntu(); break;
      case "fedora": fedora(); break;
      case "arch": arch(); break;
      default: error("Unknown distro:" + distro);
    }
  }

  private static String calcSize(String files_list) {
    try {
      String files[] = new String(JF.readAll(new FileInputStream(files_list))).replaceAll("\r", "").split("\n");
      long size = 0;
      for(int a=0;a<files.length;a++) {
        size += new File(files[a]).length();
      }
      return Long.toString(size / 1024L);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return null;
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
      for(int b=0;b<attrs;b++) {
        if (tag.attrs.get(b).name.equals(name)) {
          return tag.attrs.get(b).value;
        }
      }
    }
    return null;
  }

  private static String getDepends(String tagName, String fieldName) {
    String depends = getTag(tagName);
    if (depends.length() > 0) depends = "," + depends;
    if (!app.equals("javaforce")) depends = "javaforce" + ((depends.length() > 0) ? "," : "") + depends;
    if (depends.length() > 0) return fieldName + depends + "\n"; else return "";
  }

  private static void ubuntu() {
    try {
      StringBuffer sb = new StringBuffer();
      //mandatory
      sb.append("Package: " + app + "\n");
      sb.append("Version: " + ver + "\n");
      sb.append("Architecture: ");
      switch (archtype) {
        case "any": sb.append("noarch"); break;
        case "x32": sb.append("i386"); break;
        case "x64": sb.append("amd64"); break;
        case "a32": sb.append("armhf"); break;
        case "a64": sb.append("aarch64"); break;
      }
      sb.append("\n");
      sb.append("Description: " + desc + "\n");
      sb.append("Maintainer: Peter Quiring <pquiring@gmail.com>");
      //optional
      sb.append("Installed-Size: " + size + "\n");
      sb.append(getDepends("ubuntu.depends", "Depends: "));
      new File("deb").mkdir();
      {
        FileOutputStream fos = new FileOutputStream("deb/control");
        fos.write(sb.toString().getBytes());
        fos.close();
      }
      //generate deb/postinst if not present
      if (!new File("deb/postinst").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postinst");
        fos.write("#!/bin/sh\nset -e\nupdate-desktop-database\n".getBytes());
        fos.close();
      }
      //generate deb/postrm if not present
      if (!new File("deb/postrm").exists()) {
        FileOutputStream fos = new FileOutputStream("deb/postrm");
        fos.write("#!/bin/sh\nset -e\nupdate-desktop-database\n".getBytes());
        fos.close();
      }
      System.out.println("Ubuntu package info created");
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
      sb.append("Architecture: ");
      switch (archtype) {
        case "any": sb.append("noarch"); break;
        case "x32": sb.append("i686"); break;
        case "x64": sb.append("x86_64"); break;
        case "a32": sb.append("armv7hl"); break;
        case "a64": sb.append("aarch64"); break;
      }
      sb.append("\n");
      sb.append("Summary: " + desc + "\n");
      sb.append(getDepends("fedora.depends", "Requires: "));
      sb.append("%description\n " + desc + "\n");
      sb.append("%post\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("update-desktop-database\n");
      sb.append("%pre\n");
      sb.append("#!/bin/sh\n");
      sb.append("set -e\n");
      sb.append("update-desktop-database\n");
      //%files is added by jfrpm
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
      sb.append("pkgver = " + ver + "\n");
      sb.append("pkgdesc = " + desc + "\n");
      sb.append("builddate = " + Long.toString(Calendar.getInstance().getTimeInMillis() / 1000L));
      sb.append("packager = Peter Quiring <pquiring@gmail.com>\n");
      sb.append("size = " + size + "\n");
      sb.append("license = LGPL\n");
      sb.append("arch = ");
      switch (archtype) {
        case "any": sb.append("any"); break;
        case "x32": sb.append("i686"); break;
        case "x64": sb.append("x86_64"); break;
        case "a32": sb.append("arm"); break;
        case "a64": sb.append("aarch64"); break;
      }
      sb.append("\n");
      sb.append(getDepends("arch.depends", "depend = "));
      FileOutputStream fos = new FileOutputStream("pac.info");
      fos.write(sb.toString().getBytes());
      fos.close();
      System.out.println("Arch package info created");
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
