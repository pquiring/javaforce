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
      }
    } catch (Exception e) {
      e.printStackTrace();
      win.setClientStatus(e.toString());
    }
    timer = new Timer();
    task = new TT();
    timer.scheduleAtFixedRate(task, 1000, 1000);
  }
  public void close() {
    active = false;
    timer.cancel();
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
}
