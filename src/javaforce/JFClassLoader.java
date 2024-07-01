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
    public Object lock = new Object();
    public HashMap<String, byte[]> files = new HashMap<>();
    public HashMap<String, Class<?>> classes = new HashMap<>();
  };
  private ArrayList<Folder> cp_folders = new ArrayList<>();
  private HashMap<String, Folder> cp_files = new HashMap<>();

  public static boolean debug = false;

  /** Build a Class Loader from the classpath.
   *
   * classpath may include directories and jar files.
   *
   */
  public JFClassLoader(String[] classpath) {
    super(ClassLoader.getPlatformClassLoader());
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
    super(ClassLoader.getPlatformClassLoader());
    init(classpath);
  }

  private void init(File[] classpath) {
    //need to get PlatformClassLoader
    if (debug) {
      JFLog.log("platform = " + ClassLoader.getPlatformClassLoader());
      JFLog.log("system = " + ClassLoader.getSystemClassLoader());
      ClassLoader parent = this.getParent();
      while (parent != null) {
        JFLog.log("parent = " + parent);
        parent = parent.getParent();
      }
    }
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
        Folder jar_folder = new Folder();
        cp_folders.add(jar_folder);
        doJarFolder(file, jar_folder, "");
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
          String full;
          if (path.length() == 0) full = name; else full = path + "/" + name;
          doFileFolder(file, file_folder, full);
        } else if (file.getName().endsWith(".jar")) {
          Folder jar_folder = new Folder();
          cp_folders.add(jar_folder);
          doJarFolder(file, jar_folder, "");
        } else if (file.getName().endsWith(".class")) {
          String full;
          if (path.length() == 0) full = name; else full = path + "/" + name;
          FileInputStream fis = new FileInputStream(file);
          byte[] data = fis.readAllBytes();
          fis.close();
          String cls = convert_class(full);
          if (debug) JFLog.log("class:" + cls);
          file_folder.files.put(cls, data);
          cp_files.put(cls, file_folder);
        } else {
          String full;
          if (path.length() == 0) full = name; else full = path + "/" + name;
          FileInputStream fis = new FileInputStream(file);
          byte[] data = fis.readAllBytes();
          fis.close();
          String cls = convert_resource(full);
          if (debug) JFLog.log("resource:" + cls);
          file_folder.files.put(cls, data);
          cp_files.put(cls, file_folder);
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
        if (name.endsWith(".class")) {
          String full;
          if (path.length() == 0) full = name; else full = path + "/" + name;
          byte[] data = zis.readAllBytes();
          String cls = convert_class(full);
          if (debug) JFLog.log("jar.class:" + cls);
          jar_folder.files.put(cls, data);
          cp_files.put(cls, jar_folder);
        }
      }
      zis.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private String convert_class(String in) {
    in = in.substring(0, in.length() - 6);  //remove .class
    in = in.replaceAll("[/]", ".");
    if (JF.isWindows()) {
      in = in.replaceAll("[\\\\]", ".");
    }
    return in;
  }

  private String convert_resource(String in) {
    if (JF.isWindows()) {
      in = in.replaceAll("[\\\\]", "/");
    }
    return in;
  }

  public Class<?> findClass(String name) throws ClassNotFoundException {
    return findClass(null, name);
  }

  public Class<?> findClass(String module_name, String name) {
    //try bootloader first
    if (!name.startsWith("javaforce.")) {
      try {
        Class<?> cls;
        if (module_name == null)
          cls = super.findClass(name);
        else
          cls = super.findClass(module_name, name);
        if (cls != null) {
          if (debug) JFLog.log("bootClass:" + name);
          return cls;
        }
      } catch (Exception e) {}
    }
    Folder folder = cp_files.get(name);
    if (folder == null) return null;
    synchronized (folder.lock) {
      Class<?> cls = folder.classes.get(name);
      if (cls != null) return cls;
      byte[] data = getData(name);
      if (data == null) return null;
      if (debug) JFLog.log("defineClass:" + name);
      try {
        cls = defineClass(name, data, 0, data.length);
        if (cls != null) {
          folder.classes.put(name, cls);
        }
        return cls;
      } catch (Throwable t) {
        JFLog.log(t);
        return null;
      }
    }
  }

  private byte[] getData(String name) {
    Folder folder = cp_files.get(name);
    if (folder == null) {
      JFLog.log("JFClassLoader:class not found:" + name);
      return null;
    }
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
    return super.loadClass(name);
  }

  public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    return super.loadClass(name, resolve);
  }

  public static String inspectClass(Class<?> cls, boolean recursive) {
    StringBuilder sb = new StringBuilder();
    sb.append("inspectClass{");
    sb.append("class=" + cls);
    Class<?> supercls = cls.getSuperclass();
    if (supercls != null) {
      sb.append(",super=" + supercls);
    }
    Object[] ifaces = cls.getInterfaces();
    for(Object iface : ifaces) {
      sb.append(",interface=" + iface);
    }
    sb.append("}");
    if (recursive && supercls != null) {
      sb.append(inspectClass(supercls, recursive));
    }
    return sb.toString();
  }

  public static String inspectObject(Object obj, boolean recursive) {
    StringBuilder sb = new StringBuilder();
    sb.append("inspectObject{object=" + obj);
    Class<?> cls = obj.getClass();
    sb.append(",class=" + cls);
    Class<?> supercls = cls.getSuperclass();
    sb.append(",super=" + supercls);
    Object[] ifaces = cls.getInterfaces();
    for(Object iface : ifaces) {
      sb.append(",interface=" + iface);
    }
    sb.append("}");
    if (recursive && supercls != null) {
      sb.append(inspectClass(supercls, recursive));
    }
    return sb.toString();
  }

  public String toString() {
    return "JFClassLoader";
  }
}
