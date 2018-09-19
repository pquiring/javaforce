package jpbx.core;

/** Special class loader for Plugins.<br>
Because the CLASSPATH can not be expanded dynamically a special ClassLoader must be created to load plugins.<br>
path: jpbx.plugins.*.class<br>
Plugins are JARs in the /plugins folder.
*/

import java.io.*;
import java.util.*;
import java.util.zip.*;
import javaforce.*;

public class PluginsClassLoader extends ClassLoader {
  private ArrayList<String> jarlist = new ArrayList<String>();
  private Hashtable<String, ZipFile> zflist = new Hashtable<String, ZipFile>();
  private Hashtable<String, ZipFile> clslist = new Hashtable<String, ZipFile>();

  public boolean isLoaded(String jar) {
    return (zflist.get(jar) != null);
  }

  public boolean loadPlugin(String jar) {
    try {
      ZipFile zf = new ZipFile(Paths.plugins + jar);
      ZipEntry ze;
      zflist.put(jar, zf);
      jarlist.add(jar);
      Enumeration<? extends ZipEntry> e = zf.entries();
      while (e.hasMoreElements()) {
        ze = e.nextElement();
        if (!ze.getName().endsWith(".class")) continue;
        clslist.put(ze.getName(), zf);
      }
    } catch (Exception e) {
      JFLog.log("PluginsClassLoader:unable to open:" + jar);
      JFLog.log(e);
      return false;
    }
    return true;
  }

  public void unloadPlugins() {
    ZipFile zf;
    while (zflist.size() > 0) {
      try {
        zf = zflist.remove(jarlist.remove(0));
        zf.close();
      } catch (Exception e) {
        JFLog.log("Error:" + e);
      }
    }
  }

  public Class findClass(String cls) throws ClassNotFoundException {
    if (!cls.startsWith("jpbx.plugins.")) return super.findClass(cls);
    byte[] b = loadClassData(cls);
    return defineClass(cls, b, 0, b.length);
  }

  private byte[] loadClassData(String cls) {
    //cls = jpbx.plugins.**.*.class
    try {
      String pathname = cls.replaceAll("[.]", "/") + ".class";
      ZipFile zf = clslist.get(pathname);
      if (zf == null) throw new Exception("class not found(1):" + cls);
      ZipEntry ze = zf.getEntry(pathname);
      if (ze == null) throw new Exception("class not found(2):" + cls);
      InputStream is = zf.getInputStream(ze);
      int len = is.available();
      byte data[] = new byte[len];
      JF.readAll(is, data, 0, len);
      return data;
    } catch (Exception e) {
      JFLog.log(e);
    }
    return null;
  }
}