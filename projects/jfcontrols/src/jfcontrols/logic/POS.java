package jfcontrols.logic;

/** Positive Edge detection
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class POS extends Logic {

  public boolean isBlock() {
    return false;
  }

  public String getDesc() {
    return "pos";
  }

  public String getCode(int types[]) {
    return "if (enabled) {if (tags[1].getBoolean()) enabled = false; else tags[1].setBoolean(true);} else {if (tags[1].getBoolean()) tags[1].setBoolean(false);}";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.bit;
  }
}
