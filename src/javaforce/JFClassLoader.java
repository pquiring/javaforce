package javaforce;

/** Class Loader from a ClassPath.
 *
 * You could use URLClassLoader with file://... but this is easier.
 *
 * see https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/jdk/internal/loader/BuiltinClassLoader.java
 * see https://github.com/apache/tomcat/blob/main/java/org/apache/catalina/loader/WebappClassLoaderBase.java
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class JFClassLoader extends ClassLoader {
  private static class Folder {
    HashMap<String, byte[]> files = new HashMap<>();
  };
  private ArrayList<Folder> cp_folders = new ArrayList<>();
  private HashMap<String, Folder> cp_files = new HashMap<>();

  /** Build a Class Loader from the classpath.
   *
   * classpath may include directories and jar files.
   *
   */
  public JFClassLoader(String[] classpath) {
    File[] files = new File[classpath.length];
    int idx = 0;
    for(String cp : classpath) {
      files[idx++] = new File(cp);
    }
    init(files);
  }

  /** Build a Class Loader from the classpath.
   *
   * classpath may include directories and jar files.
   *
   */
  public JFClassLoader(File[] classpath) {
    init(classpath);
  }

  private void init(File[] classpath) {
    for(File file : classpath) {
      if (!file.exists()) {
        JFLog.log("Error:ClassPath element not found:" + file.getAbsolutePath());
        continue;
      }
      if (file.isDirectory()) {
        Folder folder = new Folder();
        cp_folders.add(folder);
        doFileFolder(file, folder, "");
        continue;
      }
      if (file.getName().endsWith(".jar")) {
        Folder folder = new Folder();
        cp_folders.add(folder);
        doJarFolder(file, folder, "");
        continue;
      }
      JFLog.log("Unknown ClassPath element:" + file);
    }
  }

  private void doFileFolder(File folder, Folder file_folder, String path) {
    try {
      File[] files = folder.listFiles();
      for(File file : files) {
        String name = file.getName();
        if (file.isDirectory()) {
          doFileFolder(file, file_folder, path + "/" + name);
        } else {
          String full;
          if (path.length() == 0) full = name; else full = path + "/" + name;
          FileInputStream fis = new FileInputStream(file);
          byte[] data = fis.readAllBytes();
          fis.close();
          file_folder.files.put(full, data);
          cp_files.put(full, file_folder);
        }
      }
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void doJarFolder(File jar, Folder jar_folder, String path) {
    try {
      ZipInputStream zis = new ZipInputStream(new FileInputStream(jar));
      ZipEntry ze;
      while ((ze = zis.getNextEntry()) != null) {
        String name = ze.getName();  //includes directory path
        if (ze.isDirectory()) {
          continue;
        }
        String full;
        if (path.length() == 0) full = name; else full = path + "/" + name;
        byte[] data = zis.readAllBytes();
        jar_folder.files.put(full, data);
        cp_files.put(full, jar_folder);
      }
      zis.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public Class<?> findClass(String name) throws ClassNotFoundException {
    //try bootloader first
    if (!name.startsWith("javaforce.")) {
      try { return super.findClass(name); } catch (Exception e) {}
    }
    byte[] data = getData(name);
    if (data == null) return null;
    return defineClass(name, data, 0, data.length);
  }

  public Class<?> findClass(String module_name, String name) {
    //try bootloader first
    if (!name.startsWith("javaforce.")) {
      try { return super.findClass(module_name, name); } catch (Exception e) {}
    }
    byte[] data = getData(name);
    if (data == null) return null;
    return defineClass(name, data, 0, data.length);
  }

  private byte[] getData(String name) {
    Folder folder = cp_files.get(name);
    if (folder == null) return null;
    return folder.files.get(name);
  }

  public URL findResource(final String name) {
    return super.findResource(name);
  }

  public Enumeration<URL> findResources(String name) throws IOException {
    return super.findResources(name);
  }

  public URL getResource(String name) {
    return super.getResource(name);
  }

  public Enumeration<URL> getResources(String name) throws IOException {
    return super.getResources(name);
  }

  public InputStream getResourceAsStream(String name) {
    byte[] data = getData(name);
    if (data == null) return super.getResourceAsStream(name);
    return new ByteArrayInputStream(data);
  }

  public Class<?> loadClass(String name) throws ClassNotFoundException {
    return loadClass(name);
  }

  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return loadClass(name, resolve);
  }
}
