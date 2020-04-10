package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class ListRow extends javaforce.db.Row {
  public ListRow() {}
  public ListRow(int idx, String value) {
    this.idx = idx;
    this.value = value;
  }
  public int idx;
  public String value;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    idx = readInt();
    value = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(idx);
    writeString(value);
  }
}
