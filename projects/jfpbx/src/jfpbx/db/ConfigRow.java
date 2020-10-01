package jfpbx.db;

/**
 *
 * @author pquiring
 */

public class ConfigRow extends javaforce.db.Row {
  public String name;
  public String value;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    value = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(value);
  }
}
