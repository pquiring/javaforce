package jfcontrols.logic;

/** Not bit logic.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class NOT extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "not";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "enabled = !enabled;\r\n";
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.unknown;
  }
}
