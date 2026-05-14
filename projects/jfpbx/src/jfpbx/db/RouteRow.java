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
}
