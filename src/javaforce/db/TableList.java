package javaforce.db;

import java.io.*;
import java.util.*;

import javaforce.*;

/** List of tables stored in a folder.
 *
 * @author pquiring
 */

public class TableList<ROW extends Row>  {
  private Row.Creator ctr;

  @SuppressWarnings("unchecked")
  private Table<ROW> create() {
    return (Table<ROW>)new Table(ctr);
  }

  /** Create TableList
   * @param rowCreator = interface to create new rows
   */
  public TableList(Row.Creator rowCreator) {
    ctr = rowCreator;
    data = new Data();
  }

  private static class Data implements Serializable {
    public static final long serialVersionUID = 1L;
    public int minid = 1;
    public int nextid = 1;
    public int maxid = 2147483647;  //2^31-1
  }

  private String folder;
  private ArrayList<Table<ROW>> tables = new ArrayList<Table<ROW>>();
  private Data data;

  private void loadTables() {
    for(int a=data.minid;a<data.nextid;a++) {
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

  /** Load list of tables from folder. */
  public boolean load(String folder) {
    this.folder = folder;
    try {
      File file = new File(folder + "/0.dat");
      if (!file.exists()) {
        return false;
      }
      data = (Data)Compression.deserialize(file);
      tables = new ArrayList<Table<ROW>>();
      loadTables();
      return true;
    } catch (Exception e) {
      JFLog.log(e);
      return false;
    }
  }

  /** Returns Table with assigned id. */
  public Table<ROW> get(int id) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table<ROW> table = tables.get(a);
      if (table.getTableId() == id) return table;
    }
    return null;
  }

  /** Returns Table with user-defined name. */
  public Table<ROW> get(String name) {
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table<ROW> table = tables.get(a);
      if (table.getName().equals(name)) return table;
    }
    JFLog.log("Error:Table not found:" + name);
    return null;
  }

  /** Save table with assigned id. */
  public void save(int id) {
    Table table = get(id);
    if (table != null) {
      table.save();
    }
  }

  private void save() {
    try {
      new File(folder).mkdirs();
      Compression.serialize(folder + "/0.dat", data);
    } catch (Exception e) {
      JFLog.log(e);
    }
  }

  /** Add another Table to this TableList.
   *
   * Table will be assigned next id.
   *
   */
  public void add(Table<ROW> table) {
    table.setTableId(data.nextid++);
    if (data.nextid == data.maxid) {
      JFLog.log("Warning:TableList:next id reset to start");
      data.nextid = data.minid;
    }
    table.setFilename(folder + "/" + table.getTableId() + ".dat");
    tables.add(table);
    save();
  }

  /** Returns list of Tables. */
  public ArrayList<Table<ROW>> getTables() {
    return tables;
  }

  /** Remove table with assigned id. */
  public void remove(int id) {
    File file = new File(folder + "/" + id + ".dat");
    if (file.exists()) {
      file.delete();
    }
    int cnt = tables.size();
    for(int a=0;a<cnt;a++) {
      Table table = tables.get(a);
      if (table.getTableId() == id) {
        tables.remove(a);
        break;
      }
    }
  }
}
