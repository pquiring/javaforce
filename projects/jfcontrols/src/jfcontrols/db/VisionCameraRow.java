package jfcontrols.db;

/** VisionCameraRow
 *
 * @author pquiring
 */

public class VisionCameraRow extends javaforce.db.Row {
  public int cid;
  public String name;
  public String url;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    cid = readInt();
    name = readString();
    url = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(cid);
    writeString(name);
    writeString(url);
  }
}
