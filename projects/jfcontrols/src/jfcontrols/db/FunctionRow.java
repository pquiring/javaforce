package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class FunctionRow extends javaforce.db.Row {
  public String name;
  public long revision;
  public String comment;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    revision = readLong();
    comment = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeLong(revision);
    writeString(comment);
  }
}
