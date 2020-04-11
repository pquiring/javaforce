package jfcontrols.db;

/** GroupRow
 *
 * @author pquiring
 */

public class GroupRow extends javaforce.db.Row {
  public static class Zone extends javaforce.db.Row {
    public int rid;  //reader id
    public int zid;  //timezone id
    private static final int version = 1;
    public void readObject() throws Exception {
      super.readObject();
      int ver = readInt();
      rid = readInt();
      zid = readInt();
    }
    public void writeObject() throws Exception {
      super.writeObject();
      writeInt(version);
      writeInt(rid);
      writeInt(zid);
    }
  }
  public String name;
  public Zone zones[] = new Zone[0];
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    int cnt = readInt();
    zones = new Zone[cnt];
    for(int a=0;a<cnt;a++) {
      zones[a] = new Zone();
      zones[a].readInit(this);
      zones[a].readObject();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    int cnt = zones.length;
    writeInt(cnt);
    for(int a=0;a<cnt;a++) {
      zones[a].writeInit(this);
      zones[a].writeObject();
    }
  }
}
