package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class UDT extends javaforce.db.Row {
  public String name;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
  }
}
