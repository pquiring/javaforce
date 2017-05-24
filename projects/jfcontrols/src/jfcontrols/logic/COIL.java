package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class COIL extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getName() {
    return "Coil";
  }

  public String getCode() {
    return "tags[1].setBoolean(enabled);\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.BIT;
  }
}
