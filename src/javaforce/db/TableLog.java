package javaforce.db;

import java.io.*;
import java.nio.channels.*;
import java.util.*;

import javaforce.*;

/** Table that logs Row's with date/time.
 *
 * Rows are NOT stored in memory.
 *
 * @author pquiring
 */

public class TableLog<ROW extends Row> {
  private String folder;
  private RandomAccessFile raf;
  private String filename;
  private Object lock = new Object();
  private Row.Creator ctr;

  @SuppressWarnings("unchecked")
  private ROW create() {
    return (ROW)ctr.newInstance();
  }

  public TableLog(String folder, Row.Creator rowCreator) {
    this.folder = folder;
    this.ctr = rowCreator;
  }

  private final long ms_per_day = 24 * 60 * 60 * 1000;  //ms per day

  /** Loads rows that are within provided timestamps. */
  @SuppressWarnings("unchecked")
  public ROW[] get(long start, long end) {
    ArrayList<ROW> rows = new ArrayList<ROW>();
    byte[] len = new byte[4];
    InputStream is = Channels.newInputStream(raf.getChannel());
    try {
      synchronized(lock) {
        long current = start;
        long endp1 = end + ms_per_day;  //range may not be exact number of days
        while (current < endp1) {
          if (open(current, false)) {
            //load all rows within start to end
            raf.seek(0);
            while (raf.getFilePointer() < raf.length()) {
              //read length
              is.read(len);
              int length = BE.getuint32(len, 0);
              ROW row = (ROW)Compression.deserialize(is, length);
              if (row.timestamp >= start && row.timestamp <= end) {
                rows.add(row);
              }
            }
          }
          current += ms_per_day;
        }
      }
      return (ROW[])rows.toArray();
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  private boolean open(long timestamp, boolean create) {
    Calendar now = Calendar.getInstance();
    now.setTimeInMillis(timestamp);
    int year = now.get(Calendar.YEAR);
    int month = now.get(Calendar.MONTH) + 1;
    int day = now.get(Calendar.DAY_OF_MONTH);
    String path = String.format("%04d/%02d", year, month);
    String filename = String.format("%s/%02d.dat", path, day);
    if (!create && !new File(filename).exists()) return false;
    if (this.filename != null && filename.equals(this.filename)) return true;
    try {
      if (raf != null) {
        raf.close();
      }
      new File(path).mkdirs();
      raf = new RandomAccessFile(filename, "rw");
      this.filename = filename;
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Close this TableLog. */
  public void close() {
    synchronized(lock) {
      if (raf != null) {
        try {
          raf.close();
        } catch (Exception e) {}
        raf = null;
      }
    }
  }

  /** Add a new row to TableLog.
   *
   * The row is not assigned an id but the timestamp is set instead.
   *
   */
  public void add(ROW row) {
    row.id = -1;  //not used
    row.timestamp = System.currentTimeMillis();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream os = Channels.newOutputStream(raf.getChannel());
    byte[] len = new byte[4];
    synchronized(lock) {
      open(row.timestamp, true);
      try {
        Compression.serialize(baos, row);
        //write length
        int length = baos.size();
        BE.setuint32(len, length, length);
        os.write(len);
        //write object
        ByteArrayInputStream is = new ByteArrayInputStream(baos.toByteArray());
        JF.copyAll(is, os);
      } catch (Exception e) {
        JFLog.log(e);
      }
    }
  }
}
