/**
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class FileTest extends Thread {
  private NetApp win;
  private String file;
  private boolean active;
  private Timer timer;
  private long read, size, total;
  private Object lock = new Object();
  public FileTest(NetApp win, String file) {
    this.win = win;
    this.file = file;
  }
  public void run() {
    active = true;
    byte data[] = new byte[4096];
    FileInputStream fis = null;
    try {
      File f = new File(file);
      size = f.length();
      timer = new Timer();
      timer.scheduleAtFixedRate(new TT(), 1000, 1000);
      fis = new FileInputStream(file);
      while (active) {
        int r = fis.read(data);
        if (r == -1) break;
        synchronized(lock) {
          read += r;
        }
      }
      fis.close();
      fis = null;
      if (active) {
        win.fileTestComplete();
      }
    } catch (Exception e) {
      e.printStackTrace();
      win.setFileStatus(e.toString());
      if (fis != null) {
        try {fis.close();} catch (Exception e2) {}
      }
    }
  }
  public void close() {
    timer.cancel();
    timer = null;
    active = false;
  }
  private class TT extends TimerTask {
    private double mb = 1024 * 1024;
    private double kb = 1024;
    private long last;
    private String speedToString(double x) {
      if (x >= mb) {
        return String.format("%.3f", x / mb) + " MB/s";
      }
      if (x >= kb) {
        return String.format("%.3f", x / kb) + " KB/s";
      }
      return "" + x + " B/s";
    }
    public void run() {
      long r;
      synchronized(lock) {
        r = read;
        read = 0;
      }
      total += r;
      if (total == size) {
        r = last;
      }
      double percent = ((double)total) * 100.0 / ((double)size);
      win.setFileStatus(speedToString(r) + " : " + String.format("%.3f", percent) + "% complete");
      if (total == size) {
        timer.cancel();
      } else {
        last = r;
      }
    }
  }
}
