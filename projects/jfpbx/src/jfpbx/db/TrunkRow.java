package jfpbx.db;

/**
 *
 * @author pquiring
 */

import javaforce.db.*;

public class TrunkRow extends Row {
  public String name;
  public String register;
  public String host;
  public String cid;
  public String inrules, outrules;

  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    register = readString();
    host = readString();
    cid = readString();
    inrules = readString();
    outrules = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(register);
    writeString(host);
    writeString(cid);
    writeString(inrules);
    writeString(outrules);
  }
}
