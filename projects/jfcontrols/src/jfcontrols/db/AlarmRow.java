package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class AlarmRow extends javaforce.db.Row {
  public AlarmRow() {}
  public AlarmRow(int id) {
    this.aid = id;
  }
  public int aid;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    aid = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeInt(aid);
  }
}
