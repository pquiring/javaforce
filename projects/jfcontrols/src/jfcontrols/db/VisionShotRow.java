package jfcontrols.db;

/** VisionShotRow
 *
 * @author pquiring
 */

public class VisionShotRow extends javaforce.db.Row {
  public int pid;  //program id
  public int cid;  //camera id
  public int offset;  //offset (mm)
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    pid = readInt();
    cid = readInt();
    offset = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(pid);
    writeInt(cid);
    writeInt(offset);
  }
}
