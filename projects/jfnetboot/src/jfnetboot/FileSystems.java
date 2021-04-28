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
        fos.write("#default filesystem\narch=arm\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    if (!new File(Paths.filesystems + "/default-x86.cfg").exists()) {
      try {
        FileOutputStream fos = new FileOutputStream(Paths.filesystems + "/default-x86.cfg");
        fos.write("#default filesystem\narch=x86\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    if (!new File(Paths.filesystems + "/default-bios.cfg").exists()) {
      try {
        FileOutputStream fos = new FileOutputStream(Paths.filesystems + "/default-bios.cfg");
        fos.write("#default filesystem\narch=bios\n".getBytes());
        fos.close();
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
    ArrayList<FileSystem> derived = new ArrayList();
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
      FileSystem fs = FileSystem.load(name, arch);
      if (fs.canIndex()) {
        fs.index();
        map.put(fs.name + "-" + fs.arch, fs);
      } else {
        derived.add(fs);
      }
    }
    //now load derived file systems
    while (derived.size() > 0) {
      int before_size = derived.size();
      for(int a=0;a<derived.size();a++) {
        FileSystem fs = derived.get(a);
        if (fs.canIndex()) {
          fs.index();
          map.put(fs.name + "-" + fs.arch, fs);
          derived.remove(a);
          break;
        }
      }
      int after_size = derived.size();
      if (before_size == after_size) {
        JFLog.log("Error:Unable to init all file systems");
        JFLog.log("Reason:either a base file system was deleted or a circular dependancy was created some how");
        System.exit(1);
      }
    }
  }

  public static boolean create(String name, String arch, String derived_from) {
    if (map.get(name + "-" + arch) != null) {
      return false;
    }
    FileSystem fs;
    if (derived_from == null) {
      fs = new FileSystem(name, arch);
    } else {
      FileSystem base = map.get(derived_from + "-" + arch);
      if (base == null) return false;
      fs = new FileSystem(Paths.filesystems + "/" + name, name, base);
    }
    fs.index();
    map.put(name + "-" + arch, fs);
    return true;
  }

  public static boolean create(String name, String arch) {
    return FileSystems.create(name, arch, null);
  }

  public static boolean remove(String name, String arch) {
    if (name.equals("default")) {
      return false;
    }
    if (Clients.isFileSystemInUse(name)) {
      return false;
    }
    for(FileSystem other : map.values()) {
      if (other.derived_from == null) continue;
      if (other.derived_from.equals(name) && other.arch.equals(arch)) {
        //is base to a derived filesystem
        return false;
      }
    }
    String key = name + "-" + arch;
    map.remove(key);
    return true;
  }

}
