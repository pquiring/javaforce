package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class BlockRow extends javaforce.db.Row {
  public int fid;
  public int rid;
  public int bid;
  public String name;
  public String tags;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    fid = readInt();
    rid = readInt();
    bid = readInt();
    name = readString();
    tags = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(fid);
    writeInt(rid);
    writeInt(bid);
    writeString(name);
    writeString(tags);
  }
}
