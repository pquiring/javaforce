package javaforce.db;

import java.io.*;
import javaforce.io.*;

public class Row extends SerialObject {
  /** auto-increment id */
  public int id;

  /** insert timestamp */
  public long timestamp;

  /** Override this method to sort your rows.
   *  @return 0=equal -1=this is lower +1=other is lower
   */
  public int compare(Row other) {
    if (id == other.id) return 0;
    if (id < other.id) return -1;
    return 1;
  }

  public static interface Creator {
    public Row newInstance();
  }

  private static final int version = 1;

  public void readObject() throws Exception {
    int ver = readInt();
    id = readInt();
    timestamp = readLong();
  }

  public void writeObject() throws Exception {
    writeInt(version);
    writeInt(id);
    writeLong(timestamp);
  }
}
