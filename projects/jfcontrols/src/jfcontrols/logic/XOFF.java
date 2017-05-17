package jfcontrols.logic;

/** Examine Off.
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class XOFF extends Logic {

  public Type getType() {
    return Type.inline;
  }

  public String getName() {
    return "xoff";
  }

  public String getCode() {
    return "en[enidx] &= !tags[0].isset();\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return Types.BIT;
  }
}
