package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class ControllerRow extends javaforce.db.Row {
  public int cid;
  public String ip;
  public int type;
  public int speed;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    cid = readInt();
    ip = readString();
    type = readInt();
    speed = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(cid);
    writeString(ip);
    writeInt(type);
    writeInt(speed);
  }
}
