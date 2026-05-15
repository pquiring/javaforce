package javaforce.db;

import java.io.*;
import java.util.*;

import javaforce.*;

/** Tables store Row's in memory for fast access.
 *
 * @author pquiring
 */

public class Table<ROW extends Row> implements Serializable {
  public static final long serialVersionUID = 1L;
  private Row.Creator ctr;

  @SuppressWarnings("unchecked")
  private ROW create() {
    return (ROW)ctr.newInstance();
  }

  public Table(Row.Creator rowCreator) {
    ctr = rowCreator;
  }

  private ArrayList<ROW> rows = new ArrayList<ROW>();
  private int minid = 1;
  private int nextid = 1;
  private int maxid = 2147483647;  //2^31-1
  private boolean reuseids = false;

  public int id;  //table id if in TableList (else free to use)
  public String name;  //user defined table name

  public int xid;  //user defined xref table id

  protected transient String filename;

  public static Table load(String filename) {
    try {
      Table table = (Table)Compression.deserialize(filename);
      return table;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public boolean save() {
    try {
      return Compression.serialize(this.filename, this);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private int findIdx(ROW row) {
    int cnt = rows.size();
    if (cnt == 0) return 0;
    int min = 0;
    int idx = cnt / 2;
    int max = cnt;
    int tmp, delta;
    while (true) {
      if (idx == cnt) return idx;
      ROW r = rows.get(idx);
      switch (r.compare(row)) {
        case 0: return idx;
        case -1:
          //r is lower : move towards max
          if (idx == max) return idx;
          tmp = idx;
          delta = ((max - idx) / 2);
          if (delta == 0) delta++;
          idx += delta;
          min = tmp;
          break;
        case 1:
          //r is higher : move towards min
          if (idx == min) return idx;
          tmp = idx;
          delta = ((idx - min) / 2);
          if (delta == 0) delta++;
          idx -= delta;
          max = tmp;
          break;
      }
    }
  }

  public synchronized void add(ROW row) {
    if (reuseids) {
      int id = minid;
      int cnt = getCount();
      for(int a=0;a<cnt;a++) {
        ROW r = rows.get(a);
        if (r.id == id) {
          id++;
          if (id == maxid) {
            JFLog.log("Warning:Table is full!");
            return;
          }
          a = -1;
        }
      }
      row.id = id;
    } else {
      row.id = nextid++;
      if (nextid == maxid) {
        JFLog.log("Warning:Table id reset to start!");
        nextid = minid;
      }
    }
    row.timestamp = System.currentTimeMillis();
    rows.add(findIdx(row), row);
  }

  public ROW get(int id) {
    int size = rows.size();
    for(int a=0;a<size;a++) {
      ROW row = rows.get(a);
      if (row.id == id) return row;
    }
    return null;
  }

  public void remove(int id) {
    int size = rows.size();
    for(int a=0;a<size;a++) {
      ROW row = rows.get(a);
      if (row.id == id) {
        rows.remove(a);
        break;
      }
    }
  }

  public void clear() {
    rows.clear();
  }

  public ArrayList<ROW> getRows() {
    return rows;
  }

  public int getCount() {
    return rows.size();
  }

  public int getMinId() {
    return minid;
  }

  public int getMaxId() {
    return maxid;
  }

  public int getNextId() {
    return nextid;
  }

  public void setMinId(int id) {
    if (nextid < id) {
      nextid = id;
    }
    minid = id;
  }

  public void setMaxId(int id) {
    if (nextid > id) {
      nextid = minid;
    }
    maxid = id;
  }

  public boolean getReuseIds() {
    return reuseids;
  }

  public void setReuseIds(boolean state) {
    reuseids = state;
  }
}
