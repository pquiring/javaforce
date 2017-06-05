package jfcontrols.tags;

/** Tag Address
 *
 * @author pquiring
 */

import javaforce.*;

public class TagAddr {
  public int cid = 0;
  public String name;
  public int idx = -1;
  public String member;
  public int midx = -1;
  public String tempValue;
  public static TagAddr tempValue(String value) {
    TagAddr ta = new TagAddr();
    ta.tempValue = value;
    return ta;
  }

  public String toString() {
    if (member != null) {
      return "TagAddr:" + name + "[" + idx + "]." + member;
    } else {
      return "TagAddr:" + name + "[" + idx + "]";
    }
  }

  public boolean isArray() {
    return idx != -1 || midx != -1;
  }
}
