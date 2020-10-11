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
  public String xip;
  public int flags;

  public static int FLAG_REGISTER = 1;

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
    xip = readString();
    flags = readInt();
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
    writeString(xip);
    writeInt(flags);
  }

  public boolean doRegister() {
    return (flags & FLAG_REGISTER) != 0;
  }

  public void setRegister(boolean state) {
    if (state) {
      flags |= FLAG_REGISTER;
    } else {
      flags &= -1 ^ FLAG_REGISTER;
    }
  }
}
