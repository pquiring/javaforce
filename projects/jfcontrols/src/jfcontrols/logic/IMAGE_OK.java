package jfcontrols.logic;

import javaforce.controls.*;

/** Image OK
 *
 * @author pquiring
 */

public class IMAGE_OK extends Logic {

  public boolean isBlock() {
    return true;
  }

  public String getDesc() {
    return "ImageOK";
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
