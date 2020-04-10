package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class IORow extends javaforce.db.Row {
  public int mid;
  public int idx;
  public String comment;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    mid = readInt();
    idx = readInt();
    comment = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(mid);
    writeInt(idx);
    writeString(comment);
  }
}
