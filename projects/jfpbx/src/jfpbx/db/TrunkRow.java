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
