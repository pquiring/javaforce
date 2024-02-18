package javaforce;

/** DebugState
 *
 * Continuously updates debug info.
 *
 * To view real time : watch -n 1 cat debug.log
 *
 * To install watch : pacman -S procps-ng
 *
 * @author pquiring
 */

import java.io.*;

import javaforce.*;

public class DebugState extends Thread {
  private RandomAccessFile file;
  private boolean active = true;
  private Runnable runnable;
  private StringBuilder sb = new StringBuilder();

  public DebugState(String filename, Runnable update) {
    this.runnable = update;
    try {
      file = new RandomAccessFile(filename, "rw");
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void write(String msg) {
    sb.append(msg);
    sb.append("\n");
  }

  private void flush() {
    try {
      file.setLength(0);
      file.write(sb.toString().getBytes());
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void cancel() {
    active = false;
  }

  private static String mark = "*********************************************\n";

  public void run() {
    while (active) {
      sb.setLength(0);
      sb.append(mark);
      try {
        runnable.run();
      } catch (Exception e) {}
      sb.append(mark);
      flush();
      JF.sleep(1000);
    }
  }
}
