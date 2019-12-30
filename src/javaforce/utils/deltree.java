package javaforce.utils;

/** deltree
 *
 * Delete files/folder that are x days old.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class deltree {
  public static long timestamp;
  public static int fileCount;
  public static int folderCount;
  public static int checkCount;
  public static void main(String args[]) {
    if (args.length != 2) {
      System.out.println("Usage : deltree PATH days");
      System.out.println(" Desc : delete all files/folder from PATH that are specifed days old or older.");
      System.exit(0);
    }
    File path = new File(args[0]);
    if (!path.isDirectory()) {
      System.err.println("Error:PATH is not a folder");
      System.exit(0);
    }
    System.out.println("Deleting files from: " + args[0]);
    Calendar c = Calendar.getInstance();
    timestamp = c.getTimeInMillis();
    System.out.println("   now timestamp = " + c.getTimeInMillis());
    timestamp -= Long.valueOf(args[1]) * 86400000L;
    System.out.println("target timestamp = " + timestamp);
    deleteFolder(path);
    System.out.println("Deleted Files: " + fileCount);
    System.out.println("Deleted Folders: " + folderCount);
  }

  public static void deleteFolder(File file) {
    File files[] = file.listFiles();
    if (files == null) return;
    for(int a=0;a<files.length;a++) {
//      System.out.println("checking:" + files[a].getAbsolutePath() + ":" + files[a].lastModified());  //test
      checkCount++;
      if (checkCount >= 1000) {
        System.out.println("Files processes:" + checkCount);
        checkCount = 0;
      }
      if (files[a].isDirectory()) {
        deleteFolder(files[a]);
        if (files[a].lastModified() < timestamp) {
          System.out.println("Delete Folder:" + files[a].getAbsolutePath());
          files[a].delete();
          folderCount++;
        }
      } else {
        if (files[a].lastModified() < timestamp) {
          System.out.println("Delete File:" + files[a].getAbsolutePath());
          files[a].delete();
          fileCount++;
        }
      }
    }
  }
}
