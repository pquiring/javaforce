package jfcontrols.logic;

import javaforce.controls.*;

/** Image Compare
 *
 * @author pquiring
 */

public class IMAGE_COMPARE extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageCompare";
  }

  public String getCode(int[] tagTypes, boolean[] array, boolean[] unsigned) {
    //TODO
    return null;
  }

  public int getTagsCount() {
    return 3;  //program# shot# roi#
  }

  public int getTagType(int idx) {
    return TagType.uint32;
  }
}
