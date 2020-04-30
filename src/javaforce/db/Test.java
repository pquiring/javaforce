package javaforce.db;

/** TestRow
 *
 * @author pquiring
 */

public class Test extends Row {
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
    Table<Test> test = new Table<Test>(() -> {return new Test();});
    test.load("test.dat");
    Test row = new Test();
    row.value = "123";
    test.add(row);
    test.save();

    TableList<Test> list = new TableList<Test>(() -> {return new Test();});
    list.load("test");
    list.add(test);
  }
}
