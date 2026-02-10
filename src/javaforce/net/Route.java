package javaforce.net;

/** Routing entry
 *
 * @author pquiring
 */

public class Route {
  public static Route[] RouteArrayType = new Route[0];

  public Subnet4 dest_mask;
  public IP4 gateway;
  public String dev;
  public int metric;
}
