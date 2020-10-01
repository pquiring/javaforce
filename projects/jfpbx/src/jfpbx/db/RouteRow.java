package jfpbx.db;

/**
 *
 * @author pquiring
 */

import javaforce.db.*;

public class RouteRow extends Row {
  public String name;

  public String patterns;
  public String trunks;

  public String priority;
  public String cid, did, dest;

  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    patterns = readString();
    trunks = readString();
    priority = readString();
    cid = readString();
    did = readString();
    dest = readString();
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeString(patterns);
    writeString(trunks);
    writeString(priority);
    writeString(cid);
    writeString(did);
    writeString(dest);
  }
}
