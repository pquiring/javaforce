package jfcontrols.logic;

/** Sub
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class SUB extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Sub";
  }

  public String getCode() {
    return "if (enabled) tag[3].setInt(tag[1].getInt() - tag[2].getInt() );\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.INT32;
  }
}
