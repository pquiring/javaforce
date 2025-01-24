/*

Records a line to a WAV file.

*/

import java.io.*;
import java.util.*;

import javaforce.*;
import javaforce.media.*;

public class Record extends Thread {
  private RandomAccessFile raf;
  private int length;
  private byte buf8[] = null;
  private int sample_rate;
  private AudioBuffer buffer = null;
  private boolean active, done;
  private Object lock = new Object();
  private int bufsiz;

  public Record() {
    sample_rate = Settings.current.sample_rate;
    switch (sample_rate) {
      case 44100: bufsiz = 882; break;
      case 48000: bufsiz = 960; break;
    }
    buf8 = new byte[bufsiz * 2];
    buffer = new AudioBuffer(sample_rate, 1, 2);
  }

  public void open() {
    try {
      length = 0;
      Calendar cal = Calendar.getInstance();
      String fn = String.format("%s/%d-%02d-%02d %02d-%02d-%02d.wav"
        , Settings.current.downloadPath
        , cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH)
        , cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND));
      raf = new RandomAccessFile(fn, "rw");
      writeHeader();
      active = true;
      start();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  private void writeHeader() {
    byte header[] = new byte[44];
    header[0 + 0] = 'R';
    header[0 + 1] = 'I';
    header[0 + 2] = 'F';
    header[0 + 3] = 'F';
    LE.setuint32(header, 4, 36 + length*2);
    header[8 + 0] = 'W';
    header[8 + 1] = 'A';
    header[8 + 2] = 'V';
    header[8 + 3] = 'E';
    header[12 + 0] = 'f';
    header[12 + 1] = 'm';
    header[12 + 2] = 't';
    header[12 + 3] = ' ';
    LE.setuint32(header, 16, 16);  //chunk size
    LE.setuint16(header, 20, 1);   //1=PCM
    LE.setuint16(header, 22, 1);   //1=mono
    LE.setuint32(header, 24, sample_rate);  //freq
    LE.setuint32(header, 28, sample_rate * 2);  //byte rate
    LE.setuint16(header, 32, 2);    //block align
    LE.setuint16(header, 34, 16);   //bits/sample
    header[36 + 0] = 'd';
    header[36 + 1] = 'a';
    header[36 + 2] = 't';
    header[36 + 3] = 'a';
    LE.setuint32(header, 40, length*2);  //data size
    try {
      raf.seek(0);
      raf.write(header);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public void add(short buf[]) {
    buffer.add(buf, 0, buf.length);
    synchronized(lock) {
      lock.notify();
    }
  }

  private void write(short buf[]) {
    LE.shortArray2byteArray(buf, buf8);
    try {raf.write(buf8);} catch (Exception e) {JFLog.log(e);}
    length += buf.length;
    if (length > (12 * 60 * 60 * sample_rate)) {  //12 hrs
      close();
      open();
    }
  }

  public void close() {
    if (raf == null) return;
    active = false;
    while (!done) {
      synchronized(lock) {
        lock.notify();
      }
      JF.sleep(10);
    }
    //patch header (length)
    writeHeader();
    try {
      raf.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
    raf = null;
  }

  public void run() {
    short buf[] = new short[bufsiz];
    while (active) {
      synchronized(lock) {
        if (buffer.size() < bufsiz) {
          try { lock.wait(); } catch (Exception e) {}
        }
        if (buffer.size() < bufsiz) continue;
      }
      buffer.get(buf, 0, bufsiz);
      write(buf);
    }
    done = true;
  }
}
