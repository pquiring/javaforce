package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class COIL extends Logic {

  public Type getType() {
    return Type.inline;
  }

  public String getName() {
    return "coil";
  }

  public String getCode() {
    return "tags[0].set(en[enidx]);\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return Types.BIT;
  }
}
