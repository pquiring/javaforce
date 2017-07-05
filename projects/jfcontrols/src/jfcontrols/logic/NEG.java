package jfcontrols.logic;

/** Negative Edge detection
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class NEG extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "pos";
  }

  public String getCode(int[] types, boolean[] array, boolean[] unsigned) {
    return "if (enabled) {if (!tags[1].getBoolean()) tags[1].setBoolean(true); enabled = false;} else {if (tags[1].getBoolean()) {tags[1].setBoolean(false); enabled = true;}}";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
