package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class RungRow extends javaforce.db.Row {
  public int fid;
  public int rid;
  public String logic;
  public String comment;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    fid = readInt();
    rid = readInt();
    logic = readString();
    comment = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(fid);
    writeInt(rid);
    writeString(logic);
    writeString(comment);
  }
}
