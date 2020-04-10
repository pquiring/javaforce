package jfcontrols.db;

/** TimezoneRow
 *
 * @author pquirnig
 */

public class TimezoneRow extends javaforce.db.Row {
  //format 00:00 to 23:59
  public static class Time extends javaforce.db.Row {
    public int hour;
    public int min;
    private static final int version = 1;
    public void readObject() throws Exception {
      super.readObject();
      int ver = readInt();
      hour = readInt();
      min = readInt();
    }
    public void writeObject() throws Exception {
      super.writeObject();
      writeInt(version);
      writeInt(hour);
      writeInt(min);
    }
  }
  public String name;
  public Time begin[] = new Time[7];
  public Time end[] = new Time[7];
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    for(int a=0;a<7;a++) {
      begin[a].readInit(this);
      begin[a].readObject();
    }
    for(int a=0;a<7;a++) {
      end[a].readInit(this);
      end[a].readObject();
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    for(int a=0;a<7;a++) {
      begin[a].writeInit(this);
      begin[a].writeObject();
    }
    for(int a=0;a<7;a++) {
      end[a].writeInit(this);
      end[a].writeObject();
    }
  }
}
