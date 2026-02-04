package javaforce.utils;

/**
 * Monitor Directory utility
 *
 * @author pquiring
 *
 * Created : Nov 3, 2013
 */

import java.util.*;

import javaforce.*;
import javaforce.io.*;


public class monitordir implements FolderListener {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.out.println("Usage:jf-monitor-dir folder");
      return;
    }
    MonitorFolder mf = MonitorFolder.getInstance();
    mf.create(args[0]);
    mf.poll(new monitordir());
  }

  public void folderChangeEvent(String event, String path) {
    System.out.println(event + ":" + path);
  }
}
