package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class PanelRow extends javaforce.db.Row {
  public String name;
  public boolean popup;
  public boolean builtin;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    readString();  //old display name (obsolete)
    popup = readBoolean();
    builtin = readBoolean();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(null);  //old display name (obsolete)
    writeBoolean(popup);
    writeBoolean(builtin);
  }
}
