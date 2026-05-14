package javaforce.db;

import java.io.*;

public class Row implements Serializable {
  public static final long serialVersionUID = 1L;

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
}
