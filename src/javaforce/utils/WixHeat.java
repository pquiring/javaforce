package javaforce.utils;

/**
 * Heat for JavaForce.
 *
 * Builds Wix files like heat does.
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class WixHeat {
  private static void usage() {
    System.out.println("Usage:WixHeat input_folder output_xml resource_name base_folder [files...]");
    System.exit(1);
  }

  private static class Entry {
    public String path, file;
  }

  private static StringBuilder out;
  private static String input_folder;
  private static String output_xml;
  private static String base_folder;
  private static String res_name;
  private static ArrayList<String> files;
  private static ArrayList<PathMatcher> matches;
  private static ArrayList<String> wixfolders;
  private static ArrayList<Entry> wixfiles;
  private static String dir;
  private static String cmp;

  private static void outHeader() {
//    out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    out.append("<Wix xmlns=\"http://wixtoolset.org/schemas/v4/wxs\">\n");
  }

  private static void addFolder(String parent, String path) throws Exception {
    if (path.startsWith("/")) path = path.substring(1);
    String fullpath = input_folder + path;
    File folder = new File(fullpath);
    File[] files = folder.listFiles();
    if (files == null || files.length == 0) return;
    int idx = path.lastIndexOf("/");
    if (idx == -1) idx = 0; else idx++;
    int cnt = 0;
    for(int a=0;a<files.length;a++) {
      String name = files[a].getName();
      if (files[a].isDirectory()) {
        if (files[a].getName().equals("jmods")) continue;  //skip jmods folder
        if (files[a].getName().equals("legal")) continue;  //skip legal folder
        addFolder(path, path + "/" + name);
      } else {
        //check if file matches wildcard list
        boolean matched = false;
        for(int m=0;m<matches.size();m++) {
          PathMatcher matcher = matches.get(m);
          if (matcher.matches(files[a].toPath())) {
            matched = true;
            break;
          }
        }
        if (!matched) {
          continue;
        }
        Entry e = new Entry();
        e.path = path;
        e.file = name;
        wixfiles.add(e);
        cnt++;
      }
    }
    if (cnt == 0) return;
    outFolder(parent, path.substring(idx), path);
    wixfolders.add(path);
  }

  private static void outFolder(String parent, String name, String path) {
    if (name.equals("")) name = base_folder;  //base folder
    String id = path.replaceAll("/", "_");
    String did = dir + id;
    String pid = parent.replaceAll("/", "_");
    String dpid = dir + pid;
    if (parent.equals("APPLICATIONROOTDIRECTORY")) {
      dpid = "APPLICATIONROOTDIRECTORY";
    }
    out.append("<Fragment>\n");
    out.append("  <DirectoryRef Id=\"" + dpid + "\">\n");
    out.append("    <Directory Id=\"" + did + "\" Name=\"" + name + "\" FileSource=\"" + (input_folder + path).replaceAll("/", "\\\\") + "\" />\n");
    out.append("  </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static int baseguid;
  private static int guid;
  private static int fileid;

  private static void outFiles(String path) {
    String id = path.replaceAll("/", "_");
    String did = dir + id;
    String cid = cmp + id;

    out.append("<Fragment>\n");
    out.append("    <DirectoryRef Id=\"" + did + "\">\n");
    out.append("        <Component Id=\"" + cid + "\" Guid=\"{8A8E15CB-3AA6-4D96-AD6D-5241AD" + Integer.toHexString(baseguid) + guid++ + "}\" Bitness=\"always64\">\n");
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
      String fid = dir + "_" + fileid++;
      switch (src) {
        case "bin/java.exe": fid = "java_exe"; break;
        case "bin/javaw.exe": fid = "javaw_exe"; break;
      }
      out.append("<File Id=\"" + fid + "\" Source=\"" + src + "\" />\n");
    }
    out.append("        </Component>\n");
    out.append("    </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static void outTrailer() {
    out.append("<Fragment>\n");
    out.append("  <ComponentGroup Id=\"" + res_name + "\">\n");
    for(int a=0;a<wixfolders.size();a++) {
      String path = wixfolders.get(a).replaceAll("/", "_");
      out.append("    <ComponentRef Id=\"" + cmp + path + "\" />\n");
    }
    out.append("  </ComponentGroup>\n");
    out.append("</Fragment>\n");
    out.append("</Wix>\n");
  }

  public static void main(String[] args) {
    if (args.length < 4) {
      usage();
    }
    //create jvm.xml
    out = new StringBuilder();
    input_folder = args[0].replaceAll("\\\\", "/");
    output_xml = args[1].replaceAll("\\\\", "/");
    res_name = args[2];
    base_folder = args[3].replaceAll("\\\\", "/");

    files = new ArrayList<String>();
    matches = new ArrayList<PathMatcher>();
    wixfolders = new ArrayList<String>();
    wixfiles = new ArrayList<Entry>();

    baseguid = 0;
    guid = 1000;
    fileid = 0;

    for(int a=4;a<args.length;a++) {
      files.add(args[a]);
    }
    if (args.length == 4) {
      files.add("**");
    }
    FileSystem fs = FileSystems.getDefault();
    for(int a=0;a<files.size();a++) {
      matches.add(fs.getPathMatcher("glob:" + files.get(a)));
    }
    char ca[] = res_name.toCharArray();
    for(int a=0;a<ca.length;a++) {
      baseguid += ca[a];
    }
    baseguid &= 0xff;
    if (baseguid < 0x10) {
      baseguid |= 0x10;
    }
    if (!input_folder.endsWith("/")) {
      input_folder += "/";
    }
    cmp = "cmp_" + res_name + "_";
    dir = "dir_" + res_name + "_";
    try {
      System.out.println("WixHeat : input_folder=" + input_folder);
      outHeader();

      addFolder("APPLICATIONROOTDIRECTORY", "");

      for(int a=0;a<wixfolders.size();a++) {
        outFiles(wixfolders.get(a));
      }

      outTrailer();

      FileOutputStream fos = new FileOutputStream(output_xml);
      fos.write(out.toString().getBytes());
      fos.close();

      System.out.println("WixHeat : " + output_xml + " created");

    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
