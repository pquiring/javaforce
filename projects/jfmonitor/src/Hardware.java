/** Hardware
 *
 * @author pquiring
 */

import java.io.*;
import java.util.*;

public class Hardware implements Serializable {
  public static final long serialVersionUID = 1;

  public String version;
  public String config;
  public ArrayList<Port> ports;
  public ArrayList<VLAN> vlans;
  public ArrayList<Group> groups;
}
