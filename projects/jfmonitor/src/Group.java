/** Port Group, ether channel, trunk, bonded, LACP, what ever you call it.
 *
 * @author pquiring
 */

import java.io.Serializable;
import java.util.*;

public class Group implements Serializable {
  public static final long serialVersionUID = 1;

  public transient boolean valid;

  public String id;
  public ArrayList<String> ports = new ArrayList<>();
}
