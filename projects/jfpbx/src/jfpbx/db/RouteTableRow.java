package jfpbx.db;

/**
 *
 * @author pquiring
 */

import java.util.*;

import javaforce.db.*;

public class RouteTableRow extends Row {
  public String name;
  public ArrayList<RouteRow> routes = new ArrayList<RouteRow>();

  private static final int version = 1;
  public void readObject() throws Exception {
    super.readObject();
    int ver = readInt();
    name = readString();
    int cnt = readInt();
    for(int a=0;a<cnt;a++) {
      RouteRow route = new RouteRow();
      route.readInit(this);
      route.readObject();
      routes.add(route);
    }
  }
  public void writeObject() throws Exception {
    super.writeObject();
    writeInt(version);
    writeString(name);
    writeInt(routes.size());
    for(RouteRow route : routes) {
      route.writeInit(this);
      route.writeObject();
    }
  }
}
