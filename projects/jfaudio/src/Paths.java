/**
 * Returns various temp paths for Windows/Linux
 *
 * @author pquiring
 *
 * Created : Sept 21, 2013
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class Paths {
  public static File getTempFile(String name, String ext) {
    if (JF.isWindows()) {
      try {
        return File.createTempFile(name, ext, new File(System.getenv("TEMP")));
      } catch (Exception e) {
        return null;
      }
    } else {
      try {
        return File.createTempFile(name, ext, new File("/tmp"));
      } catch (Exception e) {
        return null;
      }
    }
  }

  static Random r = new Random();

  /** This is a temp folder that is moved to the users profile if saved. */
  public static String getNewProjectPath() {
    if (JF.isWindows()) {
      return System.getenv("TEMP") + "/jfaudio-" + JF.getCurrentUser() + "/project-" + Math.abs(r.nextInt());
    } else {
      return "/var/tmp/jfaudio-" + JF.getCurrentUser() + "/project-" + Math.abs(r.nextInt());
    }
  }

  public static String getClipboardPath() {
    if (JF.isWindows()) {
      return System.getenv("TEMP") + "/jfaudio-" + JF.getCurrentUser() + "/clipboard";
    } else {
      return "/var/tmp/jfaudio-" + JF.getCurrentUser() + "/clipboard";
    }
  }

  public static boolean isTempPath(String path) {
    if (JF.isWindows()) {
      return path.startsWith(System.getenv("TEMP"));
    } else {
      return path.startsWith("/var/tmp") || path.startsWith("/tmp");
    }
  }

  public static boolean testPaths() {
    try {
      if (JF.isWindows()) {
        File temp = new File(System.getenv("TEMP"));
        if (!temp.exists()) throw new Exception("%TEMP% doesn't exist");
        if (!temp.isDirectory()) throw new Exception("%TEMP% is not a folder");
        return true;
      } else {
        File temp = new File("/tmp");
        if (!temp.exists()) throw new Exception("/tmp doesn't exist");
        if (!temp.isDirectory()) throw new Exception("/tmp is not a folder");
        temp = new File("/var/tmp");
        if (!temp.exists()) throw new Exception("/var/tmp doesn't exist");
        if (!temp.isDirectory()) throw new Exception("/var/tmp is not a folder");
        return true;
      }
    } catch (Exception e) {
      JFLog.log(e);
      JFAWT.showError("Error", "Unable to find temporary folders.");
      return false;
    }
  }

  /** Moves a folder. */
  public static boolean moveFolder(String src, String dst) throws Exception {
    File fdst = new File(dst);
    if (fdst.exists()) throw new Exception("dest already exists");
    if (JF.isWindows()) {
      Runtime.getRuntime().exec(new String[] {"cmd", "/c", "move", src, dst});
    } else {
      Runtime.getRuntime().exec(new String[] {"mv", src, dst});
    }
    while (!fdst.exists()) {
      JF.sleep(100);  //wait for move to complete
    }
    JF.sleep(100);
    return true;
  }

  private static void deleteFolder(File folder) {
    File[] files = folder.listFiles();
    if (files != null) {
      for(File f: files) {
        if(f.isDirectory()) {
          deleteFolder(f);
        } else {
          f.delete();
        }
      }
    }
    folder.delete();
  }

  /** Deletes a folder and all contents */
  public static void deleteFolderEx(String path) throws Exception {
    if (JF.isWindows()) {
      deleteFolder(new File(path));
    } else {
      Runtime.getRuntime().exec(new String[] {"rm", "-rf", path});
    }
  }
}
