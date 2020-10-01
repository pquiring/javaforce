package jfpbx.db;

/**
 *
 * @author pquiring
 */

import javaforce.db.*;

public class UserRow extends Row {
  public String name, password;

  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    password = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(password);
  }
}
