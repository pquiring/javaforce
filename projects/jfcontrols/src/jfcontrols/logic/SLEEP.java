package jfcontrols.logic;

/** Add
 *
 * @author pquiring
 */

import javaforce.controls.*;

public class SLEEP extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getName() {
    return "Sleep";
  }

  public String getCode() {
    return "if (enabled) javaforce.JF.sleep(getInt(tags[1]));\r\n";
  }

  public int getTagsCount() {
    return 1;
  }

  public int getTagType(int idx) {
    return TagType.int32;
  }
}
