package javaforce.utils;

/**
 * Created : Apr 16, 2012
 *
 * @author pquiring
 */
import java.io.*;
import javaforce.*;
import javaforce.linux.*;

public class OpenFile {

  public static void main(String args[]) {
    //opens arg[0]
    if ((args.length < 1) || (args[0].length() == 0)) {
      System.out.println("usage : jfopen file\ndesc : open program associated with file type");
      return;
    }
    String action = "open";
    String file;
    if (args[0].equals("--edit")) {
      action = "edit";
      file = args[1];
    } else {
      file = args[0];
    }
    try {
      openFile(file, action);
    } catch (Exception e) {
      System.out.println("" + e);
    }
  }

  public static void openFile(String file, String action) throws Exception {
    int idx = file.lastIndexOf(".");
    if (idx == -1) {
      throw new Exception("file has no extension");
    }
    String ext = file.substring(idx + 1);
    //open /etc/mime.types to convert extension to mime type
    String mime = getMimeType(ext);
    if (mime == null) {
      throw new Exception("mime-type not found");
    }
    //open /usr/share/applications/*.desktop
    String desktop = getHandler(mime, action);
    if (desktop == null) {
      if (!action.equals("open")) {
        //try with just "open"
        desktop = getHandler(mime, "open");  //TODO : don't re-read files
      }
      if (desktop == null) {
        throw new Exception("handler not found");
      }
    }
    Linux.executeDesktop(desktop, new String[] {file});
  }
  private static String mimetypes;

  public static String getMimeType(String ext) throws Exception {
    if (mimetypes == null) {
      FileInputStream fis = new FileInputStream("/etc/mime.types");
      byte data[] = JF.readAll(fis);
      fis.close();
      mimetypes = new String(data);
    }
    String lns[] = mimetypes.split("\n");
    for (int a = 0; a < lns.length; a++) {
      String f[] = lns[a].split("\t+");  //greedy tabs
      if (f.length < 2) {
        continue;
      }
      String exts[] = f[1].split(" ");
      for (int b = 0; b < exts.length; b++) {
        if (exts[b].equalsIgnoreCase(ext)) {
          return f[0];
        }
      }
    }
    return null;
  }

  public static String getHandler(String mime, String action) throws Exception {
    String desktop = getDesktop(mime, "/usr/share/applications", action);
    if (desktop != null) return desktop;
    return getDesktop(mime, JF.getUserPath() + "/.local/share/applications", action);
  }

  public static String getIcon(String mime, String action) throws Exception {
    String desktop = getDesktop(mime, "/usr/share/applications", action);
    if (desktop == null) {
      desktop = getDesktop(mime, JF.getUserPath() + "/.local/share/applications", action);
      if (desktop == null) {
        return null;
      }
    }
    FileInputStream fis = new FileInputStream(desktop);
    byte data[] = JF.readAll(fis);
    fis.close();
    String str = new String(data);
    String lns[] = str.split("\n");
    for (int a = 0; a < lns.length; a++) {
      if (lns[a].startsWith("Icon=")) {
        return lns[a].substring(5).trim();
      }
    }
    return null;
  }

  public static String getDesktop(String mime, String folder, String action) throws Exception {
    File file = new File(folder + "/mimeinfo.cache");
    if (!file.exists()) {
      return null;
    }
    FileInputStream fis = new FileInputStream(file);
    byte data[] = JF.readAll(fis);
    fis.close();
    String str = new String(data);
    String lns[] = str.split("\n");
    String cacheAction = "open";  //assume open in cause cache is not generated from JF
    for (int a = 0; a < lns.length; a++) {
      String ln = lns[a].trim();
      if (ln.startsWith("[") || ln.endsWith("]")) {
        cacheAction = ln.substring(1, ln.length() - 1);
        continue;
      }
      if (!cacheAction.equals(action)) {
        continue;
      }
      int idx = lns[a].indexOf("=");
      if (idx == -1) {
        continue;
      }
      if (lns[a].substring(0, idx).equals(mime)) {
        String desktops[] = lns[a].substring(idx + 1).trim().split(";");
        if ((desktops == null) || (desktops.length == 0)) {
          continue;
        }
        return folder + "/" + desktops[0];
      }
    }
    return null;
  }
}
