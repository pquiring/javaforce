package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class ADD extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Add";
  }

  public String getCode() {
    return "tag[2].setInt(tag[0].getInt() + tag[1].getInt() );\r\n";
  }

  public int getTagsCount() {
    return 3;
  }

  public int getTagType(int idx) {
    return TagType.INT;
  }
}
