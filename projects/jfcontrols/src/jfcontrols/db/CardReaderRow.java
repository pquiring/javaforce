package jfcontrols.db;

/** CardReaderRow
 *
 * @author pquiring
 */

public class CardReaderRow extends javaforce.db.Row {
  public String name;  //display name
  public String addr;  //ip address of IP card reader
  public String door;  //output tag
  public int type;  //0=generic 1=RFideas
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    addr = readString();
    door = readString();
    type = readInt();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(addr);
    writeString(door);
    writeInt(type);
  }
}
