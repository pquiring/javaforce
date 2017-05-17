package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class XON extends Logic {

  public Type getType() {
    return Type.inline;
  }

  public String getName() {
    return "xon";
  }

  public String getCode() {
    return "en[enidx] &= tags[0].isset();\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return Types.BIT;
  }
}
