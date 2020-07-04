package jfcontrols.logic;

/** Positive Edge detection
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class POS extends LogicBlock {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "pos";
  }

  public boolean execute(boolean enabled) {
    if (enabled) {
      if (tags[1].getBoolean()) enabled = false; else tags[1].setBoolean(true);
    } else {
      if (tags[1].getBoolean()) tags[1].setBoolean(false);
    }
    return enabled;
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
