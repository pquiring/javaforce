package jfcontrols.db;

/**
 *
 * @author pquiring
 */

public class PanelRow extends javaforce.db.Row {
  public String name;
  public String display;
  public boolean popup;
  public boolean builtin;
  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    display = readString();
    popup = readBoolean();
    builtin = readBoolean();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(display);
    writeBoolean(popup);
    writeBoolean(builtin);
  }
}
