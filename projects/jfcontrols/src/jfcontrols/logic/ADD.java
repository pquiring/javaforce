package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import javaforce.controls.*;

import jfcontrols.tags.*;

public class ADD extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Add";
  }

  public String getCode() {
    return "if (enabled) tags[3].setInt(tags[1].getInt() + tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.int32;
  }
}
