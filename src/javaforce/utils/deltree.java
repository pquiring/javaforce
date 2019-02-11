package javaforce.utils;

/** deltree
 *
 * Delete files/folder that are x days old.
 *
 * @author peterq.admin
 */

import java.io.*;
import java.util.*;

public class deltree {
  public static long timestamp;
  public static int fileCount;
  public static int folderCount;
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
    Calendar c = Calendar.getInstance();
    c.add(Calendar.DAY_OF_MONTH, -Integer.valueOf(args[1]));
    timestamp = c.getTimeInMillis();
    System.out.println("Deleting files from: " + args[0]);
    deleteFolder(path);
    System.out.println("Deleted Files: " + fileCount);
    System.out.println("Deleted Folders: " + folderCount);
  }

  public static void deleteFolder(File file) {
    File files[] = file.listFiles();
    if (files == null) return;
    for(int a=0;a<files.length;a++) {
      if (files[a].isDirectory()) {
        deleteFolder(files[a]);
        if (files[a].lastModified() < timestamp) {
          files[a].delete();
          folderCount++;
        }
      } else {
        if (files[a].lastModified() < timestamp) {
          files[a].delete();
          fileCount++;
        }
      }
    }
  }
}
