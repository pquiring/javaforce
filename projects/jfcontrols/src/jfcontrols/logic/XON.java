package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class XON extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getName() {
    return "xon";
  }

  public String getCode() {
    return "enabled &= tags[0].getBoolean();\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.BIT;
  }
}
