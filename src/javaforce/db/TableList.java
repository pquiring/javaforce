package javaforce.db;

/** List of tables stored in a folder.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class TableList implements java.io.Serializable {
  public static final long serialVersionUID = 1;

  private String folder;
  private int minid = 1;
  private int nextid = 1;
  private int maxid = 2147483647;  //2^31-1
  private transient ArrayList<Table> tables = new ArrayList<Table>();

  private void loadTables() {
    for(int a=1;a<nextid;a++) {
      String filename = folder + "/" + a + ".dat";
      if (!new File(filename).exists()) continue;
      Table table = Table.load(filename);
      if (table == null) {
        JFLog.log("Error:Table.load() failed:" + filename);
        continue;
      }
      tables.add(table);
    }
  }
  public static TableList load(String folder) {
    try {
      TableList list;
      File file = new File(folder + "/0.dat");
      if (!file.exists()) {
        list = new TableList();
        list.folder = folder;
        return list;
      }
      FileInputStream fis = new FileInputStream(folder + "/0.dat");
      ObjectInputStream ois = new ObjectInputStream(fis);
      list = (TableList)ois.readObject();
      fis.close();
      list.loadTables();
      return list;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public Table get(int id) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.id == id) return table;
    }
    return null;
  }
  public Table get(String name) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.name.equals(name)) return table;
    }
    return null;
  }
  public void save(int id) {
    Table table = get(id);
    if (table != null) {
      table.save();
    }
  }
  private void save() {
    try {
      FileOutputStream fos = new FileOutputStream(folder + "/0.dat");
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(this);
      fos.close();
    } catch (Exception e) {
      JFLog.log(e);
    }
  }
  public synchronized void add(Table table) {
    table.id = nextid++;
    if (nextid == maxid) {
      JFLog.log("Warning:TableList:next id reset to start");
      nextid = minid;
    }
    save();
  }
  public ArrayList<Table> getTables() {
    return tables;
  }
  public void remove(int id) {
    File file = new File(folder + "/" + id + ".dat");
    if (file.exists()) {
      file.delete();
    }
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.id == id) {
        tables.remove(a);
        break;
      }
    }
  }
}
