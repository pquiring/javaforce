package jfcontrols.logic;

/** Sets bit.
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SET extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getName() {
    return "set";
  }

  public String getCode() {
    return "if (enabled) tags[1].setBoolean(true);\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
