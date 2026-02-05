package javaforce.utils;

/**
 * Monitor Directory utility
 *
 * @author pquiring
 *
 * Created : Nov 3, 2013
 */

import javaforce.io.*;


public class monitordir implements FolderListener {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage:jf-monitor-dir folder");
      return;
    }
    monitordir md = new monitordir();
    md.run(args);
  }

  private MonitorFolder mf;

  private int count;

  private void run(String[] args) {
    if (args.length > 1 && args[1].equals("debug")) {
      count = 2;
    }
    mf = MonitorFolder.getInstance();
    mf.create(args[0]);
    mf.poll(this);
  }

  public void folderChangeEvent(String event, String path) {
    System.out.println("folderChangeEvent:event=" + event + ":path=" + path);
    if (count > 0) {
      count--;
      if (count == 0) {
        //do not call close() in callback or it deadlocks
        new Thread() {
          public void run() {
            mf.close();
          }
        }.start();
      }
    }
  }
}
