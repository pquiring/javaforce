package javaforce.db;

/** List of tables stored in a folder.
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

import javaforce.*;

public class TableList<ROW extends Row> implements Serializable {
  public static final long serialVersionUID = 1L;
  private Row.Creator ctr;

  @SuppressWarnings("unchecked")
  public Table<ROW> create() {
    return (Table<ROW>)new Table(ctr);
  }

  public TableList(Row.Creator rowCreator) {
    ctr = rowCreator;
  }

  private String folder;
  private int minid = 1;
  private int nextid = 1;
  private int maxid = 2147483647;  //2^31-1
  private transient ArrayList<Table<ROW>> tables = new ArrayList<Table<ROW>>();

  private void loadTables() {
    for(int a=1;a<nextid;a++) {
      String filename = folder + "/" + a + ".dat";
      if (!new File(filename).exists()) continue;
      Table<ROW> table = create();
      table.load(filename);
      if (table == null) {
        JFLog.log("Error:Table.load() failed:" + filename);
        continue;
      }
      tables.add(table);
    }
  }

  @SuppressWarnings("unchecked")
  public static TableList load(String folder) {
    try {
      File file = new File(folder + "/0.dat");
      if (!file.exists()) {
        return null;
      }
      TableList list = (TableList)Compression.deserialize(file);
      list.tables = new ArrayList<Table>();
      list.loadTables();
      return list;
    } catch (Exception e) {
      JFLog.log(e);
      return null;
    }
  }

  public Table<ROW> get(int id) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table<ROW> table = tables.get(a);
      if (table.id == id) return table;
    }
    return null;
  }
  public Table<ROW> get(String name) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table<ROW> table = tables.get(a);
      if (table.name.equals(name)) return table;
    }
    JFLog.log("Error:Table not found:" + name);
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
      new File(folder).mkdirs();
      Compression.serialize(folder + "/0.dat", this);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  public synchronized void add(Table<ROW> table) {
    table.id = nextid++;
    if (nextid == maxid) {
      JFLog.log("Warning:TableList:next id reset to start");
      nextid = minid;
    }
    table.filename = folder + "/" + table.id + ".dat";
    tables.add(table);
    save();
  }
  public ArrayList<Table<ROW>> getTables() {
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
