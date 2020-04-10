package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class LogicRow extends javaforce.db.Row {
  public String name;
  public String shortname;
  public String group;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    shortname = readString();
    group = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(shortname);
    writeString(group);
  }
}
