package jfcontrols.logic;

/** Examine On.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class XON extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getName() {
    return "xon";
  }

  public String getCode() {
    return "enabled &= getBoolean(tags[1]);\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
