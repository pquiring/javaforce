package jfcontrols.logic;

/** Sub
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SUB extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Sub";
  }

  public String getCode() {
    return "if (enabled) tags[2].setInt(tags[1].getInt() - tags[2].getInt());\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.int32;
  }
}
