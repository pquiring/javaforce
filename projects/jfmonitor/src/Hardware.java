/** Hardware
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class Hardware implements Serializable {
  public static final long serialVersionUID = 1;

  public String user;
  public String pass;

  public String version;
  public String config;
  public ArrayList<Port> ports = new ArrayList<>();
  public ArrayList<VLAN> vlans = new ArrayList<>();
  public ArrayList<Port> groups = new ArrayList<>();
}
