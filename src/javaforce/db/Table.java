package javaforce.db;

import java.io.*;
import java.util.*;

import javaforce.*;

/** Tables store Row's in memory for fast access.
 *
 * @author pquiring
 */

public class Table<ROW extends Row> {
  private Row.Creator ctr;

  @SuppressWarnings("unchecked")
  private ROW create() {
    return (ROW)ctr.newInstance();
  }

  public Table(Row.Creator rowCreator) {
    ctr = rowCreator;
    data = new Data<ROW>();
  }

  public static class Data<ROW> implements Serializable {
    public static final long serialVersionUID = 1L;
    public ArrayList<ROW> rows = new ArrayList<>();
    public int minid = 1;  //row starting ID
    public int nextid = 1;  //row next ID to assign
    public int maxid = 2147483647;  //2^31-1
    public boolean reuseids = false;

    public int id;  //table id if in TableList (else free to use)

    public int xid;  //user id
    public String name;  //user defined table name
  }

  private Data<ROW> data;
  private String filename;

  @SuppressWarnings("unchecked")
  public boolean load(String filename) {
    this.filename = filename;
    if (!new File(filename).exists()) return false;
    try {
      data = (Data<ROW>)Compression.deserialize(filename);
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  public boolean save() {
    try {
      return Compression.serialize(filename, data);
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  private int findIdx(ROW row) {
    int cnt = data.rows.size();
    if (cnt == 0) return 0;  //insert @ start
    int min = 0;
    int idx = cnt / 2;
    int max = cnt;
    int tmp, delta;
    while (true) {
      if (idx == cnt) return idx;
      ROW r = data.rows.get(idx);
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

  public void add(ROW row) {
    if (data.reuseids) {
      int id = data.minid;
      int cnt = getCount();
      for(int a=0;a<cnt;a++) {
        ROW r = data.rows.get(a);
        if (r.id == id) {
          id++;
          if (id == data.maxid) {
            JFLog.log("Warning:Table is full!");
            return;
          }
          a = -1;
        }
      }
      row.id = id;
    } else {
      row.id = data.nextid++;
      if (data.nextid == data.maxid) {
        JFLog.log("Warning:Table id reset to start!");
        data.nextid = data.minid;
      }
    }
    row.timestamp = System.currentTimeMillis();
    data.rows.add(findIdx(row), row);
  }

  public ROW get(int id) {
    int size = data.rows.size();
    for(int a=0;a<size;a++) {
      ROW row = data.rows.get(a);
      if (row.id == id) return row;
    }
    return null;
  }

  public void remove(int id) {
    int size = data.rows.size();
    for(int a=0;a<size;a++) {
      ROW row = data.rows.get(a);
      if (row.id == id) {
        data.rows.remove(a);
        break;
      }
    }
  }

  public void clear() {
    data.rows.clear();
  }

  @SuppressWarnings("unchecked")
  public ArrayList<ROW> getRows() {
    return data.rows;
  }

  public int getCount() {
    return data.rows.size();
  }

  public int getMinId() {
    return data.minid;
  }

  public int getMaxId() {
    return data.maxid;
  }

  public int getNextId() {
    return data.nextid;
  }

  public void setMinId(int id) {
    if (data.nextid < id) {
      data.nextid = id;
    }
    data.minid = id;
  }

  public void setMaxId(int id) {
    if (data.nextid > id) {
      data.nextid = data.minid;
    }
    data.maxid = id;
  }

  public boolean getReuseIds() {
    return data.reuseids;
  }

  public void setReuseIds(boolean state) {
    data.reuseids = state;
  }

  public int getTableId() {
    return data.id;
  }

  public void setTableId(int id) {
    data.id = id;
  }

  public String getName() {
    return data.name;
  }

  public void setName(String name) {
    data.name = name;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public int getXId() {
    return data.xid;
  }

  public void setXId(int id) {
    data.xid = id;
  }
}
