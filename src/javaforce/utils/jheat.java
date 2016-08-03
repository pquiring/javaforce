package javaforce.utils;

/**
 * Heat for JavaForce.
 *
 * Builds Wix files like heat does.
 *
 * Work in progress, not complete.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class jheat {
  private static void usage() {
    System.out.println("Usage:jheat JAVA_HOME");
    System.exit(1);
  }

  private static class Entry {
    public String path, file;
  }

  private static StringBuilder out;
  private static String java_home;
  private static ArrayList<String> wixfolders = new ArrayList<String>();
  private static ArrayList<Entry> wixfiles = new ArrayList<Entry>();
  private static boolean win64;

  private static void outHeader() {
    out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    out.append("<Wix xmlns=\"http://schemas.microsoft.com/wix/2006/wi\">\n");
  }

  private static void addFolder(String parent, String path) throws Exception {
    if (path.startsWith("/")) path = path.substring(1);
    String fullpath = java_home + path;
//    System.out.println("Path:" + fullpath);
    File folder = new File(fullpath);
    File files[] = folder.listFiles();
    if (files == null || files.length == 0) return;
    int idx = path.lastIndexOf("/");
    if (idx == -1) idx = 0; else idx++;
    outFolder(parent, path.substring(idx), path);
    wixfolders.add(path);
    for(int a=0;a<files.length;a++) {
      String name = files[a].getName();
      if (files[a].isDirectory()) {
        addFolder(path, path + "/" + name);
      } else {
//        System.out.println("File:" + path + ":" + name);
        Entry e = new Entry();
        e.path = path;
        e.file = name;
        wixfiles.add(e);
      }
    }
  }

  private static void outFolder(String parent, String name, String path) {
    if (name.equals("")) name = "jre";  //base folder
    String id = path.replaceAll("/", "_");
    String did = "dir_" + id;
    String pid = parent.replaceAll("/", "_");
    String dpid = "dir_" + pid;
    if (parent.equals("APPLICATIONROOTDIRECTORY")) {
      dpid = "APPLICATIONROOTDIRECTORY";
    }
    out.append("<Fragment>\n");
    out.append("  <DirectoryRef Id=\"" + dpid + "\">\n");
    out.append("    <Directory Id=\"" + did + "\" Name=\"" + name + "\" FileSource=\"" + (java_home + path).replaceAll("/", "\\\\") + "\" />\n");
    out.append("  </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static int guid = 1000;
  private static int fileid;

  private static void outFiles(String path) {
    String id = path.replaceAll("/", "_");
    String did = "dir_" + id;
    String cid = "cmp_" + id;

    out.append("<Fragment>\n");
    out.append("    <DirectoryRef Id=\"" + did + "\">\n");
    out.append("        <Component Id=\"" + cid + "\" Guid=\"{8A8E15CB-3AA6-4D96-AD6D-5241AD6E" + guid++ + "}\"" + (win64 ? " Win64=\"yes\"" : "") + ">\n");
    int cnt = wixfiles.size();
    for(int a=0;a<cnt;a++) {
      Entry e = wixfiles.get(a);
      if (!e.path.equals(path)) continue;
      String src;
      if (e.path.length() == 0) {
        src = e.file;
      } else {
        src = e.path + "/" + e.file;
      }
      out.append("<File Id=\"_" + fileid++ + "\" Source=\"" + src + "\" />\n");
    }
    out.append("        </Component>\n");
    out.append("    </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static void outTrailer() {
    out.append("<Fragment>\n");
    out.append("  <ComponentGroup Id=\"JRE\">\n");
    for(int a=0;a<wixfolders.size();a++) {
      String path = wixfolders.get(a).replaceAll("/", "_");
      out.append("    <ComponentRef Id=\"cmp_" + path + "\" />\n");
    }
    out.append("  </ComponentGroup>\n");
    out.append("</Fragment>\n");
    out.append("</Wix>\n");
  }

  private static void loadProperties() {
    try {
      Properties props = new Properties();
      FileInputStream fis = new FileInputStream("jre.properties");
      props.load(fis);
      //TODO - any config options
      fis.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  public static void main(String args[]) {
    if (args.length == 0) {
      usage();
    }
    loadProperties();
    //create jvm.xml
    out = new StringBuilder();
    java_home = args[0].replaceAll("\\\\", "/");
    if (args.length > 1 && args[1].equals("win64")) {
      win64 = true;
    }
    if (!java_home.endsWith("/")) {
      java_home += "/";
    }
    try {
      File test = new File(java_home + "release");
      if (!test.exists()) throw new Exception("JAVA_HOME not valid:" + java_home);

      outHeader();

      addFolder("APPLICATIONROOTDIRECTORY", "");

      for(int a=0;a<wixfolders.size();a++) {
        outFiles(wixfolders.get(a));
      }

      outTrailer();

      FileOutputStream fos = new FileOutputStream("jre.xml");
      fos.write(out.toString().getBytes());
      fos.close();

      System.out.println("jheat : jre.xml created");

    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
