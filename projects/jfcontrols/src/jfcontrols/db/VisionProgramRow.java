package jfcontrols.db;

/** VisionProgramRow
 *
 * @author pquiring
 */

public class VisionProgramRow extends javaforce.db.Row {
  public int pid;
  public String name;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    pid = readInt();
    name = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(pid);
    writeString(name);
  }
}
