package jfcontrols.logic;

import javaforce.controls.*;

/** Image NOK
 *
 * @author pquiring
 */

public class IMAGE_NOK extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageNOK";
  }

  public String getCode(int[] tagTypes, boolean[] array, boolean[] unsigned) {
    //TODO
    return null;
  }

  public int getTagsCount() {
    return 0;
  }

  public int getTagType(int idx) {
    return TagType.uint32;
  }

}
