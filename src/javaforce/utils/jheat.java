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
import java.util.zip.*;
import javaforce.JF;

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
  private static ArrayList<Entry> wixfiles = new ArrayList<Entry>();
  private static boolean win64;

  private static void outHeader() {
    out.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
    out.append("<Wix xmlns=\"http://schemas.microsoft.com/wix/2006/wi\">\n");
  }

  private static boolean omitPath(String file) {
    if (file.startsWith("com/oracle")) return true;
//    if (file.startsWith("com/sun/beans")) return true;
//    if (file.startsWith("java/beans")) return true;
    if (file.startsWith("com/sun/corba")) return true;
    if (file.startsWith("com/sun/jmx")) return true;
    if (file.startsWith("com/sun/jndi")) return true;
    if (file.startsWith("com/sun/org/")) return true;
//    if (file.startsWith("com/sun/org/glassfish")) return true;
//    if (file.startsWith("com/sun/org/omg")) return true;
    if (file.startsWith("com/sun/xml")) return true;
    if (file.startsWith("com/sun/security")) return true;
    if (file.startsWith("org/omg")) return true;
    if (file.startsWith("org/jcp")) return true;
    if (file.startsWith("org/w3c")) return true;
    if (file.startsWith("org/xml")) return true;
    if (file.startsWith("sun/applet")) return true;
    if (file.startsWith("sun/corba")) return true;
    if (file.startsWith("sun/rmi")) return true;
    if (file.startsWith("sun/launcher")) return true;
    if (file.startsWith("java/rmi")) return true;
    if (file.startsWith("javax/xml")) return true;
//    if (file.startsWith("jdk/")) return true;
    if (!awt) {
      if (file.startsWith("java/awt")) return true;
    }
    if (!swing) {
      if (file.startsWith("javax/swing")) return true;
    }
    if (!sql) {
      if (file.startsWith("java/sql")) return true;
      if (file.startsWith("javax/sql")) return true;
    }
    return false;
  }

  private static byte buf[] = new byte[1024 * 64];

  private static void buildRT_JAR(String in_rt_jar) {
    try {
      ZipFile in = new ZipFile(in_rt_jar);
      FileOutputStream fos = new FileOutputStream("rt.jar");
      ZipOutputStream out = new ZipOutputStream(fos);
      Enumeration<? extends ZipEntry> e = in.entries();
      while (e.hasMoreElements()) {
        ZipEntry ze = e.nextElement();
        if (omitPath(ze.getName())) continue;
        InputStream is = in.getInputStream(ze);
        out.putNextEntry(ze);
        int length = is.available();
        int copied = 0;
        while (copied < length) {
          int read = is.read(buf);
          if (read > 0) {
            out.write(buf, 0, read);
            copied += read;
          }
        }
        is.close();
        out.closeEntry();
      }
      out.close();
      fos.close();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static String omitFiles[] = {
    "jfxmedia.dll",  //139K
    "jfxwebkit.dll",  //20M
    "deploy.jar",  //4.7M
    "deploy.dll",  //568K
    "plugin.jar",  //1.9M
    "charsets.jar",  //3M
    "gstreamer-lite.dll",  //622K
    "glib-lite.dll",  //456K
    "fxplugins.dll",  //187K
    "javafx_font.dll",  //75K
    "javafx_font_t2k.dll",  //539K
    "splashscreen.dll",  //210K
  };

  private static boolean omitFile(String file) {
    for(int a=0;a<omitFiles.length;a++) {
      if (file.equals(omitFiles[a])) return true;
    }
    if (!awt) {
      if (file.equals("awt.dll")) return true;
      if (file.equals("jawt.dll")) return true;
      if (file.equals("resources.jar")) return true;
    }
    return false;
  }

  private static void addFolder(String path, final String filespec) throws Exception {
    File folder = new File(path);
    File files[] = folder.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return JF.wildcardCompare(name, filespec, false);
      }
    });
    for(int a=0;a<files.length;a++) {
      String name = files[a].getName();
      if (omitFile(name)) continue;
      Entry e = new Entry();
      e.path = path;
      e.file = name;
      wixfiles.add(e);
    }
  }

  private static void outFolder(String parent, String name, String path) {
    String id = path.replaceAll("/", "_");
    String did = "dir" + id;
    out.append("<Fragment>\n");
    out.append("  <DirectoryRef Id=\"" + parent + "\">\n");
    out.append("    <Directory Id=\"" + did + "\" Name=\"" + name + "\" FileSource=\"" + java_home + path + "\" />\n");
    out.append("  </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static int guid;
  private static int fileid;

  private static void outFiles(String path) {
    int cnt = wixfiles.size();
    String id = path.replaceAll("/", "_");
    String did = "dir" + id;
    String cid = "cmp" + id;

    out.append("<Fragment>\n");
    out.append("    <DirectoryRef Id=\"" + did + "\">\n");
    out.append("        <Component Id=\"" + cid + "\" Guid=\"{8A8E15CB-3AA6-4D96-AD6D-5241AD6E3F6" + guid++ + "}\"" + (win64 ? " Win64=\"yes\"" : "") + ">\n");
    for(int a=0;a<cnt;a++) {
      Entry e = wixfiles.get(a);
      if (!e.path.endsWith(path)) continue;
      if (e.file.equals("rt.jar")) {
        buildRT_JAR(e.path + "/" + e.file);
        out.append("<File Id=\"_" + fileid++ + "\" Source=\"rt.jar\" />\n");
      } else {
        out.append("<File Id=\"_" + fileid++ + "\" Source=\"" + e.path + "/" + e.file + "\" />\n");
      }
    }
    out.append("        </Component>\n");
    out.append("    </DirectoryRef>\n");
    out.append("</Fragment>\n");
  }

  private static void outTrailer() {
    out.append("<Fragment>\n");
    out.append("  <ComponentGroup Id=\"JRE\">\n");
    out.append("    <ComponentRef Id=\"cmp_bin\" />\n");
    out.append("    <ComponentRef Id=\"cmp_bin_server\" />\n");
    out.append("    <ComponentRef Id=\"cmp_lib\" />\n");
    out.append("    <ComponentRef Id=\"cmp_lib_fonts\" />\n");
    out.append("    <ComponentRef Id=\"cmp_lib_images_cursors\" />\n");
    out.append("    <ComponentRef Id=\"cmp_lib_security\" />\n");
    out.append("  </ComponentGroup>\n");
    out.append("</Fragment>\n");
    out.append("</Wix>\n");
  }

  private static boolean awt = false;
  private static boolean swing = false;
  private static boolean sql = false;

  private static void loadProperties() {
    try {
      Properties props = new Properties();
      FileInputStream fis = new FileInputStream("jre.properties");
      props.load(fis);
      fis.close();
      String modules[] = props.getProperty("modules").split(",");
      for(int a=0;a<modules.length;a++) {
        if (modules[a].equals("awt")) awt = true;
        else if (modules[a].equals("swing")) swing = true;
        else if (modules[a].equals("sql")) sql = true;
      }
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
    java_home = args[0];
    if (args.length > 1 && args[1].equals("win64")) {
      win64 = true;
    }
    try {
      File test = new File(java_home + "/release");
      if (!test.exists()) throw new Exception("JAVA_HOME not valid:" + java_home);

      outHeader();

      addFolder(java_home + "/bin", "*.dll");
      addFolder(java_home + "/bin/server", "*.dll");
      addFolder(java_home + "/lib", "*.jar");
      addFolder(java_home + "/lib", "keytool.exe");
      addFolder(java_home + "/lib/fonts", "*.ttf");
      addFolder(java_home + "/lib/images/cursors", "*");
      addFolder(java_home + "/lib/security", "*");
      addFolder(java_home + "/lib", "*.bfc");
      addFolder(java_home + "/lib", "*.dat");
      addFolder(java_home + "/lib", "*.data");
      addFolder(java_home + "/lib", "classlist");
      addFolder(java_home + "/lib", "meta-index");
      addFolder(java_home + "/lib", "tzmappings");
      addFolder(java_home + "/lib", "*.ja");
      addFolder(java_home + "/lib", "*.src");
      addFolder(java_home + "/lib", "*.properties");

      outFolder("APPLICATIONROOTDIRECTORY", "bin", "/bin");
      outFolder("dir_bin", "server", "/bin/server");
      outFolder("APPLICATIONROOTDIRECTORY", "lib", "/lib");
      outFolder("dir_lib", "fonts", "/lib/fonts");
      outFolder("dir_lib", "images", "/lib/images");
      outFolder("dir_lib_images", "cursors", "/lib/images/cursors");
      outFolder("dir_lib", "security", "/lib/security");

      outFiles("/bin");
      outFiles("/bin/server");
      outFiles("/lib");
      outFiles("/lib/fonts");
      outFiles("/lib/images/cursors");
      outFiles("/lib/security");

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
