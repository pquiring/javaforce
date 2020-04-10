package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class WatchRow extends javaforce.db.Row {
  public String tag;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    tag = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(tag);
  }
}
