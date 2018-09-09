package javaforce.db;

import java.io.*;
import java.util.*;

import javaforce.*;

public class Table implements java.io.Serializable {
  public static final long serialVersionUID = 1;
    
  private ArrayList<Row> rows = new ArrayList<Row>();
  private int nextid = 1;
  
  public int id;  //user defined table id
  public String name;  //user defined table name
    
  public static Table load(String filename) {
    try {
      FileInputStream fis = new FileInputStream(filename);
      ObjectInputStream ois = new ObjectInputStream(fis);
      Table table = (Table)ois.readObject();
      fis.close();
      return table;     
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }
    
  public boolean save(String filename) {
    try {
      FileOutputStream fos = new FileOutputStream(filename);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      fos.close();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }
  
  private int findIdx(Row row) {
    int cnt = rows.size();
    if (cnt == 0) return 0;
    int min = 0;
    int idx = cnt / 2;
    int max = cnt;
    int tmp;
    while (true) {
      Row r = rows.get(idx);
      int c = r.compare(row);
      switch (c) {
        case 0: return idx;
        case -1:
          //r is lower : move towards max
          if (idx == max) return idx;
          tmp = idx;
          idx += ((max - idx) / 2);
          min = tmp;
          break;
        case 1:
          //r is higher : move towards min
          if (idx == min) return idx;
          tmp = idx;
          idx -= ((idx - min) / 2);
          max = tmp;
          break;
      }
    }
  }
    
  public synchronized void add(Row row) {
    row.id = nextid++;
    rows.add(findIdx(row), row);
  }
    
  public Row get(int id) {
    int size = rows.size();
    for(int a=0;a<size;a++) {
      Row row = rows.get(a);
      if (row.id == id) return row;
    }
    return null;
  }
    
  public void remove(int id) {
    int size = rows.size();
    for(int a=0;a<size;a++) {
      Row row = rows.get(a);
      if (row.id == id) {
        rows.remove(a);
        break;
      }
    }
  }
  
  public ArrayList<Row> getRows() {
    return rows;
  }
}
