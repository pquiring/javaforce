/** Hardware
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class Hardware implements Serializable, Cloneable {
  public static final long serialVersionUID = 1;

  public String user;
  public String pass;
  public long lastUpdate;

  public String version;
  public String config;
  public ArrayList<Port> ports = new ArrayList<>();
  public ArrayList<VLAN> vlans = new ArrayList<>();
  public ArrayList<Port> groups = new ArrayList<>();

  public boolean routing;
  public String gateway;  //default gateway if !routing
  public ArrayList<Route> routes = new ArrayList<>();

  public String getGateway() {
    if (gateway == null) gateway = "";
    return gateway;
  }

  public Hardware clone() {
    try {
      Hardware clone = (Hardware)super.clone();  //shallow copy
      clone.ports = new ArrayList<>();
      clone.vlans = new ArrayList<>();
      clone.groups = new ArrayList<>();
      clone.routes = new ArrayList<>();
      return clone;
    } catch (Exception e) {
      return null;
    }
  }
}
