package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import jfcontrols.tags.*;

public class SLEEP extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Sleep";
  }

  public String getCode() {
    return "if (enabled) javaforce.JF.sleep(tags[1].getInt());\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.INT32;
  }
}
