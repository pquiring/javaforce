package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class UDTMember extends javaforce.db.Row {
  public int uid;
  public int mid;
  public String name;
  public int type;
  public int length;
  public String comment;
  public boolean builtin;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    uid = readInt();
    mid = readInt();
    name = readString();
    type = readInt();
    length = readInt();
    comment = readString();
    builtin = readBoolean();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(uid);
    writeInt(mid);
    writeString(name);
    writeInt(type);
    writeInt(length);
    writeString(comment);
    writeBoolean(builtin);
  }
}
