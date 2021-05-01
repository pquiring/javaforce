package jfnetboot;

/** File Systems
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class FileSystems {
  private static HashMap<String, FileSystem> map = new HashMap<>();

  public static int getCount() {
    return map.size();
  }

  public static FileSystem get(String name, String arch) {
    return map.get(name + "-" + arch);
  }

  public static FileSystem[] getFileSystems() {
    return map.values().toArray(new FileSystem[0]);
  }

  public static String[] getFileSystemNames() {
    ArrayList<String> names = new ArrayList<>();
    FileSystem[] fss = getFileSystems();
    for(FileSystem fs : fss) {
      if (names.contains(fs.name)) continue;
      names.add(fs.name);
    }
    return names.toArray(new String[names.size()]);
  }

  public static void init() {
    if (!new File(Paths.filesystems + "/default-arm.cfg").exists()) {
      try {
        FileOutputStream fos = new FileOutputStream(Paths.filesystems + "/default-arm.cfg");
        fos.write("#filesystem\nname=default\narch=arm\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    if (!new File(Paths.filesystems + "/default-x86.cfg").exists()) {
      try {
        FileOutputStream fos = new FileOutputStream(Paths.filesystems + "/default-x86.cfg");
        fos.write("#filesystem\nname=default\narch=x86\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    if (!new File(Paths.filesystems + "/default-bios.cfg").exists()) {
      try {
        FileOutputStream fos = new FileOutputStream(Paths.filesystems + "/default-bios.cfg");
        fos.write("#filesystem\nname=default\narch=bios\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    File[] files = new File(Paths.filesystems).listFiles();
    for(File file : files) {
      if (file.isDirectory()) continue;
      String name = file.getName();
      if (!name.endsWith(".cfg")) continue;
      name = name.substring(0, name.length() - 4);
      int idx = name.indexOf("-");
      if (idx == -1) continue;
      String arch = name.substring(idx + 1);
      name = name.substring(0, idx);
      JFLog.log("FileSystem:" + name + "-" + arch);
      FileSystem fs = new FileSystem(name, arch);
      map.put(fs.name + "-" + fs.arch, fs);
    }
  }

  public static boolean create(String name, String arch) {
    if (map.get(name + "-" + arch) != null) {
      return false;
    }
    FileSystem fs = new FileSystem(name, arch);
    map.put(name + "-" + arch, fs);
    return true;
  }

  public static void add(FileSystem fs) {
    map.put(fs.name + "-" + fs.arch, fs);
  }

  public static boolean remove(String name, String arch) {
    if (name.equals("default")) {
      return false;
    }
    if (Clients.isFileSystemInUse(name)) {
      return false;
    }
    String key = name + "-" + arch;
    map.remove(key);
    return true;
  }

}
