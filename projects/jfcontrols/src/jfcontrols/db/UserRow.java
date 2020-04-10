package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class UserRow extends javaforce.db.Row {
  public String name;
  public String pass;
  public int gid;  //group id
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    pass = readString();
    gid = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(pass);
    writeInt(gid);
  }
}
