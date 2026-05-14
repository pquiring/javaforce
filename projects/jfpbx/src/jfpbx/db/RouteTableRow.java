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
}
