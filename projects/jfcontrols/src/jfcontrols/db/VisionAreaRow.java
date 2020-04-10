package jfcontrols.db;

/** VisionAreaRow
 *
 * @author pquiring
 */

public class VisionAreaRow extends javaforce.db.Row {
  public int pid;  //program
  public int sid;  //shot
  public String name;
  public int x1,y1,x2,y2;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    pid = readInt();
    sid = readInt();
    name = readString();
    x1 = readInt();
    y1 = readInt();
    x2 = readInt();
    y2 = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(pid);
    writeInt(sid);
    writeString(name);
    writeInt(x1);
    writeInt(y1);
    writeInt(x2);
    writeInt(y2);
  }
}
