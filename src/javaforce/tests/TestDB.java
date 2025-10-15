package javaforce.tests;

/** TestRow
 *
 * @author pquiring
 */

import javaforce.db.*;

public class TestDB extends Row {
  public String value;

  private static final int version = 1;

  public void readObject() throws Exception {
    int ver = readInt();
    value = readString();
  }

  public void writeObject() throws Exception {
    writeInt(version);
    writeString(value);
  }

  public static void main(String[] args) {
    Table<TestDB> test = new Table<TestDB>(() -> {return new TestDB();});
    test.load("test.dat");
    TestDB row = new TestDB();
    row.value = "123";
    test.add(row);
    test.save();

    TableList<TestDB> list = new TableList<TestDB>(() -> {return new TestDB();});
    list.load("test");
    list.add(test);
  }
}
