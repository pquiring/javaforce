package javaforce.utils;

/** CopyPath
 *
 * @author pquiring
 */

import java.io.*;
import java.nio.file.*;

import javaforce.*;
import javaforce.ansi.server.*;
import javaforce.jni.*;

public class CopyPath implements KeyEvents {
  public static String[] args;
  public ANSI ansi;
  public String src;
  public String dest;
  public int found;
  public int copied;
  public int skipped;
  public int total;
  public int done, lastdone;
  public ProgressBar progress;

  public void keyPressed(int keyCode, int keyMods) {
  }

  public void keyTyped(char key) {
  }

  public static void main(String[] args) {
    JFNative.load_ffmpeg = false;
    JFNative.load();
    ANSI.enableConsoleMode();
    ConsoleOutput.install();
    CopyPath.args = args;
    int ret = 0;
    try {
      new CopyPath().run();
    } catch (Throwable e) {
      e.printStackTrace();
      ret = 1;
    }
    ANSI.disableConsoleMode();
    System.exit(ret);
  }

  public void usage() {
    System.out.println("CopyPath src dest");
  }

  public void run() {
    if (args.length < 2) {
      usage();
      return;
    }
    src = args[0];
    dest = args[1];
    File fsrc = new File(src);
    if (!fsrc.exists() || !fsrc.isDirectory()) {
      System.out.println("Error:src not found");
      return;
    }
    File fdest = new File(dest);
    if (fdest.exists() && !fdest.isDirectory()) {
      System.out.println("Error:dest not folder");
      return;
    }
    if (!fdest.exists()) {
      fdest.mkdirs();
    }
    ansi = new ANSI(this);
    progress = new ProgressBar();
    ansi.getConsoleSize();
    int[] pos = ansi.getConsolePos();
    progress.setPos(0, pos[1]);
    progress.setWidth(ansi.width - 1);
    lastdone = -1;

    findPath(fsrc);
//    System.out.println("Files found :" + found);
    progress.draw();
    try {
      copyPath(fsrc, fdest);
      if (lastdone != 100) {
        progress.setValue(100);
        progress.draw();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
//    System.out.println("\r\n");
//    System.out.println("Files copied :" + copied);
//    System.out.println("Files skipped:" + skipped);
  }

  public void findPath(File src) {
    File[] files = src.listFiles();
    if (files == null) return;
    for(int a=0;a<files.length;a++) {
      if (files[a].isDirectory()) {
        findPath(files[a]);
      } else {
        found++;
      }
    }
  }

  public void copyPath(File src, File dest) throws Exception {
    File[] files = src.listFiles();
    if (files == null) return;
    for(int a=0;a<files.length;a++) {
      String name = files[a].getName();
      if (files[a].isDirectory()) {
        File destPath = new File(dest, name);
        destPath.mkdir();
        copyPath(files[a], destPath);
      } else {
        File destFile = new File(dest, name);
        if (destFile.exists()) {
          long srctime = files[a].lastModified();
          long desttime = destFile.lastModified();
          if (desttime >= srctime) {
            skipped++;
            continue;
          }
        }
        Files.copy(
          Paths.get(files[a].getAbsolutePath()),
          Paths.get(destFile.getAbsolutePath()),
          StandardCopyOption.REPLACE_EXISTING
        );
        copied++;
        done = (copied + skipped) * 100 / found;
        if (lastdone != done) {
          lastdone = done;
          progress.setValue(done);
          progress.draw();
        }
      }
    }
  }
}
