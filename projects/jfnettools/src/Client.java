/**
 *
 * @author pquiring
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class Client extends Thread {
  private NetApp win;
  private boolean active;
  private Socket s;
  private InputStream is;
  private OutputStream os;
  private Timer timer;
  private TT task;
  private Reader reader;
  private Writer writer;
  private Latency latency;
  private long read;
  private Object readLock = new Object();
  private long written;
  private Object writtenLock = new Object();
  private String host;
  private int port;
  private char mode;

  public Client(NetApp win, String host, int port, char mode) {
    this.win = win;
    this.host = host;
    this.port = port;
    this.mode = mode;
  }
  public void run() {
    try {
      win.setClientStatus("Connecting to server..." + host + ":" + port);
      s = new Socket(host, port);
      win.setClientStatus("Running...");
      is = s.getInputStream();
      os = s.getOutputStream();
      os.write(mode);
      active = true;
      switch (mode) {
        case 'F':  //full duplex
          reader = new Reader();
          reader.start();
          writer = new Writer();
          writer.start();
          break;
        case 'S':  //client send only
          writer = new Writer();
          writer.start();
          break;
        case 'R':  //client recv only
          reader = new Reader();
          reader.start();
          break;
        case 'L':  //latency test
          latency = new Latency();
          latency.start();
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
      win.setClientStatus(e.toString());
    }
    if (mode != 'L') {
      timer = new Timer();
      task = new TT();
      timer.scheduleAtFixedRate(task, 1000, 1000);
    }
  }
  public void close() {
    active = false;
    if (timer != null) {
      timer.cancel();
      timer = null;
    }
    try { s.close(); } catch (Exception e) {}
  }
  private class TT extends TimerTask {
    private double mb = 1024 * 1024;
    private double kb = 1024;
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
      long r, w;
      String rs, ws;
      if (mode == 'F' || mode == 'R') {
        synchronized(readLock) {
          r = read;
          read = 0;
        }
        rs = speedToString(r);
      } else {
        rs = "n/a";
      }
      if (mode == 'F' || mode == 'S') {
        synchronized(writtenLock) {
          w = written;
          written = 0;
        }
        ws = speedToString(w);
      } else {
        ws = "n/a";
      }
      final String f_rs = rs;
      final String f_ws = ws;
      java.awt.EventQueue.invokeLater(new Runnable() {
        public void run() {
          win.setReadSpeed(f_rs);
          win.setWriteSpeed(f_ws);
        }
      });
    }
  }
  private class Reader extends Thread {
    public void run() {
      byte data[] = new byte[1460];
      try {
        while (active) {
          int r = is.read(data);
          if (r == -1) break;
          synchronized(readLock) {
            read += r;
          }
        }
      } catch (Exception e) {
      }
    }
  }
  private class Writer extends Thread {
    public void run() {
      byte data[] = new byte[1460];
      try {
        while (active) {
          os.write(data);
          synchronized(writtenLock) {
            written += 1460;
          }
        }
      } catch (Exception e) {
      }
    }
  }

  //LE set/get

  private static void setuint32(byte[] data, int offset, int num) {
    data[offset+0] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+1] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+2] = (byte)(num & 0xff);
    num >>= 8;
    data[offset+3] = (byte)(num & 0xff);
  }

  private static int getuint32(byte[] data, int offset) {
    int ret;
    ret  = (int)data[offset] & 0xff;
    ret += ((int)data[offset+1] & 0xff) << 8;
    ret += ((int)data[offset+2] & 0xff) << 16;
    ret += ((int)data[offset+3] & 0xff) << 24;
    return ret;
  }

  private class Latency extends Thread {
    public void run() {
      byte data[] = new byte[4];
      int idx = 0;
      int sidx;
      long s1, s2, diff;
      try {
        while (active) {
          Thread.sleep(5);
          setuint32(data, 0, idx);
          s1 = System.nanoTime();
          os.write(data);
          int read = is.read(data);
          if (read == 4) {
            sidx = getuint32(data, 0);
          }
          s2 = System.nanoTime();
          diff = s2 - s1;
          win.addLatency((int)(diff / 1000L));
        }
      } catch (Exception e) {
      }
    }
  }
}
