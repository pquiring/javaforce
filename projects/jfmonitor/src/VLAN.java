/** VLAN
 *
 * @author pquiring
 */

import java.io.Serializable;

public class VLAN implements Serializable {
  public static final long serialVersionUID = 1;

  public transient boolean valid;

  public String id;
  public String name;

  //interface
  public String ip, mask;
}
